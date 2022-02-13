
name := "fs2-grpc-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.8"

run / fork := true

enablePlugins(Fs2Grpc)
enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
  "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion
)


bashScriptExtraDefines ++= Seq(
  "addJava -XX:MinRAMPercentage=70",
  "addJava -XX:MaxRAMPercentage=70",
)

dockerBaseImage := "amazoncorretto:17"


dockerCommands := {
  import com.typesafe.sbt.packager.docker._
  val s = dockerCommands.value
  val mainStage = s.indexWhere {
    case Cmd("FROM", s@_*) if s.last.endsWith("mainstage") => true
    case _ => false
  }
  val (pre, pst) = s.splitAt(mainStage + 1)
  pre ++ Seq(ExecCmd("RUN", "yum install -y shadow-utils".split(" "):_*)) ++ pst
}

dockerExposedPorts := Seq(50051)
// dependencyOverrides ++= Seq(
//   "org.typelevel" %% "fs2-grpc-runtime" % "2.4.4-15-6dd15b8-SNAPSHOT"
// )

//with streaming
// dependencyOverrides ++= Seq(
//   "org.typelevel" %% "fs2-grpc-runtime" % "2.4.4-15-fe1fe39-20220212T060255Z-SNAPSHOT"
// )

javaOptions ++= Seq(
  // "-XX:+UseParallelGC",
  // "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"
)
