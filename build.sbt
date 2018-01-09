import sbt.Keys.fork

val akkaVersion = "2.5.8"
val httpVersion = "10.0.11"
val doobieVersion = "0.5.0-M13"

scalacOptions += "-Ypartial-unification" // 2.11.9+


val Frontend = config("frontend") extend Compile
val Worker = config("worker") extend Compile
def customAssemblySettings =
  inConfig(Frontend)(baseAssemblySettings ++
    inTask(assembly)(mainClass := Some("cluster.main.FrontendApp")) ++
    inTask(assembly)(assemblyJarName := "sclr-frontend.jar")) ++
    inConfig(Worker)(baseAssemblySettings ++
      inTask(assembly)(mainClass := Some("cluster.main.WorkerApp")) ++
      inTask(assembly)(assemblyJarName := "sclr-worker.jar"))

lazy val `sclr` = project
  .in(file("."))
  .settings(customAssemblySettings: _*)
  .settings(
    name := "sparse-conditional-linear-regression",
    organization := "wustl.engineering",
    scalaVersion := "2.12.4",
    version := "0.1",

    libraryDependencies ++= Seq(

      // sigar is a os dependent toolset to make akka-cluster-metrics give more details
      "io.kamon" % "sigar-loader" % "1.6.6-rev002",

      "com.lightbend.akka" %% "akka-management-cluster-http" % "0.6",

      "org.scalatest" %% "scalatest" % "3.0.4" % Test,
      "org.scalamock" %% "scalamock" % "4.0.0" % Test,

      "ch.qos.logback" % "logback-classic" % "1.2.3",

      // Doobie, a JDBC functional programming layer
      "org.tpolecat" %% "doobie-core"      % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"    % doobieVersion,
      "org.tpolecat" %% "doobie-specs2"    % doobieVersion % Test,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
      // mysql connection driver
      "mysql" % "mysql-connector-java" % "5.1.45",

      // Weka, a Machine Learning library.
      "nz.ac.waikato.cms.weka" % "weka-stable" % "3.8.2",

      "com.typesafe.akka" %% "akka-http"               % httpVersion,
      "com.typesafe.akka" %% "akka-http-testkit"       % httpVersion   % Test,
      "com.typesafe.akka" %% "akka-actor"              % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit"            % akkaVersion   % Test,
      "com.typesafe.akka" %% "akka-stream"             % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-testkit"     % akkaVersion   % Test,
      "com.typesafe.akka" %% "akka-cluster"            % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics"    % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools"      % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding"   % akkaVersion,
      "com.typesafe.akka" %% "akka-distributed-data"   % akkaVersion,
      "com.typesafe.akka" %% "akka-remote"             % akkaVersion,
      "com.typesafe.akka" %% "akka-distributed-data"   % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion
    ),

    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),

    fork in run := true,
    mainClass in (Compile, run) := Some("cluster.main.LocalApp"),
    parallelExecution in Test := false
)
