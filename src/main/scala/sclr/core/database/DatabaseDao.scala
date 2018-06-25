package sclr.core.database

import java.io.{BufferedReader, InputStreamReader}
import java.sql.{SQLException, Statement, Timestamp}
import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import sclr.core.ScriptRunner
import sclr.core.database.DatabaseDao.{coeffNames, dimensionNames, rowNames}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie._


case class XYZ(id: Int, x: Array[Boolean], y: Array[Double], z: Double)
case class Dataset(data: Array[XYZ], xLength: Int, yLength: Int)
case class DatasetInfo(xLength: Int, yLength: Int, rowCount: Int)
case class Result(index: Int, dimensions: Vector[Int], rows: Vector[Int], coefficients: Vector[Double], error: Option[Double], kDNF: Option[String])

class DatabaseDao extends LazyLogging {

  private val indexColumn = "work_index"
  private val errorColumn = "error"

  def clearDataset(xa: Transactor[IO], name: String): Long = {
    (sql"DROP SCHEMA IF EXISTS " ++ Fragment.const(name)).update.run.transact(xa).unsafeRunSync()
  }

  def initializeDataset(xa: Transactor[IO], name: String): Unit = {
    FC.raw { connection =>
      val runner = new ScriptRunner(connection, false, false)
      val file = new BufferedReader(new InputStreamReader(getClass.getClassLoader.getResourceAsStream(s"datasets/$name.sql")))
      runner.runScript(file)
    }.transact(xa).unsafeRunSync()
  }

