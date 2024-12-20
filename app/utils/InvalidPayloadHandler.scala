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

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersion, ValidationMessage}
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.jdk.CollectionConverters._


class InvalidPayloadHandler {

  private val logger = LoggerFactory.getLogger("InvalidPayloadHandler")

  private[utils] def loadSchema(schemaFileName: String): JsonSchema = {
    val schemaUrl = getClass.getResourceAsStream(schemaFileName)
    val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
    factory.getSchema(schemaUrl)
  }

  def getFailures(schemaFileName: String)(json: JsValue): Set[ValidationFailure] = {

    val schema = loadSchema(schemaFileName)
    getFailures(schema, json)

  }

  private[utils] def getFailures(schema: JsonSchema, json: JsValue): Set[ValidationFailure] = {

    val mapper = new ObjectMapper()
    val jsonNode = mapper.readTree(json.toString())

    val set = schema.validate(jsonNode).asScala.toSet

    set.map {
      message =>
        val value = InvalidPayloadHandlerImpl.valueFromJson(message, json)
        ValidationFailure(message.getType, message.getMessage, value)
    }

  }

  def logFailures(schemaFileName: String, args: String*)(json: JsValue): Unit = {

    val schema = loadSchema(schemaFileName)
    logFailures(schema, json, args)

  }

  private[utils] def logFailures(schema: JsonSchema, json: JsValue, args: Seq[String]): Unit = {

    val failures = getFailures(schema, json)
    val msg = new StringBuilder()

    msg.append(s"Invalid Payload JSON Failures${if (args.nonEmpty) s" for url: ${args.head}"}\n")
    msg.append(s"Failures: ${failures.size}\n")
    msg.append("\n")

    failures.foreach {
      failure =>
        msg.append(s"${failure.message}\n")
        msg.append(s"Type: ${failure.failureType}\n")
        msg.append(s"Value: ${failure.value.getOrElse("[none]")}\n")
        msg.append("\n")
    }

    logger.warn(msg.toString())

  }
}

object InvalidPayloadHandlerImpl {
  private[utils] def valueFromJson(message: ValidationMessage, json: JsValue): Option[String] = {
    message.getType match {
      case "enum" | "format" | "maximum" | "maxLength" | "minimum" | "minLength" | "pattern" | "type" =>
        (json \ message.getMessageKey).toEither match {
          case Right(jsValue) =>
            jsValue match {
              case JsBoolean(bool) => Some(bool.toString)
              case JsNull => Some("null")
              case JsNumber(n) => Some(depersonalise(n.toString))
              case JsString(s) => Some(depersonalise(s))
              case _ => None
            }
          case Left(_) => None
        }
      case _ => None
    }
  }


  private def depersonalise(value: String): String = {
    value
      .replaceAll("[a-zA-Z]", "x")
      .replaceAll("[0-9]", "9")
  }
}

case class ValidationFailure(failureType: String, message: String, value: Option[String])
