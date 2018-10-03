/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers


import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.CustomsExportsBaseSpec

class NotificationsControllerSpec extends CustomsExportsBaseSpec {

  val uri = "/customs-declare-exports/notify"

  val getNotificationUri = "/customs-declare-exports/notifications/lrn1/mrn1/eori1"
  val validXML = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
    <wstxns1:Response xmlns:wstxns1="urn:wco:datamodel:WCO:RES-DMS:2"></wstxns1:Response></MetaData>


  "NotificationsControllerSpec" should {

    "save Notification" in {
      withAuthorizedUser()
      val result = route(app, FakeRequest(POST,uri).withXmlBody(validXML)).get
      status(result) must be (OK)

    }

    "get Notifications" in {
      withAuthorizedUser()
      val result = route(app, FakeRequest(GET,getNotificationUri)).get
      status(result) must be (OK)

    }
  }
}
