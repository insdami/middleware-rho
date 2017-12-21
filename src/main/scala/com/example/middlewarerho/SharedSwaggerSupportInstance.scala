package com.example.middlewarerho

import fs2.Task
import org.http4s.Service
import scala.concurrent.ExecutionContext
import scala.util.Properties.envOrNone
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.util.StreamApp
import org.http4s.rho._
import org.http4s._
import org.http4s.dsl._
import org.http4s.rho.swagger.SwaggerSupport
import cats.implicits._
import fs2.interop.cats._

object SharedSwaggerSupportInstance extends StreamApp {
  val port: Int = envOrNone("HTTP_PORT").fold(8080)(_.toInt)

  val swaggerSupport: RhoMiddleware = SwaggerSupport()
  val service1: HttpService = Middleware.lift(Service1.service.toService(swaggerSupport), Header("SomeKey", "SomeValue"))
  val service2: HttpService = Service2.service.toService(swaggerSupport)

  val services: HttpService = service1 |+| service2

  def stream(args: List[String]): fs2.Stream[Task, Nothing] = BlazeBuilder.bindHttp(port)
    .mountService(Service.withFallback(StaticContentService.routes)(services))
    .serve


  object Service1 {
    val service: RhoService = new RhoService {
      GET / "hello" |>> Ok("Hello world!")
    }
  }


  object Service2 {
    val service: RhoService = new RhoService {
      GET / "goodbye" |>> Ok("Goodbye world!")
    }
  }

  object Middleware {
    def lift(service: HttpService, header: Header): HttpService = Service.lift { req =>
      service(req).map {
        case Status.Successful(resp) =>
          resp.putHeaders(header)
        case resp =>
          resp
      }
    }
  }

  object StaticContentService {

    private val swaggerUiDir = "/swagger-ui"

    def fetchResource(path: String, req: Request): Task[Response] = {
      StaticFile.fromResource(path, Some(req)).getOrElseF(NotFound())
    }

    def routes(implicit executionContext: ExecutionContext = ExecutionContext.global): HttpService = HttpService {
      case req@GET -> Root / "css" / rest =>
        fetchResource(swaggerUiDir + req.pathInfo, req)
      case req@GET -> Root / "images" / rest =>
        fetchResource(swaggerUiDir + req.pathInfo, req)
      case req@GET -> Root / "lib" / rest =>
        fetchResource(swaggerUiDir + req.pathInfo, req)
      case req@GET -> Root / "swagger-ui" =>
        fetchResource(swaggerUiDir + "/index.html", req)
      case req@GET -> Root / "swagger-ui.js" =>
        fetchResource(swaggerUiDir + "/swagger-ui.min.js", req)
    }

  }

}
