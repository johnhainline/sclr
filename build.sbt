import sbt.Keys.fork

val akkaVersion = "2.5.14"
val httpVersion = "10.1.1"
val doobieVersion = "0.5.3"

organization in ThisBuild := "wustl.engineering"
scalaVersion in ThisBuild := "2.12.6"
version in ThisBuild := "1.8.9"

scalacOptions in ThisBuild += "-Ypartial-unification" // 2.11.9+
scalacOptions in ThisBuild += "-Xlint" // Get more warnings

val dockerSettings = Seq(
  dockerRepository := Some("us.gcr.io"),
  dockerUpdateLatest := true,
  dockerBaseImage := "local/openjdk-custom",
  maintainer := "John Hainline (john.hainline@wustl.edu)",
  packageSummary := "SCLR Server",
  packageDescription := "Sparse Conditional Linear Regression"
)

lazy val sclr = project
  .in(file("."))
  .settings(dockerSettings: _*)
  .settings(
    name := "sclr",
    description := "sparse-conditional-linear-regression",
    mainClass := Some("cluster.main.Sclr"),
    packageName in Docker := "washu-seas-mltheory/sclr",

    resolvers ++= Seq(
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
    ),

    libraryDependencies ++= Seq(
      // sigar is a os dependent toolset to make akka-cluster-metrics give more details
//      "io.kamon" % "sigar-loader" % "1.6.6-rev002",

      // scallop is a command line parser so we get pretty "main" args.
      "org.rogach" %% "scallop" % "3.1.2",

      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.scalamock" %% "scalamock" % "4.1.0" % Test,

      // Logging
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      // Doobie, a JDBC functional programming layer
      "org.tpolecat"  %% "doobie-core"      % doobieVersion,
      "org.tpolecat"  %% "doobie-hikari"    % doobieVersion,
      "org.tpolecat"  %% "doobie-specs2"    % doobieVersion % Test,
      "org.tpolecat"  %% "doobie-scalatest" % doobieVersion % Test,
      // mysql connection driver
      "mysql" % "mysql-connector-java" % "5.1.45",

      // JMXMP connector, used for a remote JMX connection (when debugging using VisualVM)
      "org.glassfish.external" % "opendmk_jmxremote_optional_jar" % "1.0-b01-ea",

      "com.typesafe.akka" %% "akka-http"               % httpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"    % httpVersion,
      "com.typesafe.akka" %% "akka-http-testkit"       % httpVersion % Test,

      "com.typesafe.akka" %% "akka-actor"              % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit"            % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream"             % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-contrib"     % "0.9",
      "com.typesafe.akka" %% "akka-stream-testkit"     % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-cluster"            % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics"    % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools"      % akkaVersion,
      "com.typesafe.akka" %% "akka-remote"             % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion
    ),

    javaOptions in run ++= Seq(
      "-Djava.library.path=./target/native"
    ),

    fork in run := true
  )
  .enablePlugins(MultiJvmPlugin, JavaServerAppPackaging, DockerPlugin)
  .configs(MultiJvm)

// Benchmarks
lazy val bench = project
  .in(file("bench"))
  .settings(
    name := "bench",
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    logBuffered := false,
    libraryDependencies ++= Seq(
      "com.storm-enroute" %% "scalameter" % "0.9" % Test
    ),
    parallelExecution in Test := false
  )
  .dependsOn(sclr)
