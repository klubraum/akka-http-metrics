/*
 * Copyright 2019 Michel Davit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.davit.akka.http.metrics.core

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ClosedShape
import akka.stream.scaladsl.{GraphDSL, RunnableGraph}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import eu.monniot.scala3mock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class MeterStageSpec
    extends TestKit(ActorSystem("MeterStageSpec"))
    with AnyFlatSpecLike
    with Matchers
    with MockFactory
    with ScalaFutures {

  val request  = HttpRequest()
  val response = HttpResponse()

  trait Fixture {
    val handler = mock[HttpMetricsHandler]

    when(handler.onConnection _)
      .expects()
      .returns((): Unit)

    val (requestIn, requestOut, responseIn, responseOut) = RunnableGraph
      .fromGraph(
        GraphDSL.createGraph(
          TestSource[HttpRequest](),
          TestSink[HttpRequest](),
          TestSource[HttpResponse](),
          TestSink[HttpResponse]()
        )((_, _, _, _)) { implicit builder => (reqIn, reqOut, respIn, respOut) =>
          import GraphDSL.Implicits.*
          val meter = builder.add(new MeterStage(handler))

          reqIn ~> meter.in1
          meter.out1 ~> reqOut
          respIn ~> meter.in2
          meter.out2 ~> respOut
          ClosedShape
        }
      )
      .run()

    // simulate downstream demand
    responseOut.request(1)
    requestOut.request(1)

  }

  "MeterStage" should "call onConnection on materialization and onDisconnection once terminated" in new Fixture {
    when(handler.onDisconnection _)
      .expects()
      .returns((): Unit)

    requestIn.sendComplete()
    requestOut.expectComplete()

    responseIn.sendComplete()
    responseOut.expectComplete()
  }

  it should "call onRequest wen request is offered" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    when(handler.onResponse _)
      .expects(request, response)
      .returns(response)

    responseIn.sendNext(response)
    responseOut.expectNext() shouldBe response
  }

  it should "flush the stream before stopping" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    // close request side
    requestIn.sendComplete()
    requestOut.expectComplete()

    // response should still be accepted
    when(handler.onResponse _)
      .expects(request, response)
      .returns(response)

    responseIn.sendNext(response)
    responseOut.expectNext() shouldBe response
  }

  it should "propagate error from request in" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    val error = new Exception("BOOM!")
    requestIn.sendError(error)
    requestOut.expectError(error)
  }

  it should "propagate error from request out" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestOut.expectNext() shouldBe request

    val error = new Exception("BOOM!")
    requestOut.cancel(error)
    requestIn.expectCancellation()

  }

  it should "terminate and fail pending" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestIn.sendComplete()
    requestOut.expectNext() shouldBe request
    requestOut.expectComplete()

    when(handler.onFailure _)
      .expects(request, MeterStage.PrematureCloseException)
      .returns(MeterStage.PrematureCloseException)

    responseIn.sendComplete()
    responseOut.expectComplete()
  }

  it should "propagate error from response in and fail pending" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestIn.sendComplete()
    requestOut.expectNext() shouldBe request
    requestOut.expectComplete()

    val error = new Exception("BOOM!")
    when(handler.onFailure _)
      .expects(request, error)
      .returns(error)

    responseIn.sendError(error)
    responseOut.expectError(error)
  }

  it should "propagate error from response out and fail pending" in new Fixture {

    when(handler.onRequest _)
      .expects(request)
      .returns(request)

    requestIn.sendNext(request)
    requestIn.sendComplete()
    requestOut.expectNext() shouldBe request
    requestOut.expectComplete()

    val error = new Exception("BOOM!")
    when(handler.onFailure _)
      .expects(request, error)
      .returns(error)

    responseOut.cancel(error)
    responseIn.expectCancellation()
  }

}
