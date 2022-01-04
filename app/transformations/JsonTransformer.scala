/*
 * Copyright 2022 HM Revenue & Customs
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

package transformations

import play.api.libs.json.{JsObject, JsPath, JsString, Json, Reads, __}

trait JsonTransformer {

  val doNothing: Reads[JsObject] = __.json.put(Json.obj())

  protected def transformBooleanToYesNo(path: JsPath): Reads[JsString] = path.read[Boolean].map {
    case true => JsString("Yes")
    case false => JsString("No")
  }

}
