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

import connectors.HeaderUtilsSpec.mock
import connectors.SchemeConnector
import controllers.actions.{PspAuthRequest, PspSchemeAuthAction}
import models.SchemeReferenceNumber
import play.api.mvc.{ActionFunction, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class FakePspSchemeAuthAction extends PspSchemeAuthAction(mock[SchemeConnector]) {
  override def apply(srn: SchemeReferenceNumber): ActionFunction[PspAuthRequest, PspAuthRequest] = {
    new ActionFunction[PspAuthRequest, PspAuthRequest] {
      val executionContext: ExecutionContext = global
      override def invokeBlock[T](request: PspAuthRequest[T], block: PspAuthRequest[T] => Future[Result]): Future[Result] = {
        block(request)
      }
    }
  }
}
