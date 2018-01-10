package cluster.sclr.doobie

import java.sql.Timestamp
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import cluster.sclr.doobie.ResultsDao.Result
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Fragment, _}

class ResultsDao extends LazyLogging {

  // Do initial work to prevent bad database connection.
  setupDatabase()

  private lazy val xa: Transactor[IO] = ResultsDao.makeHikariTransactor()

  def getResultCount() = {
    sql"SELECT COUNT(*) FROM results".asInstanceOf[Fragment].query[Long].unique.transact(xa).unsafeRunSync()
  }

  def getResults() = {
    import ResultsDao.ZonedDateTimeMeta
    sql"SELECT id, data, created_at FROM results".asInstanceOf[Fragment].query[Result].list.transact(xa).unsafeRunSync()
  }

  def insertResult(data: String) = {
    Update[String]("INSERT INTO results (data) VALUES (?)").toUpdate0(data).run.transact(xa).unsafeRunSync()
  }

  private def setupDatabase() = {
    try {
      createSchemaIfNeeded()
      ResultsDao.up1.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        logger.error("Could not set up database.", e)
        throw e
    }
  }

  private def createSchemaIfNeeded(): Int = {
    val (driver, url, schema, username, password) = ResultsDao.getConfigSettings
    val xa = Transactor.fromDriverManager[IO](driver, url, username, password)

    val createIfNotExists = (fr"CREATE SCHEMA IF NOT EXISTS".asInstanceOf[Fragment] ++ Fragment.const(schema)).update
    createIfNotExists.run.transact(xa).unsafeRunSync()
  }

}

object ResultsDao {

  case class Result(id: Long, data: String, createdAt: ZonedDateTime)

  implicit val ZonedDateTimeMeta: Meta[ZonedDateTime] =
    Meta[Timestamp].xmap(
      ts  => ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime), ZoneId.systemDefault),
      zdt => new Timestamp(Instant.from(zdt).toEpochMilli)
    )

  private val up1: Update0 = sql"""
    CREATE TABLE IF NOT EXISTS results (
      id                BIGINT NOT NULL AUTO_INCREMENT,
      data              VARCHAR(255) NOT NULL,
      created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY       (id)
    )""".asInstanceOf[Fragment].update

  private def makeSimpleTransactor(): Transactor[IO] = {
    val (driver, url, schema, username, password) = getConfigSettings
    Transactor.fromDriverManager[IO](driver, s"$url/$schema", username, password)
  }

  private def makeHikariTransactor(): HikariTransactor[IO] = {
    val (driver, url, schema, username, password) = getConfigSettings
    Class.forName(driver)
    val config = new HikariConfig()
    config.setJdbcUrl(s"$url/$schema")
    config.setUsername(username)
    config.setPassword(password)
    config.setAutoCommit(false)
    HikariTransactor[IO](new HikariDataSource(config))
  }

  private def getConfigSettings = {
    val config = ConfigFactory.load()
    val driver   = config.getString("database.driver")
    val url      = config.getString("database.url")
    val schema   = config.getString("database.schema")
    val username = config.getString("database.username")
    val password = config.getString("database.password")
    (driver, url, schema, username, password)
  }
}