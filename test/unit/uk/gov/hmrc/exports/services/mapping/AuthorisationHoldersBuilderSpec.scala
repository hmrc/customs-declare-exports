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

package uk.gov.hmrc.exports.services.mapping

import org.scalatest.Assertion
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.STANDARD_FRONTIER
import uk.gov.hmrc.exports.models.declaration.{DeclarationHolder, EntityDetails, EoriSource}
import uk.gov.hmrc.exports.services.mapping.AuthorisationHoldersBuilder.{authCodesForGVMSPorts, EXRR}
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.GoodsLocationBuilderSpec.{gvmGoodsLocation, validGoodsLocation}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class AuthorisationHoldersBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val authorisationHoldersBuilder = new AuthorisationHoldersBuilder()

  "AuthorisationHolders" should {

    "build the expected WCO declaration" when {

      "the ExportsDeclaration instance contains no holders" in {
        val model = aDeclaration(withoutDeclarationHolders())
        val declaration = new Declaration()

        authorisationHoldersBuilder.buildThenAdd(model, declaration)

        declaration.getAuthorisationHolder mustBe empty
      }

      "the ExportsDeclaration instance contains multiple holders" in {
        val model = aDeclaration(
          withDeclarationHolders(
            DeclarationHolder(Some("auth code1"), Some("eori1"), Some(EoriSource.OtherEori)),
            DeclarationHolder(Some("auth code2"), Some("eori2"), Some(EoriSource.OtherEori))
          )
        )
        val declaration = new Declaration()

        authorisationHoldersBuilder.buildThenAdd(model, declaration)

        declaration.getAuthorisationHolder must have(size(2))
        declaration.getAuthorisationHolder.get(0).getID.getValue mustBe "eori1"
        declaration.getAuthorisationHolder.get(1).getID.getValue mustBe "eori2"
        declaration.getAuthorisationHolder.get(0).getCategoryCode.getValue mustBe "auth code1"
        declaration.getAuthorisationHolder.get(1).getCategoryCode.getValue mustBe "auth code2"
      }

      "the ExportsDeclaration instance contains an empty auth code" in {
        val model = aDeclaration(withDeclarationHolders(DeclarationHolder(None, Some("eori"), Some(EoriSource.OtherEori))))
        val declaration = new Declaration()

        authorisationHoldersBuilder.buildThenAdd(model, declaration)

        declaration.getAuthorisationHolder mustBe empty
      }

      "the ExportsDeclaration instance contains an empty eori" in {
        val model = aDeclaration(withDeclarationHolders(DeclarationHolder(Some("auth code"), None, Some(EoriSource.OtherEori))))
        val declaration = new Declaration()

        authorisationHoldersBuilder.buildThenAdd(model, declaration)

        declaration.getAuthorisationHolder mustBe empty
      }
    }
  }

  "AuthorisationHolders" when {
    "the ExportsDeclaration instance is of 'Arrived' type and" when {
      val additionalType = withAdditionalDeclarationType(STANDARD_FRONTIER)

      val declarantEori = "GB123456"

      "the user didn't enter any authorisation code and" when {
        "the user selects a GVMS port on /location-of-goods" should {
          "add to the WCO Declaration an 'EXRR' 'AuthorisationHolder' property" in {
            val model =
              aDeclaration(additionalType, withGoodsLocation(gvmGoodsLocation), withDeclarantIsExporter(), withDeclarantDetails(Some(declarantEori)))
            val declaration = new Declaration()

            authorisationHoldersBuilder.buildThenAdd(model, declaration)

            val authHolders = declaration.getAuthorisationHolder
            authHolders.size mustBe 1

            authHolders.get(0).getID.getValue mustBe declarantEori
            authHolders.get(0).getCategoryCode.getValue mustBe EXRR
          }
        }
      }

      val authCode0 = "APEX"
      val eori0 = "GB717572504502811"

      "the user enters an authorisation code on /add-authorisation-required but different from 'EXRR', 'MIB' or 'CSE' and" when {
        val holders = withDeclarationHolders(DeclarationHolder(Some(authCode0), Some(eori0), None))

        "the user selects a GVMS port on /location-of-goods" should {
          val locationOfGoods = withGoodsLocation(gvmGoodsLocation)

          "add to the WCO Declaration an 'AuthorisationHolder' property with 'EXRR' as category code and" should {

            "set the declarant's EORI as ID" when {

              "the declarant is the exporter and the declarant's EORI starts with 'GB'" in {
                val modifiers = List(holders, locationOfGoods, withDeclarantIsExporter(), withDeclarantDetails(Some(declarantEori)))
                verifyAuthHolder(modifiers, declarantEori)
              }

              "the declarant is agent and the declarant holds the contract" in {
                val modifiers = List(
                  holders,
                  locationOfGoods,
                  withDeclarantIsExporter("No"),
                  withDeclarantDetails(Some(declarantEori)),
                  withRepresentativeDetails(None, None)
                )
                verifyAuthHolder(modifiers, declarantEori)
              }
            }

            "set the exporter's EORI as ID" when {
              "the declarant is agent and the exporter's EORI starts with 'GB'" in {
                val exporterEori = "GB654321"
                val modifiers = List(
                  holders,
                  locationOfGoods,
                  withDeclarantIsExporter("No"),
                  withDeclarantDetails(Some(declarantEori)),
                  withExporterDetails(Some(exporterEori))
                )
                verifyAuthHolder(modifiers, exporterEori)
              }
            }

            "set the representative's EORI as ID" when {

              "the declarant is the exporter but the declarant's EORI does not start with 'GB'" in {
                val declarantEori = "US123456"
                val representativeEori = "GB123456"
                val modifiers = List(
                  holders,
                  locationOfGoods,
                  withDeclarantIsExporter(),
                  withDeclarantDetails(Some(declarantEori)),
                  withRepresentativeDetails(Some(EntityDetails(Some(representativeEori), None)), None)
                )
                verifyAuthHolder(modifiers, representativeEori)
              }

              "the declarant is agent but the exporter's EORI does not start with 'GB' and declarant does not hold the contract" in {
                val exporterEori = "US123456"
                val representativeEori = "GB654321"
                val modifiers = List(
                  holders,
                  locationOfGoods,
                  withDeclarantIsExporter("No"),
                  withDeclarantDetails(Some(declarantEori)),
                  withExporterDetails(Some(exporterEori)),
                  withRepresentativeDetails(Some(EntityDetails(Some(representativeEori), None)), None, Some("No"))
                )
                verifyAuthHolder(modifiers, representativeEori)
              }
            }
          }
        }

        "the user selects a non-GVMS port on /location-of-goods" should {
          "not add to the WCO Declaration an 'AuthorisationHolder' property with 'EXRR' as category code" in {
            val modifiers = List(holders, withGoodsLocation(validGoodsLocation), withDeclarantIsExporter(), withDeclarantDetails(Some(declarantEori)))
            verifyAuthHolder(modifiers, declarantEori, expectedSize = 1)
          }
        }
      }

      "the user selects a GVMS port on /location-of-goods and" when {
        val eori1 = "GB717572504502812"
        val locationOfGoods = withGoodsLocation(gvmGoodsLocation)

        authCodesForGVMSPorts.foreach { authCode =>
          s"the user enter '$authCode' on /add-authorisation-required and" should {
            val holders =
              withDeclarationHolders(DeclarationHolder(Some(authCode0), Some(eori0), None), DeclarationHolder(Some(authCode), Some(eori1), None))

            "add to the WCO Declaration a single 'AuthorisationHolder' property with 'EXRR' as category code" in {
              val modifiers = List(holders, locationOfGoods, withDeclarantIsExporter(), withDeclarantDetails(Some(declarantEori)))
              verifyAuthHolder(modifiers, eori1, authCode)
            }
          }
        }
      }

      def verifyAuthHolder(
        modifiers: List[ExportsDeclarationModifier],
        expectedEori: String,
        expectedAuthCode: String = EXRR,
        expectedSize: Int = 2
      ): Assertion = {
        val model = aDeclaration((List(additionalType) ++ modifiers): _*)
        val declaration = new Declaration()

        authorisationHoldersBuilder.buildThenAdd(model, declaration)

        val authHolders = declaration.getAuthorisationHolder
        authHolders.size mustBe expectedSize

        if (expectedSize == 2) {
          authHolders.get(1).getID.getValue mustBe expectedEori
          authHolders.get(1).getCategoryCode.getValue mustBe expectedAuthCode
        }

        authHolders.get(0).getID.getValue mustBe eori0
        authHolders.get(0).getCategoryCode.getValue mustBe authCode0
      }
    }
  }
}
