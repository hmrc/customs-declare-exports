/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.base

import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatcher, ArgumentMatchers, MockitoSugar}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.exports.models.{Eori, SignedInUser}

import scala.concurrent.Future

trait AuthTestSupport extends MockitoSugar {

  lazy val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val enrolment: Predicate = Enrolment("HMRC-CUS-ORG")
  val userEori = Eori("12345")

  def cdsEnrollmentMatcher(user: SignedInUser): ArgumentMatcher[Predicate] = new ArgumentMatcher[Predicate] {
    override def matches(p: Predicate): Boolean =
      p == enrolment && user.enrolments.getEnrolment("HMRC-CUS-ORG").isDefined
  }

  def withAuthorizedUser(user: SignedInUser = newUser(userEori.value, "external1")): Unit =
    when(mockAuthConnector.authorise(ArgumentMatchers.argThat(cdsEnrollmentMatcher(user)), ArgumentMatchers.eq(allEnrolments))(any(), any()))
      .thenReturn(Future.successful(user.enrolments))

  def withUnauthorizedUser(error: Throwable): Unit =
    when(mockAuthConnector.authorise(any(), any())(any(), any())).thenReturn(Future.failed(error))

  def userWithoutEori(user: SignedInUser = newUser("", externalId = "external1")): Unit =
    when(mockAuthConnector.authorise(ArgumentMatchers.argThat(cdsEnrollmentMatcher(user)), ArgumentMatchers.eq(allEnrolments))(any(), any()))
      .thenReturn(Future.successful(Enrolments(Set())))

  def newUser(eori: String, externalId: String): SignedInUser = SignedInUser(
    Credentials("2345235235", "GovernmentGateway"),
    Name(Some("Aldo"), Some("Rain")),
    Some("amina@hmrc.co.uk"),
    eori,
    externalId,
    Some("Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"),
    Some(AffinityGroup.Individual),
    Enrolments(
      Set(
        Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "111111111")), "Activated", None),
        Enrolment("IR-CT", List(EnrolmentIdentifier("UTR", "222222222")), "Activated", None),
        Enrolment("HMRC-CUS-ORG", List(EnrolmentIdentifier("EORINumber", eori)), "Activated", None)
      )
    )
  )
}
