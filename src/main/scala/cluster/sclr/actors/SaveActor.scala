package cluster.sclr.actors

import akka.actor.{Actor, ActorLogging, Props}
import cats.effect.{Async, IO}
import cluster.sclr.Messages._
import cluster.sclr.doobie.ResultsDao
import doobie.implicits._

import scala.util.Success

class SaveActor() extends Actor with ActorLogging {

  private lazy val xa = ResultsDao.makeDefaultTransactor()

  private def setupDatabase() = {
    try {
      ResultsDao.createSchemaIfNeeded()
      ResultsDao.up1.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        log.error(e, "Could not set up database.")
        throw e
    }
  }

  override def preStart(): Unit = {
    setupDatabase()
  }

  override def postStop(): Unit = {
    xa.configure(ds => Async[IO].delay(ds.close)).unsafeRunSync()
  }

  def receive = {
    case Result(Success(data)) => {
      // Save result!
      val insert = ResultsDao.insertResult(data.something)
      log.debug(s"sql: ${insert.sql}")
      try {
        val insertedRowCount = insert.run.transact(xa).unsafeRunSync()
        log.debug(s"insertedRows: $insertedRowCount")
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

}

object SaveActor {
  def props() = Props(new SaveActor())
}
