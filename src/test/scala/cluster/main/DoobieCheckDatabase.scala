package cluster.main

import cats.effect.IO
import doobie._
import doobie.implicits._

object DoobieCheckDatabase {

  def main(args: Array[String]): Unit = {
    val transactor = Transactor.fromDriverManager[IO](
      "com.mysql.jdbc.Driver", "jdbc:mysql://localhost", "root", ""
    )

    sql"SHOW DATABASES"
      .query[String]        // Query0[String]
      .to[List]             // ConnectionIO[List[String]]
      .transact(transactor) // IO[List[String]]
      .unsafeRunSync        // List[String]
      .foreach(println)
  }
}
