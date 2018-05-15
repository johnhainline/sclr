/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.updated.stream.scaladsl

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.NotUsed
import akka.annotation.InternalApi
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * A BalanceHub is a special streaming hub that is able to broadcast streamed elements to a dynamic set of consumers.
  * It consists of two parts, a [[Sink]] and a [[Source]]. The [[Sink]] broadcasts elements from a producer to the
  * actually live consumers it has. Once the producer has been materialized, the [[Sink]] it feeds into returns a
  * materialized value which is the corresponding [[Source]]. This [[Source]] can be materialized an arbitrary number
  * of times, where each of the new materializations will receive their elements from the original [[Sink]].
  */
object BalanceHub {

  /**
    * INTERNAL API
    */
  @InternalApi private[akka] val defaultBufferSize = 16

  /**
    * Creates a [[Sink]] that receives elements from its upstream producer and broadcasts them to a dynamic set
    * of consumers. After the [[Sink]] returned by this method is materialized, it returns a [[Source]] as materialized
    * value. This [[Source]] can be materialized an arbitrary number of times and each materialization will receive the
    * broadcast elements from the original [[Sink]].
    *
    * Every new materialization of the [[Sink]] results in a new, independent hub, which materializes to its own
    * [[Source]] for consuming the [[Sink]] of that materialization.
    *
    * If the original [[Sink]] is failed, then the failure is immediately propagated to all of its materialized
    * [[Source]]s (possibly jumping over already buffered elements). If the original [[Sink]] is completed, then
    * all corresponding [[Source]]s are completed. Both failure and normal completion is "remembered" and later
    * materializations of the [[Source]] will see the same (failure or completion) state. [[Source]]s that are
    * cancelled are simply removed from the dynamic set of consumers.
    *
    * @param bufferSize determines the amount of buffered input this BalanceHub will try to maintain.
    */
  def sink[T](bufferSize: Int = defaultBufferSize): Sink[T, Source[T, NotUsed]] =
    Sink.fromGraph(new BalanceHub[T](bufferSize))
}

/**
  * INTERNAL API
  */
