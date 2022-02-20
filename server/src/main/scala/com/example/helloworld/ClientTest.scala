package com.example.helloworld

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Ref
import cats.syntax.parallel._
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.examples.helloworld.helloworld.GreeterFs2Grpc
import io.grpc.examples.helloworld.helloworld.HelloRequest
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

object ClientTest extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    for {
      ch <- NettyChannelBuilder.forTarget(sys.env.getOrElse("target", "192.168.1.3:50051")).usePlaintext().resource[IO]
      client <- GreeterFs2Grpc.stubResource[IO](ch)
    } yield for {
      _ <- IO.println("press key " + ProcessHandle.current().pid().toString)
      _ <- IO.readLine
      msg <- IO.pure(HelloRequest())
      result <- IO.ref(List.empty[Result])
      test = log(result, IO.defer(client.sayHello(msg, new Metadata())))
      _ <- startTest(50, 30.seconds)(test)
      _ <- result.getAndSet(List.empty).flatMap(printResult)
      _ <- IO.sleep(2.seconds)
      _ <- startTest(50, 30.seconds)(test)
      _ <- result.getAndSet(List.empty).flatMap(printResult)
    } yield ExitCode.Success
  }.use(identity)

  def startTest(
    par: Int,
    duration: FiniteDuration
  )(action: IO[Unit]): IO[Unit] =
    action.attempt.foreverM
      .parReplicateA(par)
      .void
      .timeout(duration)
      .attempt
      .void

  def printResult(result: List[Result]): IO[Unit] = IO {
    val dist = result.toArray.sorted
    val N = dist.length
    println(s"count: $N")
    println(s"success count: ${dist.count(_.ok)}")
    val totalRun = result.foldLeft(Duration.Zero)(_ + _.duration)
    if (N > 0) {
      println(s"rps: ${N / 30.0}")
      println(s"avg: ${totalRun / N}")
      println(s"min: ${dist(0).duration}")
      println(s"25%: ${dist(N / 4 - 1).duration}")
      println(s"50%: ${dist(N / 2 - 1).duration}")
      println(s"75%: ${dist(3 * N / 4 - 1).duration}")
      println(s"95%: ${dist(95 * N / 100 - 1).duration}")
      println(s"99%: ${dist(99 * N / 100 - 1).duration}")
      println(s"max: ${dist(N - 1).duration}")
    }
  }

  def log[A](ref: Ref[IO, List[Result]], io: IO[A]): IO[Unit] =
    for {
      start <- IO.monotonic
      ret <- io.attempt
      end <- IO.monotonic
      result = Result(start, end, ret.isRight)
      _ <- ref.update(result :: _)
    } yield ()
}

case class Result(start: FiniteDuration, end: FiniteDuration, ok: Boolean) {
  def duration: FiniteDuration = end - start
}

object Result {
  implicit val ord: Ordering[Result] = new Ordering[Result] {
    override def compare(x: Result, y: Result): Int = Ordering[FiniteDuration].compare(x.duration, y.duration)
  }
}