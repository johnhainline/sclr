import com.typesafe.sbt.packager.docker._
import sbt.Keys.fork

val akkaVersion = "2.5.8"
val httpVersion = "10.0.11"
val doobieVersion = "0.5.0"

scalacOptions += "-Ypartial-unification" // 2.11.9+

organization in ThisBuild := "wustl.engineering"
scalaVersion in ThisBuild := "2.12.4"
version in ThisBuild := "0.8.2"

lazy val sclr = project
  .in(file("."))
  .settings(
  name := "sclr",
  description := "sparse-conditional-linear-regression",

  libraryDependencies ++= Seq(
    // sigar is a os dependent toolset to make akka-cluster-metrics give more details
    "io.kamon" % "sigar-loader" % "1.6.6-rev002",

    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.scalamock" %% "scalamock" % "4.0.0" % Test,

    // Logging
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    // Doobie, a JDBC functional programming layer
    "org.tpolecat" %% "doobie-core"      % doobieVersion,
    "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
    "org.tpolecat" %% "doobie-specs2"    % doobieVersion % Test,
    "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
    // mysql connection driver
    "mysql" % "mysql-connector-java" % "5.1.45",

    "com.typesafe.akka" %% "akka-http"             % httpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json"  % httpVersion,
    "com.typesafe.akka" %% "akka-http-testkit"     % httpVersion % Test,
    "com.typesafe.akka" %% "akka-actor"            % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit"          % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream"           % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit"   % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-cluster"          % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-metrics"  % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools"    % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote"           % akkaVersion,
    "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion
  ),

  javaOptions in run ++= Seq("-Djava.library.path=./target/native"),

  fork in run := true,
  parallelExecution in Test := false
)

val dockerSettings = Seq(
  dockerRepository := Some("us.gcr.io"),
  dockerUpdateLatest := true,
  dockerBaseImage := "local/openjdk-custom",
  maintainer := "John Hainline (john.hainline@wustl.edu)",
  packageSummary := "SCLR Server",
  packageDescription := "Sparse Conditional Linear Regression"
)

val clusterName = "sclr-01"

lazy val frontend = project
  .in(file("frontend"))
  .settings(dockerSettings: _*)
  .settings(
    name := "sclr-frontend",
    mainClass := Some("cluster.main.FrontendApp"),
    packageName in Docker := s"$clusterName/frontend"
  )
  .dependsOn(sclr)
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)

lazy val manage = project
  .in(file("manage"))
  .settings(dockerSettings: _*)
  .settings(
    name := "sclr-manage",
    mainClass := Some("cluster.main.ManageApp"),
    packageName in Docker := s"$clusterName/manage"
  )
  .dependsOn(sclr)
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)

lazy val compute = project
  .in(file("compute"))
  .settings(dockerSettings: _*)
  .settings(
    name := "sclr-compute",
    mainClass := Some("cluster.main.ComputeApp"),
    packageName in Docker := s"$clusterName/compute"
  )
  .dependsOn(sclr)
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
