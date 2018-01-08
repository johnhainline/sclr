package cluster.sclr.actors

import java.sql.{Connection, DriverManager}

import akka.actor.{Actor, ActorLogging, Props}
import cluster.sclr.Messages._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable.ListBuffer

class SaveActor(makeConnection: () => Connection) extends Actor with ActorLogging {

  private lazy val connection = makeConnection()
  private def checkDatabase() = {
    try {
      val statement = connection.createStatement()
      val names = ListBuffer[String]()
      val resultSet = statement.executeQuery("SHOW DATABASES")
      while (resultSet.next()) {
        names.append(resultSet.getString("Database"))
      }
      if (!names.contains("sclr")) {
        statement.execute("CREATE DATABASE sclr")
      }
      statement.execute("USE sclr")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  override def preStart: Unit = {
    checkDatabase()
  }

  def receive = {
    case Result(result) => {
      // Save result!
      log.debug(s"$result")
    }
  }

}

object SaveActor {
  def props(makeConnection: () => Connection = makeDefaultConnection) = Props(new SaveActor(makeConnection))

  def makeDefaultConnection() = {
    val config = ConfigFactory.load()
    val databaseDriver = config.getString("database.driver")
    Class.forName(databaseDriver)
    val url      = config.getString("database.url")
    val username = config.getString("database.username")
    val password = config.getString("database.passsword")
    val connection = DriverManager.getConnection(url, username, password)
    connection
  }
}
