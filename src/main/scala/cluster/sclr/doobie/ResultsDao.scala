package cluster.sclr.doobie

import java.sql.Timestamp
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import cluster.sclr.doobie.ResultsDao.{Result, coeffNames, dimensionNames, rowNames}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Fragment, _}
import ResultsDao._

class ResultsDao extends LazyLogging {

  private lazy val xa: Transactor[IO] = ResultsDao.makeHikariTransactor()

  def getResultCount() = {
    sql"SELECT COUNT(*) FROM results".query[Long].unique.transact(xa).unsafeRunSync()
  }

  def getResults() = {
    import ResultsDao.ZonedDateTimeMeta
    sql"SELECT id, data, created_at FROM results".query[Result].list.transact(xa).unsafeRunSync()
  }

  def insertResult(resultType: String, isGood: Boolean, selectDimensions: Vector[Int], selectRows: Vector[Int], coefficients: Vector[Double]) = {
    val insertNames = Vector("is_good",
      dimensionNames(selectDimensions.length).mkString(","),
      rowNames(selectRows.length).mkString(","),
      coeffNames(coefficients.length).mkString(",")
    )

    val reducer = { (l:Fragment, r:Fragment) => l ++ r}
    val fragmentValues =
      fr"$isGood" ++
      selectDimensions.map(d => fr", $d").reduce(reducer) ++
      selectRows.map(r => fr", $r").reduce(reducer) ++
      coefficients.map(c => fr", $c").reduce(reducer)

    val dbUpdate = (fr"INSERT INTO " ++ Fragment.const(tableName(resultType)) ++
      Fragment.const(insertNames.mkString("(", ",", ")")) ++
      fr"VALUES" ++
      Fragment.const("(") ++ fragmentValues ++ Fragment.const(")"))
      .update
    dbUpdate.run.transact(xa).unsafeRunSync()
  }

  def setupDatabase() = {
    try {
      val (driver, url, schema, username, password) = ResultsDao.getConfigSettings
      val xa = Transactor.fromDriverManager[IO](driver, url, username, password)

      val createIfNotExists = (fr"CREATE SCHEMA IF NOT EXISTS" ++ Fragment.const(schema)).update
      createIfNotExists.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        logger.error("Could not set up database.", e)
        throw e
    }
  }

  def setupTable(resultType: String, dimensions: Int, rows: Int, coefficients: Int): Int = {
    try {
      val tableDims = dimensionNames(dimensions).map(name => s"$name INT NOT NULL")
      val tableRows = rowNames(rows).map(name => s"$name INT NOT NULL")
      val tableCoeffs = coeffNames(coefficients).map(name => s"$name DOUBLE NOT NULL")
      val fragment = fr"CREATE TABLE IF NOT EXISTS" ++
        Fragment.const(tableName(resultType)) ++
        Fragment.const((
          Vector("id BIGINT NOT NULL AUTO_INCREMENT", "is_good BOOLEAN NOT NULL") ++
            tableDims ++ tableRows ++ tableCoeffs ++
            Vector("created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "PRIMARY KEY (id)")
          ).mkString("(", ",", ")"))
      fragment.update.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        logger.error("Could not set up database.", e)
        throw e
    }
  }
}

object ResultsDao {

  case class Result(id: Long, data: String, createdAt: ZonedDateTime)

  implicit val ZonedDateTimeMeta: Meta[ZonedDateTime] =
    Meta[Timestamp].xmap(
      ts  => ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime), ZoneId.systemDefault),
      zdt => new Timestamp(Instant.from(zdt).toEpochMilli)
    )

  private def tableName(resultType: String) = s"results_$resultType"
  private def dimensionNames(dims: Int) = Range(0, dims).map(dim => s"dim$dim")
  private def rowNames(rows: Int) = Range(0, rows).map(row => s"row$row")
  private def coeffNames(coeffs: Int) = Range(0, coeffs).map(coeff => s"coeff$coeff")

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