  def getDatasetInfo(xa: Transactor[IO], name: String): DatasetInfo = {
    val xDimensionsQuery = (fr"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = " ++
      Fragment.const(s"""\"$name\"""") ++ fr" AND table_name = " ++ Fragment.const(""""x"""")).query[Int]
    // We take -1 off the dimensions to account for our id primary key column.
    val xDimensionCount = xDimensionsQuery.unique.transact(xa).unsafeRunSync() - 1

    val yDimensionsQuery = (fr"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = " ++
      Fragment.const(s"""\"$name\"""") ++ fr" AND table_name = " ++ Fragment.const(""""yz"""")).query[Int]
    // We take -2 off the dimensions to account for our id primary key and z columns.
    val yDimensionCount = yDimensionsQuery.unique.transact(xa).unsafeRunSync() - 2

    val xRowsQuery = Fragment.const(s"SELECT COUNT(*) FROM $name.x").query[Int]
    val xRowCount = xRowsQuery.unique.transact(xa).unsafeRunSync()

    DatasetInfo(xDimensionCount, yDimensionCount, xRowCount)
  }

  def getDataset(xa: Transactor[IO], name: String): Dataset = {
//    val data = sql"SELECT * FROM $name.x, $name.yz WHERE $name.x.id = $name.yz.id".query[XYZ].to[Array].transact(xa).unsafeRunSync()
//    Dataset(data, data.head.x.length, data.head.y.length)

    val info = getDatasetInfo(xa, name)
    var statement: Statement = null
    FC.raw { connection =>
      try {
        statement = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY)
        statement.setFetchSize(500)
        val data = new Array[XYZ](info.rowCount)
        var dataIndex = 0
        // Ex: (id, x1, x2, x3, id, y1, y2, z)
        val results = statement.executeQuery(s"SELECT * FROM $name.x, $name.yz WHERE $name.x.id = $name.yz.id")
        while (results.next()) {
          val id = results.getInt(1)
          val xOffset = 2
          val x = new Array[Boolean](info.xLength)
          for (i <- x.indices) {
            x(i) = results.getBoolean(xOffset + i)
          }
          val yOffset = xOffset + info.xLength + 1 // +1 skips the extra "id" column
          val y = new Array[Double](info.yLength)
          for (i <- y.indices) {
            y(i) = results.getDouble(yOffset + i)
          }
          val z = results.getDouble(yOffset + info.yLength)
          data(dataIndex) = XYZ(id, x, y, z)
          dataIndex += 1
        }
        Dataset(data, info.xLength, info.yLength)
      } catch {
        case e: SQLException =>
          logger.error("Could not get dataset from DB.", e)
          throw e
      } finally {
        if (statement != null) statement.close()
      }
    }.transact(xa).unsafeRunSync()
  }

//  def getResults() = {
//    import DatabaseDao.ZonedDateTimeMeta
//    sql"SELECT id, data, created_at FROM results".query[Result].list.transact(xa).unsafeRunSync()
//  }
  def getResultCount(xa: Transactor[IO], name: String): Long = {
    sql"SELECT COUNT(*) FROM $name.results".query[Long].unique.transact(xa).unsafeRunSync()
  }


  private def getInsertNames(result: Result) = {
    Fragment.const(Vector(indexColumn, errorColumn,
      dimensionNames(result.dimensions.length).mkString(","),
      rowNames(result.rows.length).mkString(","),
      coeffNames(result.coefficients.length).mkString(","),
      "kdnf"
    ).mkString("(", ",", ")"))
  }

  private val reducer = { (l:Fragment, r:Fragment) => l ++ r}
  private def getInsertValues(result: Result) = {
    val fragmentValues =
      fr"${result.index}, ${result.error}" ++
      result.dimensions.map(d => fr", $d").reduce(reducer) ++
      result.rows.map(r => fr", $r").reduce(reducer) ++
      result.coefficients.map(c => fr", $c").reduce(reducer)
    Fragment.const("(") ++ fragmentValues ++ sql", ${result.kDNF}" ++ Fragment.const(")")
  }

  def insertResults(xa: Transactor[IO], schema: String)(results: Seq[Result]): Int = {
    assert(results.nonEmpty)
    val names = getInsertNames(results.head)
    val values = results.map(getInsertValues)
    val aggregateValues = values.reduce[Fragment] { case (frag1, frag2) =>
      frag1 ++ fr"," ++ frag2
    }
    val dbUpdate = (fr"INSERT INTO " ++ Fragment.const(s"$schema.results") ++
      names ++ fr"VALUES" ++ aggregateValues).update
    dbUpdate.run.transact(xa).unsafeRunSync()
  }

  def insertResult(xa: Transactor[IO], schema: String)(result: Result): Int = insertResults(xa, schema)(List(result))

  def setupSchemaAndTable(xa: Transactor[IO], schema: String, yDimensions: Int, rows: Int): Int = {
    setupSchema(xa, schema)
    try {
      val tableDims = dimensionNames(yDimensions).map(name => s"$name INT NOT NULL")
      val tableRows = rowNames(rows).map(name => s"$name INT NOT NULL")
      val tableCoeffs = coeffNames(yDimensions).map(name => s"$name DOUBLE NOT NULL")
      val fragment = fr"CREATE TABLE IF NOT EXISTS" ++
        Fragment.const(s"$schema.results") ++
        Fragment.const((
          Vector("id BIGINT NOT NULL AUTO_INCREMENT", s"$indexColumn BIGINT NOT NULL", s"$errorColumn DOUBLE") ++
            tableDims ++ tableRows ++ tableCoeffs ++
            Vector("kdnf TEXT DEFAULT NULL", "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", "PRIMARY KEY (id)")
          ).mkString("(", ",", ")"))
      fragment.update.run.transact(xa).unsafeRunSync()
    } catch {
      case e: Exception =>
        logger.error("Could not create table.", e)
        throw e
    }
  }

  private def setupSchema(xa: Transactor[IO], schema: String) = {
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

  def makeHikariTransactor(poolSize: Int = 2): HikariTransactor[IO] = {
    val (driver, url, username, password) = getConfigSettings
    Class.forName(driver)
    val config = new HikariConfig()
    config.setJdbcUrl(url)
    config.setUsername(username)
    config.setPassword(password)
    config.setAutoCommit(false)
    config.setMaximumPoolSize(poolSize)
    HikariTransactor[IO](new HikariDataSource(config))
  }

  def makeSingleTransactor(): Transactor[IO] = {
    val (driver, url, username, password) = getConfigSettings
    Transactor.fromDriverManager[IO](driver, url, username, password)
  }

  private def getConfigSettings: (String, String, String, String) = {
    val config = ConfigFactory.load()
    val driver   = config.getString("database.driver")
    val host     = config.getString("database.host")
    val username = config.getString("database.username")
    val password = config.getString("database.password")
    (driver, s"jdbc:mysql://$host", username, password)
  }
}