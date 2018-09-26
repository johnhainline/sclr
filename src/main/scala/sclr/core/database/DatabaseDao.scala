package sclr.core.database

import java.io.{BufferedReader, InputStreamReader}
import java.sql.{PreparedStatement, ResultSet, SQLException, Statement, Timestamp}
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

import scala.reflect.ClassTag


case class XYZ(id: Int, x: Array[Boolean], y: Array[Double], z: Double)
case class Dataset(data: Array[XYZ], xLength: Int, yLength: Int)
case class DatasetInfo(xLength: Int, yLength: Int, rowCount: Int)
case class Result(index: Int, dimensions: Array[Int], rows: Array[Int], coefficients: Array[Double], error: Option[Double], kDNF: Option[String]) {
  override def toString: String = {
    import scala.runtime.ScalaRunTime._
    s"Result(index=$index, dims=${stringOf(dimensions)}, rows=${stringOf(rows)}, coeffs=${stringOf(coefficients)}, error=$error, kdnf=$kDNF)"
  }
}

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
          val yOffset = xOffset + info.xLength + 1
          val zOffset = yOffset + info.yLength
          val x = getArrayFromResultSet(results, xOffset, info.xLength, results.getBoolean)
          val y = getArrayFromResultSet(results, yOffset, info.yLength, results.getDouble)
          val z = results.getDouble(zOffset)
          data(dataIndex) = XYZ(id, x, y, z)
          dataIndex += 1
        }
        Dataset(data, info.xLength, info.yLength)
      } catch {
        case e: SQLException =>
          logger.error("Could not get dataset from DB.", e)
          throw e
        case e: Exception =>
          throw e
      } finally {
        if (statement != null) statement.close()
      }
    }.transact(xa).unsafeRunSync()
  }

  def getBestResult(xa: Transactor[IO], name: String, yDimensions: Int, rows: Int): Result = {
    var statement: Statement = null
    FC.raw { connection =>
      try {
        statement = connection.createStatement()
        // Ex: (id, work_index, error, dim0, dim1, row0, row1, coeff0, coeff1, kdnf, created_at)
        val results = statement.executeQuery(s"SELECT * FROM $name.results WHERE error = (SELECT MIN(error) from $name.results)")
        assert(results.next())
        val id = results.getInt(1)
        val dimArray = getArrayFromResultSet(results, index = 4, yDimensions, results.getInt)
        val rowArray = getArrayFromResultSet(results, index = 6, yDimensions, results.getInt)
        val coeffArray = getArrayFromResultSet(results, index = 8, yDimensions, results.getDouble)
        val error = Option(results.getDouble("error"))
        val kdnf = Option(results.getString("kdnf"))
        Result(id, dimArray, rowArray, coeffArray, error, kdnf)
      } catch {
        case e: SQLException =>
          logger.error("Could not get best result from DB.", e)
          throw e
      } finally {
        if (statement != null) statement.close()
      }
    }.transact(xa).unsafeRunSync()
  }

  def getResultCount(xa: Transactor[IO], name: String): Long = {
    sql"SELECT COUNT(*) FROM $name.results".query[Long].unique.transact(xa).unsafeRunSync()
  }

  private def getArrayFromResultSet[T](result: ResultSet, index: Int, count: Int, getter: Int => T)(implicit m: ClassTag[T]): Array[T] = {
    val array = new Array[T](count)
    for (i <- array.indices) {
      array(i) = getter(index + i)
    }
    array
  }

  private def createInsertStatement(schema: String, result: Result): String = {
    val names = Vector(indexColumn, errorColumn,
      dimensionNames(result.dimensions.length).mkString(","),
      rowNames(result.rows.length).mkString(","),
      coeffNames(result.coefficients.length).mkString(","),
      "kdnf"
    ).mkString("(", ",", ")")

    val totalItems = 2 + result.dimensions.length + result.rows.length + result.coefficients.length + 1
    val questionMarks = (1 to totalItems).map(_ => "?").mkString("(", ",", ")")

    s"INSERT INTO $schema.results $names VALUES $questionMarks"
  }

  def insertResults(xa: Transactor[IO], schema: String)(results: Seq[Result]): Unit = {
    assert(results.nonEmpty)

    val insertString = createInsertStatement(schema, results.head)
    var statement: PreparedStatement = null
    FC.raw { connection =>
      try {
        val autoCommit = connection.getAutoCommit
        connection.setAutoCommit(false)
        statement = connection.prepareStatement(insertString)
        var inserted = 0
        for (result <- results) {
          // Ex L2Norm: (work_index, error, dim0, dim1, row0, row1, coeff0, coeff1, kdnf)
          // Ex SupNorm: (work_index, error, dim0, dim1, row0, row1, row2, coeff0, coeff1, kdnf)
          var index = 1
          statement.setInt(index, result.index)
          index += 1
          if (result.error.nonEmpty) {
            statement.setDouble(index, result.error.get)
          } else {
            statement.setNull(index, java.sql.Types.DOUBLE)
          }
          index += 1
          for (d <- result.dimensions) {
            statement.setInt(index, d)
            index += 1
          }
          for (r <- result.rows) {
            statement.setInt(index, r)
            index += 1
          }
          for (c <- result.coefficients) {
            statement.setDouble(index, c)
            index += 1
          }
          if (result.kDNF.nonEmpty) {
            statement.setString(index, result.kDNF.get)
          } else {
            statement.setNull(index, java.sql.Types.LONGNVARCHAR)
          }
          index += 1
          statement.addBatch()

          inserted += 1
          if (inserted % 1024 == 0) {
            statement.executeBatch()
          }
        }

        statement.executeBatch()

        connection.commit()
        connection.setAutoCommit(autoCommit)
      } catch {
        case e: SQLException =>
          logger.error("Could not save results to DB.", e)
          throw e
      } finally {
        if (statement != null) statement.close()
      }
    }.transact(xa).unsafeRunSync()
  }

  def insertResult(xa: Transactor[IO], schema: String)(result: Result): Unit = insertResults(xa, schema)(List(result))

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