private[akka] class BalanceHub[T](bufferSize: Int)
  extends GraphStageWithMaterializedValue[SinkShape[T], Source[T, NotUsed]] {
  require(bufferSize > 0, "Buffer size must be positive")

  val in: Inlet[T] = Inlet("BalanceHub.in")
  override val shape: SinkShape[T] = SinkShape(in)

  sealed trait HubEvent

  private object RegistrationPending extends HubEvent

  private final case class UnRegister(id: Long) extends HubEvent

  final case class Waiting(consumer: Consumer) extends HubEvent

  final object TryPull extends HubEvent

  final case class Consumer(id: Long, consumerCallback: AsyncCallback[ConsumerEvent])

  private object Completed

  private sealed trait HubState

  private case class Open(hubCallbackFuture: Future[AsyncCallback[HubEvent]], registrations: List[Consumer]) extends HubState

  private case class Closed(failure: Option[Throwable]) extends HubState

  private class BalanceSinkLogic(_shape: Shape) extends GraphStageLogic(_shape) with InHandler {

    private[this] val hubCallbackPromise: Promise[AsyncCallback[HubEvent]] = Promise()
    private[this] val noRegistrationsState = Open(hubCallbackPromise.future, Nil)
    val state = new AtomicReference[HubState](noRegistrationsState)
    private[this] var upstreamFinished = false

    // The pendingQueue holds all elements we received from upstream that have no consumer to go to yet.
    private val pendingQueue = new ConcurrentLinkedQueue[T]()
    // Lists all consumers that tried to get a T but couldn't.
    private val waiting: mutable.LongMap[Consumer] = mutable.LongMap.empty

    private val consumers: mutable.LongMap[Consumer] = mutable.LongMap.empty

    override def preStart(): Unit = {
      setKeepGoing(true)
      hubCallbackPromise.success(getAsyncCallback[HubEvent](onEvent))
//      println(s"pulling next...")
      pull(in)
    }

    override def onPush(): Unit = {
      val add = grab(in)
      pendingQueue.add(add)
//      println(s"added: $add")
      if (pendingQueue.size < bufferSize) {
//        println(s"pulling next...")
        pull(in)
      }
      wakeup()
    }

    private def wakeup(): Unit = {
      waiting.values.headOption foreach { consumer =>
//        println(s"Hub wakeup consumer ${consumer.id}")
        waiting -= consumer.id
        consumer.consumerCallback.invoke(Wakeup)
      }
    }

    private def wakeupAll(): Unit = {
//      println(s"Hub wakeupAll(), waiting: $waiting")
      consumers.foreach { case (id, consumer) =>
//        println(s"Hub wakeup consumer ${consumer.id}")
        consumer.consumerCallback.invoke(Wakeup)
      }
      waiting.clear()
    }

    private def onEvent(hubEvent: HubEvent): Unit = {
      hubEvent match {
        case Waiting(consumer) ⇒
//          println(s"consumer says it is waiting: $consumer")
          // If we're told a consumer is waiting, then maybe we can give it an element!
          if (!pendingQueue.isEmpty) {
//            println(s"but we have stuff in queue, so we wake it up")
            consumer.consumerCallback.invoke(Wakeup)
          } else {
//            println(s"and the queue is empty, so we add it to our waiting list")
            waiting.update(consumer.id, consumer)
          }

        case TryPull ⇒
//          println(s"Hub TryPull")
          if (!upstreamFinished && !isAvailable(in) && !hasBeenPulled(in)) {
//            println(s"pulling next...")
            pull(in)
          }

        case RegistrationPending ⇒
//          println(s"RegistrationPending")
          state.getAndSet(noRegistrationsState).asInstanceOf[Open].registrations foreach { consumer ⇒
//            println(s"Registering ${consumer.id}")
            consumers(consumer.id) = consumer
            // in case the consumer is already stopped we need to undo registration
            implicit val ec: ExecutionContext = materializer.executionContext
            consumer.consumerCallback.invokeWithFeedback(Registered).onFailure {
              case _: StreamDetachedException ⇒
                hubCallbackPromise.future.foreach { hubCallback ⇒
                  hubCallback.invoke(UnRegister(consumer.id))
                }
            }
          }

        case UnRegister(id) ⇒
//          println(s"UnRegister($id)")
          consumers.remove(id)
          if (consumers.isEmpty && isClosed(in)) {
//            println(s"No consumers left, completeStage()")
            completeStage()
          }
      }
    }

    // Producer API
    override def onUpstreamFailure(ex: Throwable): Unit = {
//      println(s"onUpstreamFailure: $ex")
      val failMessage = HubCompleted(Some(ex))

      // Notify pending consumers and set tombstone
      state.getAndSet(Closed(Some(ex))).asInstanceOf[Open].registrations foreach { consumer ⇒
        consumer.consumerCallback.invoke(failMessage)
      }

      // Notify registered consumers
      consumers.values.iterator foreach { consumer ⇒
        consumer.consumerCallback.invoke(failMessage)
      }
      failStage(ex)
    }

    override def onUpstreamFinish(): Unit = {
//      println(s"Hub onUpstreamFinish")
      if (consumers.isEmpty) {
//        println(s"Hub completeStage()")
        completeStage()
      } else {
//        println(s"Hub upstreamFinished = true")
        upstreamFinished = true
        wakeupAll()
      }
    }

    override def postStop(): Unit = {
//      println(s"Hub postStop()")
      // Notify pending consumers and set tombstone

      @tailrec def tryClose(): Unit = state.get() match {
        case Closed(_) ⇒ // Already closed, ignore
        case open: Open ⇒
          if (state.compareAndSet(open, Closed(None))) {
            val completedMessage = HubCompleted(None)
            open.registrations foreach { consumer ⇒
              consumer.consumerCallback.invoke(completedMessage)
            }
          } else tryClose()
      }

      tryClose()
    }

    // Consumer API
    def poll(id: Long, hubCallback: AsyncCallback[HubEvent]): AnyRef = {
      hubCallback.invoke(TryPull)
      if (pendingQueue.isEmpty) {
        if (upstreamFinished) {
          Completed
        } else {
          null
        }
      } else {
        pendingQueue.poll().asInstanceOf[AnyRef]
      }
    }

    setHandler(in, this)
  }

  sealed trait ConsumerEvent

  case object Wakeup extends ConsumerEvent

  case object Registered extends ConsumerEvent

  private final case class HubCompleted(failure: Option[Throwable]) extends ConsumerEvent

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Source[T, NotUsed]) = {
    val idCounter = new AtomicLong()

    val logic = new BalanceSinkLogic(shape)

    val source = new GraphStage[SourceShape[T]] {
      val out: Outlet[T] = Outlet("BalanceHub.out")
      override val shape: SourceShape[T] = SourceShape(out)

      // This is creating the BalanceSourceLogic, which controls all materialized sources this hub connects to.
      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with OutHandler {
        private[this] val id = idCounter.getAndIncrement()
        private[this] var hubCallback: AsyncCallback[HubEvent] = _
        private val consumerCallback = getAsyncCallback(onCommand)
        private val myConsumerRecord = Consumer(id, consumerCallback)

        override def preStart(): Unit = {
          val onHubReady: Try[AsyncCallback[HubEvent]] ⇒ Unit = {
            case Success(receivedHubCallback) ⇒
//              println(s"onHubReady: Success")
              hubCallback = receivedHubCallback
              receivedHubCallback.invoke(RegistrationPending)
            case Failure(ex) ⇒
//              println(s"onHubReady: Failure")
              failStage(ex)
          }

          @tailrec def register(): Unit = {
//            println(s"consumer ${myConsumerRecord.id} register()")
            logic.state.get() match {
              case Closed(Some(ex)) ⇒
                failStage(ex)
              case Closed(None) ⇒
                completeStage()
              case previousState@Open(hubCallbackFuture, registrations) ⇒
                val newRegistrations = myConsumerRecord :: registrations
                if (logic.state.compareAndSet(previousState, Open(hubCallbackFuture, newRegistrations))) {
//                  println(s"consumer ${myConsumerRecord.id} register finished")
                  hubCallbackFuture.onComplete(getAsyncCallback(onHubReady).invoke)(materializer.executionContext)
                } else {
                  register()
                }
            }
          }
          register()
        }

        override def onPull(): Unit = {
          if (hubCallback ne null) {
            val element = logic.poll(id, hubCallback)
//            println(s"consumer (${myConsumerRecord.id}) onPull(): got $element")
            element match {
              case null ⇒
//                println(s"telling hub we (${myConsumerRecord.id}) are waiting")
                hubCallback.invoke(Waiting(myConsumerRecord))
              case Completed ⇒
                completeStage()
              case _ ⇒
                push(out, element.asInstanceOf[T])
            }
          }
        }

        override def postStop(): Unit = {
          if (hubCallback ne null) {
            hubCallback.invoke(UnRegister(id))
          }
        }

        private def onCommand(cmd: ConsumerEvent): Unit = cmd match {
          case HubCompleted(Some(ex)) ⇒
            failStage(ex)
          case HubCompleted(None) ⇒
            completeStage()
          case Wakeup ⇒
//            println(s"consumer (${myConsumerRecord.id}) got Wakeup")
            if (isAvailable(out)) onPull()
          case Registered ⇒
//            println(s"consumer (${myConsumerRecord.id}) got Registered")
            if (isAvailable(out)) onPull()
        }

        setHandler(out, this)
      }
    }

    (logic, Source.fromGraph(source))
  }
}
