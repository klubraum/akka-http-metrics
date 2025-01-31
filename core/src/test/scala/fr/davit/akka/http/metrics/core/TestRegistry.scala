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

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import fr.davit.akka.http.metrics.core.HttpMetricsNames.HttpMetricsNamesImpl
import fr.davit.akka.http.metrics.core.HttpMetricsSettings.HttpMetricsSettingsImpl

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object TestRegistry {

  val settings: HttpMetricsSettings = HttpMetricsSettingsImpl(
    "", // not used
    HttpMetricsNamesImpl("", "", "", "", "", "", "", "", "", ""), // not used
    _.status.isInstanceOf[StatusCodes.ServerError],
    includeMethodDimension = false,
    includePathDimension = false,
    includeStatusDimension = false
  )

  implicit val marshaller: ToEntityMarshaller[TestRegistry] = Marshaller.opaque(_ => HttpEntity.Empty)

  private def keyer(dimensions: Seq[Dimension]): String = dimensions
    .map(d => d.name + "=" + d.label)
    .mkString(":")

  class TestCounter extends Counter {
    protected val acc = mutable.Map[String, Long]()

    override def inc(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(v) => acc += (key -> (v + 1))
        case None    => acc += (key -> 1)
      }
    }

    def value(dimensions: Seq[Dimension] = Seq.empty): Long = acc.getOrElse(keyer(dimensions), 0)
  }

  class TestGauge extends TestCounter with Gauge {

    override def dec(dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(v) => acc += (key -> (v - 1))
        case None    => acc += (key -> -1)
      }
    }
  }

  class TestTimer extends Timer {
    protected val acc = mutable.Map[String, List[FiniteDuration]]()

    override def observe(duration: FiniteDuration, dimensions: Seq[Dimension] = Seq.empty): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(vs) => acc += (key -> (duration :: vs))
        case None     => acc += (key -> (duration :: Nil))
      }
    }

    def values(dimensions: Seq[Dimension] = Seq.empty): List[FiniteDuration] = acc.getOrElse(keyer(dimensions), Nil)
  }

  final class TestHistogram extends Histogram {
    protected val acc = mutable.Map[String, List[Long]]()

    override def update[T](value: T, dimensions: Seq[Dimension] = Seq.empty)(implicit numeric: Numeric[T]): Unit = {
      val key = keyer(dimensions)
      acc.get(key) match {
        case Some(vs) => acc += (key -> (numeric.toLong(value) :: vs))
        case None     => acc += (key -> (numeric.toLong(value) :: Nil))
      }
    }

    def values(dimensions: Seq[Dimension] = Seq.empty): List[Long] = acc.getOrElse(keyer(dimensions), Nil)
  }

}

final class TestRegistry(settings: HttpMetricsSettings = TestRegistry.settings) extends HttpMetricsRegistry(settings) {

  import TestRegistry.*

  override val requests: TestCounter         = new TestCounter
  override val requestsActive: TestGauge     = new TestGauge
  override val requestsFailures: TestCounter = new TestCounter
  override val requestsSize: TestHistogram   = new TestHistogram
  override val responses: TestCounter        = new TestCounter
  override val responsesErrors: TestCounter  = new TestCounter
  override val responsesDuration: TestTimer  = new TestTimer
  override val responsesSize: TestHistogram  = new TestHistogram
  override val connections: TestCounter      = new TestCounter
  override val connectionsActive: TestGauge  = new TestGauge
}
