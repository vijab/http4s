package org.http4s
package server

import org.http4s.server.MockServer._
import org.http4s.server.middleware.PushSupport
import org.specs2.matcher.OptionMatchers
import scodec.bits.ByteVector

import scalaz.concurrent.Task
import scalaz.stream.Process._

class MockServerSpec extends Http4sSpec with OptionMatchers {

  val server = new MockServer(PushSupport(MockRoute.route()))

  private implicit class respToString(t: Task[MockResponse]) {
    def getString = new String(t.run.body)
    def getStatus = t.run.statusLine
  }

  "A mock server" should {
    "handle matching routes" in {
      val body = emitSeq(List("one", "two", "three")).map(s => ByteVector(s.getBytes))
      val req = Request(requestMethod = Method.POST, requestUri = uri("/echo"))

      server(req).getString must_== ("onetwothree")
    }

    "give a 'not found' for a match error" in {
      val req = Request(requestUri = uri("/doesntexist/neverwill"))
      server(req).getStatus must_== (Status.NotFound)
    }

    "handle exceptions with an InternalServiceError" in {
      val req = Request(requestUri = uri("/fail"))
      server(req).getStatus must_== (Status.InternalServerError)
    }

    "Get middleware attributes (For PushSupport)" in {
      val req = Request(requestUri = uri("/push"))
      val returned = server(req).run
      val pushOptions = returned.attributes.get(PushSupport.pushResponsesKey)

      pushOptions must beSome

      val pushResponder = pushOptions.get.run
      pushResponder.length must_== (1)

      pushResponder(0).location must_==("/ping")
    }
  }
}

