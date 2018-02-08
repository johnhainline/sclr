package cluster.sclr.core

import java.io.{BufferedReader, InputStreamReader}
import java.sql.Timestamp
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import cluster.sclr.ScriptRunner
import cluster.sclr.core.DatabaseDao.{coeffNames, dimensionNames, rowNames, _}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.{Fragment, _}
import weka.experiment.InstanceQuery

class DatabaseDao extends LazyLogging {

  private lazy val xa: Transactor[IO] = DatabaseDao.makeHikariTransactor()

  def initializeDataset(name: String): Unit = {
    val (driver, url, username, password) = getConfigSettings
    val xa = Transactor.fromDriverManager[IO](driver, url, username, password)
    FC.raw { connection =>
      val runner = new ScriptRunner(connection, false, false)
      val file = new BufferedReader(new InputStreamReader(getClass.getClassLoader.getResourceAsStream(s"datasets/$name.sql")))
      runner.runScript(file)
    }.transact(xa).unsafeRunSync()
  }

  def getDatasetInfo(name: String): DatasetInfo = {
    val xDimensionsQuery = (fr"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = " ++
      Fragment.const(s"""\"$name\"""") ++ fr" AND table_name = " ++ Fragment.const(""""x"""")).query[Int]
    // We take -1 off the dimensions to account for our id primary key column.
    val xDimensionCount = xDimensionsQuery.unique.transact(xa).unsafeRunSync() - 1

    val xRowsQuery = Fragment.const(s"SELECT COUNT(*) FROM $name.x").query[Int]
    val xRowCount = xRowsQuery.unique.transact(xa).unsafeRunSync()

    val xRowSubsetCount = 200 // TODO actually calculate this somehow.
    DatasetInfo(xDimensionCount, xRowCount, xRowSubsetCount)
  }

  def getDataset(name: String): Dataset = {
    val query = wekaQuery()
    val x = query.retrieveInstances(s"SELECT * FROM $name.x")
    val yz = query.retrieveInstances(s"SELECT * FROM $name.yz")
    Dataset(x,yz)
  }

  private def wekaQuery() = {
    val (_, url, username, password) = getConfigSettings
    val query = new InstanceQuery()
    query.setDatabaseURL(url)
    query.setUsername(username)
    query.setPassword(password)
    query
  }

//  def getResults() = {
//    import DatabaseDao.ZonedDateTimeMeta
//    sql"SELECT id, data, created_at FROM results".query[Result].list.transact(xa).unsafeRunSync()
//  }

  def getResultCount() = {
    sql"SELECT COUNT(*) FROM results".query[Long].unique.transact(xa).unsafeRunSync()
  }

  def insertResult(schema: String, result: Result) = {
    val insertNames = Vector("error",
      dimensionNames(result.dimensions.length).mkString(","),
      rowNames(result.rows.length).mkString(","),
      coeffNames(result.coefficients.length).mkString(",")
    )

    val reducer = { (l:Fragment, r:Fragment) => l ++ r}
    val fragmentValues =
      fr"0.0" ++
      result.dimensions.map(d => fr", $d").reduce(reducer) ++
      result.rows.map(r => fr", $r").reduce(reducer) ++
      result.coefficients.map(c => fr", $c").reduce(reducer)

    val dbUpdate = (fr"INSERT INTO " ++ Fragment.const(s"$schema.results") ++
      Fragment.const(insertNames.mkString("(", ",", ")")) ++
      fr"VALUES" ++
      Fragment.const("(") ++ fragmentValues ++ Fragment.const(")"))
      .update
    dbUpdate.run.transact(xa).unsafeRunSync()
  }

  def setupSchemaAndTable(schema: String, dimensions: Int, rows: Int, coefficients: Int): Int = {
    setupSchema(schema)
    try {
      val tableDims = dimensionNames(dimensions).map(name => s"$name INT NOT NULL")
      val tableRows = rowNames(rows).map(name => s"$name INT NOT NULL")
      val tableCoeffs = coeffNames(coefficients).map(name => s"$name DOUBLE NOT NULL")
      val fragment = fr"CREATE TABLE IF NOT EXISTS" ++
        Fragment.const(s"$schema.results") ++
        Fragment.const((
          Vector("id BIGINT NOT NULL AUTO_INCREMENT", "error DOUBLE NOT NULL") ++
            tableDims ++ tableRows ++ tableCoeffs ++
            Vector("created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "PRIMARY KEY (id)")
          ).mkString("(", ",", ")"))
      fragment.update.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        logger.error("Could not create table.", e)
        throw e
    }
  }

  private def setupSchema(schema: String) = {
    try {
      val createIfNotExists = (fr"CREATE SCHEMA IF NOT EXISTS" ++ Fragment.const(schema)).update
      createIfNotExists.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        logger.error("Could not set up schema.", e)
        throw e
    }
  }
}

object DatabaseDao {

  implicit val ZonedDateTimeMeta: Meta[ZonedDateTime] =
    Meta[Timestamp].xmap(
      ts  => ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime), ZoneId.systemDefault),
      zdt => new Timestamp(Instant.from(zdt).toEpochMilli)
    )

  private def dimensionNames(dims: Int) = Range(0, dims).map(dim => s"dim$dim")
  private def rowNames(rows: Int) = Range(0, rows).map(row => s"row$row")
  private def coeffNames(coeffs: Int) = Range(0, coeffs).map(coeff => s"coeff$coeff")

  private def makeHikariTransactor(): HikariTransactor[IO] = {
    val (driver, url, username, password) = getConfigSettings
    Class.forName(driver)
    val config = new HikariConfig()
    config.setJdbcUrl(url)
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
    val username = config.getString("database.username")
    val password = config.getString("database.password")
    (driver, s"jdbc:mysql://$host", username, password)
  }
}
