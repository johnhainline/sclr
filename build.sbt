name := "sparce-cond-linear-reg"

organization := "wustl.engineering"

version := "0.1"

scalaVersion := "2.12.4"

val akkaVersion = "2.5.8"
val httpVersion = "10.0.11"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= Seq(
  // sigar is a os dependent toolset to make akka-cluster-metrics give more details
  "io.kamon" % "sigar-loader" % "1.6.6-rev002",

  "com.lightbend.akka" %% "akka-management-cluster-http" % "0.6",

  "org.scalatest" %% "scalatest" % "3.0.4" % Test,

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
)

fork in run := true

mainClass in (Compile, run) := Some("cluster.transformation.TransformationApp")

// disable parallel tests
parallelExecution in Test := false
