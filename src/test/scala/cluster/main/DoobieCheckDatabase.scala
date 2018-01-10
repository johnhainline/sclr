package cluster.main

import doobie._, doobie.implicits._
import cats._, cats.data._, cats.effect.IO, cats.implicits._

object DoobieCheckDatabase {

  def main(args: Array[String]): Unit = {
    val transactor = Transactor.fromDriverManager[IO](
      "com.mysql.jdbc.Driver", "jdbc:mysql://localhost", "root", ""
    )

    sql"SHOW DATABASES".asInstanceOf[Fragment]
      .query[String]        // Query0[String]
      .list                 // ConnectionIO[List[String]]
      .transact(transactor) // IO[List[String]]
      .unsafeRunSync        // List[String]
      .foreach(println)
  }
}
