
name := "fs2-grpc-quickstart-scala"

version := "2.4.4"

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
dependencyOverrides ++= Seq(
   "org.typelevel" %% "fs2-grpc-runtime" % version.value
)

javaOptions ++= Seq(
  // "-XX:+UseParallelGC",
  // "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"
)
