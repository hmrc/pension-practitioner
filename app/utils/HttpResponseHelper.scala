/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import com.fasterxml.jackson.core.JsonParseException
import org.apache.pekko.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.http.Status._
import play.api.libs.json.{JsResultException, JsValue, Json, Reads}
import play.api.mvc.{ResponseHeader, Result}
import uk.gov.hmrc.http._

import scala.util.matching.Regex

trait HttpResponseHelper extends HttpErrorFunctions {

  private val logger = Logger(classOf[HttpResponseHelper])

  def result(res: HttpResponse): Result = {

    val responseBodyRegex: Regex = """^.*Response body:? '(.*)'$""".r

    val httpEntity: HttpEntity.Strict = res.body match {
      case responseBodyRegex(body) =>
        HttpEntity.Strict(ByteString(body), Some("application/json"))
      case message: String =>
        HttpEntity.Strict(ByteString(message), Some("text/plain"))
      case null =>
        HttpEntity.NoEntity
    }

    Result(ResponseHeader(res.status), httpEntity)
  }

  def handleErrorResponse(httpMethod: String, url: String, args: String*)(response: HttpResponse): HttpException =
    response.status match {
      case BAD_REQUEST =>
        if (response.body.contains("INVALID_PAYLOAD")) {
          logger.warn(s"INVALID_PAYLOAD returned for: ${args.headOption.getOrElse(url)} from: $url")
        }
        new BadRequestException(badRequestMessage(httpMethod, url, response.body))
      case FORBIDDEN =>
        new ForbiddenException(upstreamResponseMessage("POST", url, FORBIDDEN, response.body))
      case NOT_FOUND =>
        new NotFoundException(notFoundMessage(httpMethod, url, response.body))
      case status if is4xx(status) =>
        throw UpstreamErrorResponse(upstreamResponseMessage(httpMethod, url, status, response.body), status, status, response.headers)
      case status if is5xx(status) =>
        throw UpstreamErrorResponse(upstreamResponseMessage(httpMethod, url, status, response.body), status, BAD_GATEWAY)
      case _ =>
        throw new UnrecognisedHttpResponseException(httpMethod, url, response)
    }

  def parseJson(json: String, method: String, url: String): JsValue = {
    try {
      Json.parse(json)
    }
    catch {
      case _: JsonParseException =>
        throw new BadGatewayException(s"$method to $url returned a response that was not JSON")
    }
  }

  def validateJson[T](json: JsValue, method: String, url: String, onInvalid: JsValue => Unit)
                     (implicit reads: Reads[T]): T = {

    json.validate[T].fold(
      invalid => {
        onInvalid(json)
        throw new BadGatewayException(
          s"$method to $url returned invalid JSON ${JsResultException(invalid).getMessage}"
        )
      },
      identity
    )
  }

  def parseAndValidateJson[T](json: String, method: String, url: String, onInvalid: JsValue => Unit)
                             (implicit reads: Reads[T]): T = {
    validateJson(parseJson(json, method, url), method, url, onInvalid)
  }
}

class UnrecognisedHttpResponseException(method: String, url: String, response: HttpResponse)
  extends Exception(s"$method to $url failed with status ${response.status}. Response body: '${response.body}'")
