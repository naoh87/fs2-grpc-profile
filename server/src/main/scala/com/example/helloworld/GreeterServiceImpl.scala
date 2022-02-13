package com.example.helloworld

import cats.effect.IO
import io.grpc.Metadata
import io.grpc.examples.helloworld.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.helloworld.{GreeterFs2Grpc, HelloReply, HelloRequest}
import io.grpc.stub.StreamObserver
import scala.concurrent.Future
import fs2._


class GreeterServiceImpl extends GreeterFs2Grpc[IO, io.grpc.Metadata] {
  override def sayHello(request: HelloRequest, ctx: io.grpc.Metadata): IO[HelloReply] =
    IO.pure(HelloReply(request.request))

  override def sayHelloSS(request: HelloRequest, ctx: Metadata): fs2.Stream[IO, HelloReply] =
    Stream.emit(HelloReply(request.request)).repeatN(100)

  override def sayHelloCS(request: fs2.Stream[IO, HelloRequest], ctx: Metadata): IO[HelloReply] =
    request.compile.last.map(_.fold(HelloReply())(r => HelloReply(r.request)))

  override def sayHelloBS(request: Stream[IO, HelloRequest], ctx: Metadata): Stream[IO, HelloReply] =
    request.map(r => HelloReply(r.request))
}

class GreeterServiceImplPb extends GreeterGrpc.Greeter {
  override def sayHello(request: HelloRequest): Future[HelloReply] =
    Future.successful(HelloReply(request.request))

  override def sayHelloSS(request: HelloRequest, responseObserver: StreamObserver[HelloReply]): Unit = {
    (0 until 100).foreach(_ => responseObserver.onNext(HelloReply(request.request)))
    responseObserver.onCompleted()
  }

  override def sayHelloCS(responseObserver: StreamObserver[HelloReply]): StreamObserver[HelloRequest] = {
    new StreamObserver[HelloRequest] {
      var received: HelloRequest = _
      override def onNext(v: HelloRequest): Unit = received = v

      override def onError(throwable: Throwable): Unit =
        responseObserver.onError(throwable)

      override def onCompleted(): Unit =
        responseObserver.onNext(Option(received).fold(HelloReply())(r => HelloReply(r.request)))
    }
  }

  override def sayHelloBS(responseObserver: StreamObserver[HelloReply]): StreamObserver[HelloRequest] =
    new StreamObserver[HelloRequest] {
      override def onNext(v: HelloRequest): Unit = responseObserver.onNext(HelloReply(v.request))

      override def onError(throwable: Throwable): Unit = responseObserver.onError(throwable)

      override def onCompleted(): Unit = responseObserver.onCompleted()
    }
}
