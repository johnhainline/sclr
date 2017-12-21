import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import sbt.Keys.fork

val akkaVersion = "2.5.8"
val httpVersion = "10.0.11"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

lazy val `sparce-cond-linear-reg` = project
  .in(file("."))
  .settings(multiJvmSettings: _*)
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
    mainClass in (Compile, run) := Some("cluster.sclr.LocalDeploy"),
    parallelExecution in Test := false
).configs(MultiJvm)





