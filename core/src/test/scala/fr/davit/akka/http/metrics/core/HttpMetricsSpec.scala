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
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import eu.monniot.scala3mock.functions.MockFunctions.mockFunction
import eu.monniot.scala3mock.matchers.MatchAny
import eu.monniot.scala3mock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class HttpMetricsSpec
    extends TestKit(ActorSystem("HttpMetricsSpec"))
    with AnyFlatSpecLike
    with Matchers
    with ScalaFutures
    with MockFactory
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = system.dispatcher

  abstract class Fixture[T] {
    val *              = MatchAny()
    val metricsHandler = mock[HttpMetricsHandler]
    val server         = mockFunction[RequestContext, Future[RouteResult]]

    when(metricsHandler.onConnection _)
      .expects()
      .returns((): Unit)

    when(metricsHandler.onDisconnection _)
      .expects()
      .returns((): Unit)

    val (source, sink) = TestSource[HttpRequest]()
      .via(HttpMetrics.meterFlow(metricsHandler).join(HttpMetrics.metricsRouteToFlow(server)))
      .toMat(TestSink[HttpResponse]())(Keep.both)
      .run()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "HttpMetrics" should "provide newMeteredServerAt extension" in {
    """
      |import akka.http.scaladsl.Http
      |import fr.davit.akka.http.metrics.core.HttpMetrics._
      |val registry = new TestRegistry(TestRegistry.settings)
      |implicit val system: ActorSystem = ActorSystem()
      |Http().newMeteredServerAt("localhost", 8080, registry)
    """.stripMargin should compile
  }

  it should "seal route mark unhandled requests" in {
    {
      val handler  = HttpMetrics.metricsRouteToFunction(reject)
      val response = handler(HttpRequest()).futureValue
      response.attributes(PathLabeler.key) shouldBe "unhandled"
    }

    {
      val handler  = HttpMetrics.metricsRouteToFunction(failWith(new Exception("BOOM!")))
      val response = handler(HttpRequest()).futureValue
      response.attributes(PathLabeler.key) shouldBe "unhandled"
    }
  }

  it should "call the metrics handler on connection" in new Fixture {
    sink.request(1)
    source.sendComplete()
    sink.expectComplete()
  }

  it should "call the metrics handler on handled requests" in new Fixture {
//    val request  = CaptureOne[HttpRequest]()
//    val response = CaptureOne[HttpResponse]()
    when(metricsHandler.onRequest _)
//      .expects(capture(request))
      .expects(*)
      .onCall { req => req }

    server
      .expects(*)
      .onCall(complete(StatusCodes.OK))

    when(metricsHandler.onResponse _)
//      .expects(*, capture(response))
      .expects(*, *)
      .onCall { (_: HttpRequest, resp: HttpResponse) => resp }

    sink.request(1)
    source.sendNext(HttpRequest())
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    val expected = Marshal(StatusCodes.OK)
      .to[HttpResponse]
      .futureValue

//    response.value shouldBe expected
  }

  it should "call the metrics handler on rejected requests" in new Fixture {
//    val request  = CaptureOne[HttpRequest]()
//    val response = CaptureOne[HttpResponse]()
    when(metricsHandler.onRequest _)
//      .expects(capture(request))
      .expects(*)
      .onCall { req => req }

    server
      .expects(*)
      .onCall(reject)

    when(metricsHandler.onResponse _)
//      .expects(*, capture(response))
      .expects(*, *)
      .onCall { (_: HttpRequest, resp: HttpResponse) => resp }

    sink.request(1)
    source.sendNext(HttpRequest())
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    val expected = Marshal(StatusCodes.NotFound -> "The requested resource could not be found.")
      .to[HttpResponse]
      .futureValue
      .addAttribute(PathLabeler.key, "unhandled")
//    response.value shouldBe expected
  }

  it should "call the metrics handler on error requests" in new Fixture {
//    val request  = CaptureOne[HttpRequest]()
//    val response = CaptureOne[HttpResponse]()
    when(metricsHandler.onRequest _)
//      .expects(capture(request))
      .expects(*)
      .onCall { req => req }

    server
      .expects(*)
      .onCall(failWith(new Exception("BOOM!")))

    when(metricsHandler.onResponse _)
//      .expects(*, capture(response))
      .expects(*, *)
      .onCall { (_: HttpRequest, resp: HttpResponse) => resp }

    sink.request(1)
    source.sendNext(HttpRequest())
    sink.expectNext()

    source.sendComplete()
    sink.expectComplete()

    val expected = Marshal(StatusCodes.InternalServerError)
      .to[HttpResponse]
      .futureValue
      .addAttribute(PathLabeler.key, "unhandled")
//    response.value shouldBe expected
  }

}
