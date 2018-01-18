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

  private lazy val xa: Transactor[IO] = ResultsDao.makeHikariTransactor()

  def getResultCount() = {
    sql"SELECT COUNT(*) FROM results".query[Long].unique.transact(xa).unsafeRunSync()
  }

  def getResults() = {
    import ResultsDao.ZonedDateTimeMeta
    sql"SELECT id, data, created_at FROM results".query[Result].list.transact(xa).unsafeRunSync()
  }

  def insertResult(isGood: Boolean, dim1: Int, dim2: Int, row1: Int, row2: Int, row3: Int,
                   coeff1: Double, coeff2: Double, coeff3: Double) = {
    sql"""
      INSERT INTO results
        (is_good, dim1, dim2, row1, row2, row3, coefficient1, coefficient2, coefficient3)
      VALUES
        ($isGood, $dim1, $dim2, $row1, $row2, $row3, $coeff1, $coeff2, $coeff3)
      """.update.run.transact(xa).unsafeRunSync()
  }

  def setupDatabase() = {
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

    val createIfNotExists = (fr"CREATE SCHEMA IF NOT EXISTS" ++ Fragment.const(schema)).update
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
      is_good           BOOLEAN NOT NULL,
      dim1              INT NOT NULL,
      dim2              INT NOT NULL,
      row1              INT NOT NULL,
      row2              INT NOT NULL,
      row3              INT NOT NULL,
      coefficient1      DOUBLE NOT NULL,
      coefficient2      DOUBLE NOT NULL,
      coefficient3      DOUBLE NOT NULL,
      created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY       (id)
    )""".update

  private def makeHikariTransactor(): HikariTransactor[IO] = {
    val (driver, url, schema, username, password) = getConfigSettings
    Class.forName(driver)
    val config = new HikariConfig()
    config.setJdbcUrl(s"$url/$schema")
    config.setUsername(username)
    config.setPassword(password)
    config.setAutoCommit(false)
    config.setMaximumPoolSize(2)
    HikariTransactor[IO](new HikariDataSource(config))
  }

  private def getConfigSettings = {
    val config = ConfigFactory.load()
    val driver   = config.getString("database.driver")
    val host     = config.getString("database.host")
    val schema   = config.getString("database.schema")
    val username = config.getString("database.username")
    val password = config.getString("database.password")
    (driver, s"jdbc:mysql://$host", schema, username, password)
  }
}