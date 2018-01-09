package cluster.sclr.doobie

import java.sql.Timestamp
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import doobie.{Fragment, _}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor

object ResultsDao {

  implicit val ZonedDateTimeMeta: Meta[ZonedDateTime] =
    Meta[Timestamp].xmap(
      ts  => ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime), ZoneId.systemDefault),
      zdt => new Timestamp(Instant.from(zdt).toEpochMilli)
    )

  val up1: Update0 = sql"""
    CREATE TABLE IF NOT EXISTS results (
      id                BIGINT NOT NULL AUTO_INCREMENT,
      data              VARCHAR(255) NOT NULL,
      created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY       (id)
    )""".asInstanceOf[Fragment].update

  val getResults: Query0[Result] = sql"SELECT id, data, created_at FROM results".asInstanceOf[Fragment].query[Result]

  def insertResult(data: String): Update0 = {
    Update[String]("INSERT INTO results (data) VALUES (?)").toUpdate0(data)
  }

  case class Result(id: Long, data: String, createdAt: ZonedDateTime)

  def createSchemaIfNeeded(schema: String = "sclr"): Int = {
    val (driver, url, _, username, password) = getConfigSettings()
    val xa = Transactor.fromDriverManager[IO](driver, url, username, password)

    val createIfNotExists = (fr"CREATE SCHEMA IF NOT EXISTS".asInstanceOf[Fragment] ++ Fragment.const(schema)).update
    createIfNotExists.run.transact(xa).unsafeRunSync()
  }

  def makeSimpleTransactor() = {
    val (driver, url, schema, username, password) = getConfigSettings()
    Transactor.fromDriverManager[IO](driver, s"$url/$schema", username, password)
  }

  def makeHikariTransactor() = {
    val (driver, url, schema, username, password) = getConfigSettings()
    HikariTransactor.newHikariTransactor[IO](driver, s"$url/$schema", username, password).unsafeRunSync()
  }

  private def getConfigSettings() = {
    val config = ConfigFactory.load()
    val driver   = config.getString("database.driver")
    val url      = config.getString("database.url")
    val schema   = config.getString("database.schema")
    val username = config.getString("database.username")
    val password = config.getString("database.password")
    (driver, url, schema, username, password)
  }
}
