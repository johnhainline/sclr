package akka.stream.scaladsl

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.NotUsed
import akka.annotation.InternalApi
import akka.stream._
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
  final case class NeedWakeup(consumer: Consumer) extends HubEvent

  final case class Consumer(id: Long, cmdCallback: AsyncCallback[ConsumerEvent])
  private object Completed

  private sealed trait HubState
  private case class Open(callbackFuture: Future[AsyncCallback[HubEvent]], registrations: List[Consumer]) extends HubState
  private case class Closed(failure: Option[Throwable]) extends HubState

  private class BalanceSinkLogic(_shape: Shape) extends GraphStageLogic(_shape) with InHandler {

    private[this] val hubCallbackPromise: Promise[AsyncCallback[HubEvent]] = Promise()
    private[this] val noRegistrationsState = Open(hubCallbackPromise.future, Nil)
    val state = new AtomicReference[HubState](noRegistrationsState)
    private[this] var upstreamFinished = false

    // The pendingQueue holds all elements we received from upstream that have no consumer to go to yet.
    private val pendingQueue = new ConcurrentLinkedQueue[T]()
    // Lists all consumers that tried to get a T but couldn't.
    private val needWakeup: mutable.LongMap[Consumer] = mutable.LongMap.empty

    private val consumers: mutable.LongMap[Consumer] = mutable.LongMap.empty

    override def preStart(): Unit = {
      setKeepGoing(true)
      hubCallbackPromise.success(getAsyncCallback[HubEvent](onEvent))
      tryPull()
    }

    override def onPush(): Unit = {
      pendingQueue.add(grab(in))
      tryPull()
      wakeup()
    }

    private def tryPull(): Unit = {
      if (pendingQueue.size < bufferSize && !hasBeenPulled(in)) pull(in)
    }

    private def wakeup(): Unit = {
      needWakeup.values.headOption foreach { consumer =>
        needWakeup -= consumer.id
        tryPull()
        consumer.cmdCallback.invoke(Wakeup)
      }
    }

    private def wakeupAll(): Unit = {
      val all = needWakeup.values
      needWakeup.clear()
      all.foreach(_.cmdCallback.invoke(Wakeup))
    }

    private def onEvent(hubEvent: HubEvent): Unit = {
      hubEvent match {
        case NeedWakeup(consumer) ⇒
          // If we're told a consumer needs a wakeup, then maybe we can give it an element!
          // Also check if the consumer is now unblocked since we published an element since it went asleep.
          if (!pendingQueue.isEmpty)
            consumer.cmdCallback.invoke(Wakeup)
          else {
            needWakeup.update(consumer.id, consumer)
            tryPull()
          }

        case RegistrationPending ⇒
          state.getAndSet(noRegistrationsState).asInstanceOf[Open].registrations foreach { consumer ⇒
            consumers(consumer.id) = consumer
            // in case the consumer is already stopped we need to undo registration
            implicit val ec: ExecutionContext = materializer.executionContext
            consumer.cmdCallback.invokeWithFeedback(Wakeup).onFailure {
              case _: StreamDetachedException ⇒
                hubCallbackPromise.future.foreach(callback ⇒
                  callback.invoke(UnRegister(consumer.id))
                )
            }
          }

        case UnRegister(id) ⇒
          consumers.remove(id)
          if (consumers.isEmpty) {
            if (isClosed(in)) completeStage()
          }
      }
    }

    // Producer API
    override def onUpstreamFailure(ex: Throwable): Unit = {
      val failMessage = HubCompleted(Some(ex))

      // Notify pending consumers and set tombstone
      state.getAndSet(Closed(Some(ex))).asInstanceOf[Open].registrations foreach { consumer ⇒
        consumer.cmdCallback.invoke(failMessage)
      }

      // Notify registered consumers
      consumers.values.iterator foreach { consumer ⇒
        consumer.cmdCallback.invoke(failMessage)
      }
      failStage(ex)
    }

    override def onUpstreamFinish(): Unit = {
      if (consumers.isEmpty) {
        completeStage()
      } else {
        upstreamFinished = true
        wakeupAll()
      }
    }

    override def postStop(): Unit = {
      // Notify pending consumers and set tombstone

      @tailrec def tryClose(): Unit = state.get() match {
        case Closed(_) ⇒ // Already closed, ignore
        case open: Open ⇒
          if (state.compareAndSet(open, Closed(None))) {
            val completedMessage = HubCompleted(None)
            open.registrations foreach { consumer ⇒
              consumer.cmdCallback.invoke(completedMessage)
            }
          } else tryClose()
      }

      tryClose()
    }

    // Consumer API
    def poll(id: Long, hubCallback: AsyncCallback[HubEvent]): AnyRef = {
      this.synchronized {
        if (pendingQueue.isEmpty && upstreamFinished) {
          Completed
        } else if (pendingQueue.isEmpty) {
          null
        } else {
          pendingQueue.poll().asInstanceOf[AnyRef]
        }
      }
    }

    setHandler(in, this)
  }

  sealed trait ConsumerEvent
  case object Wakeup extends ConsumerEvent
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
        private val sourceCallback = getAsyncCallback(onCommand)
        private val consumer = Consumer(id, sourceCallback)

        override def preStart(): Unit = {
          val onHubReady: Try[AsyncCallback[HubEvent]] ⇒ Unit = {
            case Success(receivedHubCallback) ⇒
              hubCallback = receivedHubCallback
              if (isAvailable(out)) onPull()
              receivedHubCallback.invoke(RegistrationPending)
            case Failure(ex) ⇒
              failStage(ex)
          }

          @tailrec def register(): Unit = {
            logic.state.get() match {
              case Closed(Some(ex)) ⇒
                failStage(ex)
              case Closed(None) ⇒
                completeStage()
              case previousState @ Open(hubCallbackFuture, registrations) ⇒
                val newRegistrations = Consumer(id, sourceCallback) :: registrations
                if (logic.state.compareAndSet(previousState, Open(hubCallbackFuture, newRegistrations))) {
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
            element match {
              case null ⇒
                hubCallback.invoke(NeedWakeup(consumer))
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
            if (isAvailable(out)) onPull()
        }

        setHandler(out, this)
      }
    }

    (logic, Source.fromGraph(source))
  }
}
