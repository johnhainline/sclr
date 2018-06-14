package sclr.main

import java.sql.{Connection, DriverManager}

object JDBCCheckDatabase {
  def main(args: Array[String]): Unit = {
    // connect to the database named "mysql" on the localhost
    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://localhost"
    val username = "root"
    val password = ""

    // there's probably a better way to do this
    var connection:Connection = null

    try {
      // make the connection
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)

      // create the statement, and run the select query
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SHOW DATABASES")
      while (resultSet.next()) {
        val name = resultSet.getString("Database")
        println(s"name = $name")
      }
    } catch {
      case e: Exception => e.printStackTrace()
    }
    connection.close()
  }
}
