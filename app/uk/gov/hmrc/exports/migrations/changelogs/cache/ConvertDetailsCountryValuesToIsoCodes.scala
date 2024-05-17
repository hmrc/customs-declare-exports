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

package uk.gov.hmrc.exports.migrations.changelogs.cache

import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, equal, exists, or}
import play.api.Logging
import play.api.libs.json.{JsArray, JsString, Json}
import uk.gov.hmrc.exports.migrations.changelogs.cache.ConvertDetailsCountryValuesToIsoCodes.{allCountries, getCountryCode}
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.Country

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.IterableHasAsScala

class ConvertDetailsCountryValuesToIsoCodes extends MigrationDefinition with Logging {

  private val batchSize = 100
  private val commonFieldName = "details.address.country"
  private val detailTypes =
    Seq("exporterDetails", "consigneeDetails", "declarantDetails", "representativeDetails", "carrierDetails", "consignorDetails")

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-5774 Convert Details Country values from names to ISO codes", order = 27, author = "Tom Robinson")

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val declarationCollection = db.getCollection("declarations")
    val documentFilter = or(detailTypes.map(details => exists(s"parties.$details.$commonFieldName")): _*)

    declarationCollection
      .find(documentFilter)
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val decId = document.get("id").toString
        val eori = document.get("eori").toString
        val filter = and(equal("eori", eori), equal("id", decId))

        val updatedDeclaration = convertValueToIsoCode(document)
        declarationCollection.replaceOne(filter, updatedDeclaration)
      }

    logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
  }

  private type FieldPath = List[String]

  private def countryFieldsAndValues(dec: Document): Seq[(FieldPath, Option[String])] = {
    @tailrec
    def getFieldValue(doc: Document, path: FieldPath): Option[String] = path match {
      case head :: Nil                                     => Some(doc.get(head, classOf[String]))
      case head :: tail if Option(doc.get(head)).isDefined => getFieldValue(doc.get(head, classOf[Document]), tail)
      case _                                               => None
    }

    val commonPath = commonFieldName.split("\\.").toList

    detailTypes.map { detailType =>
      val fullPath = "parties" :: detailType :: commonPath
      (fullPath, getFieldValue(dec, fullPath))
    }
  }

  private def convertValueToIsoCode(declaration: Document): Document = {
    def updateDecWithIsoCode(dec: Document, field: FieldPath, value: String): Option[Document] = {
      def updateDocument(doc: Document, path: FieldPath): Option[Document] = path match {
        case head :: Nil =>
          doc.put(head, value)
          Some(doc)
        case head :: tail if Option(doc.get(head)).isDefined =>
          val maybeUpdatedChildDoc = updateDocument(doc.get(head, classOf[Document]), tail)
          maybeUpdatedChildDoc.map { updatedChildDoc =>
            doc.put(head, updatedChildDoc)
            doc
          }
        case _ => None
      }

      updateDocument(dec, field)
    }

    countryFieldsAndValues(declaration).foldLeft(declaration) { (currentDeclaration, fieldAndValue) =>
      def logWithDeclarationId(message: String): Unit =
        logger.warn(s"Dec Id [${currentDeclaration.getString("id")}] $message")

      val (field, maybeValue) = fieldAndValue

      maybeValue.fold(currentDeclaration) { value =>
        getCountryCode(value) match {
          case Some(code) =>
            updateDecWithIsoCode(currentDeclaration, field, code).fold {
              logWithDeclarationId(s"Unable to migrate field [$field] from [$value] to [$code]")
              currentDeclaration
            }(identity)
          case None if allCountries.map(_.countryCode).contains(value) => currentDeclaration // Value is already an ISO code
          case _ =>
            logWithDeclarationId(s"Unable to find country code for [$value]")
            currentDeclaration
        }
      }
    }
  }
}

object ConvertDetailsCountryValuesToIsoCodes {
  private val countryMappings =
    """
      |[
      |    [
      |        "Andorra",
      |        "AD"
      |    ],
      |    [
      |        "United Arab Emirates (the) - Abu Dhabi, Ajman, Dubai, Fujairah, Ras al Khaimah, Sharjah and Umm al Qaiwain",
      |        "AE"
      |    ],
      |    [
      |        "Afghanistan",
      |        "AF"
      |    ],
      |    [
      |        "Antigua and Barbuda",
      |        "AG"
      |    ],
      |    [
      |        "Anguilla",
      |        "AI"
      |    ],
      |    [
      |        "Albania",
      |        "AL"
      |    ],
      |    [
      |        "Armenia",
      |        "AM"
      |    ],
      |    [
      |        "Angola including Cabinda",
      |        "AO"
      |    ],
      |    [
      |        "Antarctica - Territory south of 60 degree south latitude; not including the French Southern Territories (TF), Bouvet Island (BV), South Georgia and South Sandwich Islands (GS)",
      |        "AQ"
      |    ],
      |    [
      |        "Argentina",
      |        "AR"
      |    ],
      |    [
      |        "American Samoa",
      |        "AS"
      |    ],
      |    [
      |        "Austria",
      |        "AT"
      |    ],
      |    [
      |        "Australia",
      |        "AU"
      |    ],
      |    [
      |        "Aruba",
      |        "AW"
      |    ],
      |    [
      |        "Azerbaijan",
      |        "AZ"
      |    ],
      |    [
      |        "Bosnia and Herzegovina",
      |        "BA"
      |    ],
      |    [
      |        "Barbados",
      |        "BB"
      |    ],
      |    [
      |        "Bangladesh",
      |        "BD"
      |    ],
      |    [
      |        "Belgium",
      |        "BE"
      |    ],
      |    [
      |        "Burkina Faso",
      |        "BF"
      |    ],
      |    [
      |        "Bulgaria",
      |        "BG"
      |    ],
      |    [
      |        "Bahrain",
      |        "BH"
      |    ],
      |    [
      |        "Burundi",
      |        "BI"
      |    ],
      |    [
      |        "Benin",
      |        "BJ"
      |    ],
      |    [
      |        "Saint Barthelemy",
      |        "BL"
      |    ],
      |    [
      |        "Bermuda",
      |        "BM"
      |    ],
      |    [
      |        "Brunei Darussalam, Often referred to as Brunei",
      |        "BN"
      |    ],
      |    [
      |        "Bolivia (Plurinational State of), Often referred to as Bolivia",
      |        "BO"
      |    ],
      |    [
      |        "Bonaire, Sint Eustatius and Saba",
      |        "BQ"
      |    ],
      |    [
      |        "Brazil",
      |        "BR"
      |    ],
      |    [
      |        "Bahamas (the)",
      |        "BS"
      |    ],
      |    [
      |        "Bhutan",
      |        "BT"
      |    ],
      |    [
      |        "Bouvet Island",
      |        "BV"
      |    ],
      |    [
      |        "Botswana",
      |        "BW"
      |    ],
      |    [
      |        "Belarus, Often referred to as Belorussia",
      |        "BY"
      |    ],
      |    [
      |        "Belize",
      |        "BZ"
      |    ],
      |    [
      |        "Canada",
      |        "CA"
      |    ],
      |    [
      |        "Cocos (Keeling) Islands (the)",
      |        "CC"
      |    ],
      |    [
      |        "Congo (the Democratic Republic of the), Formerly Zaire",
      |        "CD"
      |    ],
      |    [
      |        "Central African Republic (the)",
      |        "CF"
      |    ],
      |    [
      |        "Congo (the)",
      |        "CG"
      |    ],
      |    [
      |        "Switzerland, Including the German territory of Busingen and the Italian municipality of Campione d’Italia",
      |        "CH"
      |    ],
      |    [
      |        "Cote d’Ivoire, Often referred to as Ivory Coast",
      |        "CI"
      |    ],
      |    [
      |        "Cook Islands (the)",
      |        "CK"
      |    ],
      |    [
      |        "Chile",
      |        "CL"
      |    ],
      |    [
      |        "Cameroon",
      |        "CM"
      |    ],
      |    [
      |        "China",
      |        "CN"
      |    ],
      |    [
      |        "Colombia",
      |        "CO"
      |    ],
      |    [
      |        "Costa Rica",
      |        "CR"
      |    ],
      |    [
      |        "Cuba",
      |        "CU"
      |    ],
      |    [
      |        "Cape Verde",
      |        "CV"
      |    ],
      |    [
      |        "Curacao",
      |        "CW"
      |    ],
      |    [
      |        "Christmas Island",
      |        "CX"
      |    ],
      |    [
      |        "Cyprus",
      |        "CY"
      |    ],
      |    [
      |        "Czech Republic",
      |        "CZ"
      |    ],
      |    [
      |        "Germany, Including the island of Heligoland; excluding the territory of Busingen",
      |        "DE"
      |    ],
      |    [
      |        "Djibouti",
      |        "DJ"
      |    ],
      |    [
      |        "Denmark",
      |        "DK"
      |    ],
      |    [
      |        "Dominica",
      |        "DM"
      |    ],
      |    [
      |        "Dominican Republic (the)",
      |        "DO"
      |    ],
      |    [
      |        "Algeria",
      |        "DZ"
      |    ],
      |    [
      |        "Ecuador, Including the Galapagos Islands",
      |        "EC"
      |    ],
      |    [
      |        "Estonia",
      |        "EE"
      |    ],
      |    [
      |        "Egypt",
      |        "EG"
      |    ],
      |    [
      |        "Western Sahara",
      |        "EH"
      |    ],
      |    [
      |        "Eritrea",
      |        "ER"
      |    ],
      |    [
      |        "Spain, Including the Balearic Islands and the Canary Islands; excluding Ceuta (XC) and Melilla (XL)",
      |        "ES"
      |    ],
      |    [
      |        "Ethiopia",
      |        "ET"
      |    ],
      |    [
      |        "European Union",
      |        "EU"
      |    ],
      |    [
      |        "Finland, Including the Aland Islands",
      |        "FI"
      |    ],
      |    [
      |        "Fiji",
      |        "FJ"
      |    ],
      |    [
      |        "Falkland Islands (the)",
      |        "FK"
      |    ],
      |    [
      |        "Micronesia (Federated States of), Chuuk, Kosrae, Pohnpei and Yap",
      |        "FM"
      |    ],
      |    [
      |        "Faroe Islands (the)",
      |        "FO"
      |    ],
      |    [
      |        "France, Including Monaco, the French overseas departments (French Guiana, Guadeloupe, Martinique and Reunion) and the French northern part of St Martin",
      |        "FR"
      |    ],
      |    [
      |        "Gabon",
      |        "GA"
      |    ],
      |    [
      |        "United Kingdom, Great Britain, Northern Ireland",
      |        "GB"
      |    ],
      |    [
      |        "Grenada, Including Southern Grenadines",
      |        "GD"
      |    ],
      |    [
      |        "Georgia",
      |        "GE"
      |    ],
      |    [
      |        "Guernsey",
      |        "GG"
      |    ],
      |    [
      |        "Ghana",
      |        "GH"
      |    ],
      |    [
      |        "Gibraltar",
      |        "GI"
      |    ],
      |    [
      |        "Greenland",
      |        "GL"
      |    ],
      |    [
      |        "Gambia (the)",
      |        "GM"
      |    ],
      |    [
      |        "Guinea",
      |        "GN"
      |    ],
      |    [
      |        "Desirade",
      |        "GP"
      |    ],
      |    [
      |        "Equatorial Guinea",
      |        "GQ"
      |    ],
      |    [
      |        "Greece",
      |        "GR"
      |    ],
      |    [
      |        "South Georgia and the South Sandwich Islands",
      |        "GS"
      |    ],
      |    [
      |        "Guatemala",
      |        "GT"
      |    ],
      |    [
      |        "Guam",
      |        "GU"
      |    ],
      |    [
      |        "Guinea-Bissau",
      |        "GW"
      |    ],
      |    [
      |        "Guyana",
      |        "GY"
      |    ],
      |    [
      |        "Hong Kong, Hong Kong Special Administrative Region of the People’s Republic of China",
      |        "HK"
      |    ],
      |    [
      |        "Heard Island and McDonald Islands",
      |        "HM"
      |    ],
      |    [
      |        "Honduras, Including Swan Islands",
      |        "HN"
      |    ],
      |    [
      |        "Croatia",
      |        "HR"
      |    ],
      |    [
      |        "Haiti",
      |        "HT"
      |    ],
      |    [
      |        "Hungary",
      |        "HU"
      |    ],
      |    [
      |        "Indonesia",
      |        "ID"
      |    ],
      |    [
      |        "Ireland",
      |        "IE"
      |    ],
      |    [
      |        "Israel",
      |        "IL"
      |    ],
      |    [
      |        "Isle of Man",
      |        "IM"
      |    ],
      |    [
      |        "India",
      |        "IN"
      |    ],
      |    [
      |        "British Indian Ocean Territory (the), Chagos Archipelago",
      |        "IO"
      |    ],
      |    [
      |        "Iraq",
      |        "IQ"
      |    ],
      |    [
      |        "Iran (Islamic Republic of)",
      |        "IR"
      |    ],
      |    [
      |        "Iceland",
      |        "IS"
      |    ],
      |    [
      |        "Italy, Including Livigno; excluding the municipality of Campione d’Italia",
      |        "IT"
      |    ],
      |    [
      |        "Jersey",
      |        "JE"
      |    ],
      |    [
      |        "Jamaica",
      |        "JM"
      |    ],
      |    [
      |        "Jordan",
      |        "JO"
      |    ],
      |    [
      |        "Japan",
      |        "JP"
      |    ],
      |    [
      |        "Kenya",
      |        "KE"
      |    ],
      |    [
      |        "Kyrgyz Republic",
      |        "KG"
      |    ],
      |    [
      |        "Cambodia",
      |        "KH"
      |    ],
      |    [
      |        "Kiribati",
      |        "KI"
      |    ],
      |    [
      |        "Comoros (the), Anjouan, Grande Comore and Moheli",
      |        "KM"
      |    ],
      |    [
      |        "Saint Kitts and Nevis",
      |        "KN"
      |    ],
      |    [
      |        "Korea (the Democratic People’s Republic of), Often referred to as North Korea",
      |        "KP"
      |    ],
      |    [
      |        "Korea (the Republic of), Often referred to as South Korea",
      |        "KR"
      |    ],
      |    [
      |        "Kuwait",
      |        "KW"
      |    ],
      |    [
      |        "Cayman Islands (the)",
      |        "KY"
      |    ],
      |    [
      |        "Kazakhstan",
      |        "KZ"
      |    ],
      |    [
      |        "Lao People’s Democratic Republic (the), Often referred to as Laos",
      |        "LA"
      |    ],
      |    [
      |        "Lebanon",
      |        "LB"
      |    ],
      |    [
      |        "Saint Lucia",
      |        "LC"
      |    ],
      |    [
      |        "Liechtenstein",
      |        "LI"
      |    ],
      |    [
      |        "Sri Lanka",
      |        "LK"
      |    ],
      |    [
      |        "Liberia",
      |        "LR"
      |    ],
      |    [
      |        "Lesotho",
      |        "LS"
      |    ],
      |    [
      |        "Lithuania",
      |        "LT"
      |    ],
      |    [
      |        "Luxembourg",
      |        "LU"
      |    ],
      |    [
      |        "Latvia",
      |        "LV"
      |    ],
      |    [
      |        "Libya",
      |        "LY"
      |    ],
      |    [
      |        "Morocco",
      |        "MA"
      |    ],
      |    [
      |        "Moldova (the Republic of)",
      |        "MD"
      |    ],
      |    [
      |        "Montenegro",
      |        "ME"
      |    ],
      |    [
      |        "Madagascar",
      |        "MG"
      |    ],
      |    [
      |        "Marshall Islands (the)",
      |        "MH"
      |    ],
      |    [
      |        "Macedonia (the former Yugoslav Republic of)",
      |        "MK"
      |    ],
      |    [
      |        "Mali",
      |        "ML"
      |    ],
      |    [
      |        "Myanmar, Often referred to as Burma",
      |        "MM"
      |    ],
      |    [
      |        "Mongolia",
      |        "MN"
      |    ],
      |    [
      |        "Macao, Special Administrative Region of the People’s Republic of China",
      |        "MO"
      |    ],
      |    [
      |        "Northern Mariana Islands (the)",
      |        "MP"
      |    ],
      |    [
      |        "Mauritania",
      |        "MR"
      |    ],
      |    [
      |        "Montserrat",
      |        "MS"
      |    ],
      |    [
      |        "Malta, Including Gozo and Comino",
      |        "MT"
      |    ],
      |    [
      |        "Mauritius - Mauritius, Rodrigues Island, Agalega Islands and Cargados Carajos Shoals (St Brandon Islands)",
      |        "MU"
      |    ],
      |    [
      |        "Maldives",
      |        "MV"
      |    ],
      |    [
      |        "Malawi",
      |        "MW"
      |    ],
      |    [
      |        "Mexico",
      |        "MX"
      |    ],
      |    [
      |        "Malaysia - Peninsular Malaysia and Eastern Malaysia (Labuan, Sabah and Sarawak)",
      |        "MY"
      |    ],
      |    [
      |        "Mozambique",
      |        "MZ"
      |    ],
      |    [
      |        "Namibia",
      |        "NA"
      |    ],
      |    [
      |        "New Caledonia, Including Loyalty Islands (Lifou, Mare and Ouvea)",
      |        "NC"
      |    ],
      |    [
      |        "Niger (the)",
      |        "NE"
      |    ],
      |    [
      |        "Norfolk Island",
      |        "NF"
      |    ],
      |    [
      |        "Nigeria",
      |        "NG"
      |    ],
      |    [
      |        "Nicaragua, Including Corn Islands",
      |        "NI"
      |    ],
      |    [
      |        "Netherlands (the)",
      |        "NL"
      |    ],
      |    [
      |        "Norway, Including Svalbard Archipelago and Jan Mayen Island",
      |        "NO"
      |    ],
      |    [
      |        "Norway",
      |        "NO"
      |    ],
      |    [
      |        "Nepal",
      |        "NP"
      |    ],
      |    [
      |        "Nauru",
      |        "NR"
      |    ],
      |    [
      |        "Niue",
      |        "NU"
      |    ],
      |    [
      |        "New Zealand, Excluding Ross Dependency (Antarctica)",
      |        "NZ"
      |    ],
      |    [
      |        "Oman",
      |        "OM"
      |    ],
      |    [
      |        "Panama, Including former Canal Zone",
      |        "PA"
      |    ],
      |    [
      |        "Peru",
      |        "PE"
      |    ],
      |    [
      |        "French Polynesia - Marquesas Islands, Society Islands (including Tahiti), Tuamotu Islands, Gambier Islands and Austral Islands",
      |        "PF"
      |    ],
      |    [
      |        "Papua New Guinea - Eastern part of New Guinea; Bismarck Archipelago (including New Britain, New Ireland, Lavongai (New Hanover) and Admiralty Islands); Northern Solomon Islands (Bougainville and Buka); Trobriand Islands, Woodlark Island; d’Entrecasteaux Islands and Louisiade Archipelago",
      |        "PG"
      |    ],
      |    [
      |        "Philippines (the)",
      |        "PH"
      |    ],
      |    [
      |        "Pakistan",
      |        "PK"
      |    ],
      |    [
      |        "Poland",
      |        "PL"
      |    ],
      |    [
      |        "Saint Pierre and Miquelon",
      |        "PM"
      |    ],
      |    [
      |        "Pitcairn, Including the Ducie, Henderson and Oeno Islands",
      |        "PN"
      |    ],
      |    [
      |        "Occupied Palestinian Territory - West Bank (including East Jerusalem) and Gaza Strip",
      |        "PS"
      |    ],
      |    [
      |        "Portugal, Including Azores and Madeira",
      |        "PT"
      |    ],
      |    [
      |        "Palau",
      |        "PW"
      |    ],
      |    [
      |        "Paraguay",
      |        "PY"
      |    ],
      |    [
      |        "Qatar",
      |        "QA"
      |    ],
      |    [
      |        "Romania",
      |        "RO"
      |    ],
      |    [
      |        "Russian Federation (the), Often referred to as Russia",
      |        "RU"
      |    ],
      |    [
      |        "Rwanda",
      |        "RW"
      |    ],
      |    [
      |        "Saudi Arabia",
      |        "SA"
      |    ],
      |    [
      |        "Solomon Islands",
      |        "SB"
      |    ],
      |    [
      |        "Seychelles - Mahe Island, Praslin Island, La Digue, Fregate and Silhouette; Amirante Islands (including Desroches, Alphonse, Platte and Coetivy); Farquhar Islands (including Providence); Aldabra Islands and Cosmoledo Islands",
      |        "SC"
      |    ],
      |    [
      |        "Sudan (the)",
      |        "SD"
      |    ],
      |    [
      |        "Sweden",
      |        "SE"
      |    ],
      |    [
      |        "Singapore",
      |        "SG"
      |    ],
      |    [
      |        "Saint Helena, Ascension and Tristan da Cunha",
      |        "SH"
      |    ],
      |    [
      |        "Slovenia",
      |        "SI"
      |    ],
      |    [
      |        "Slovakia",
      |        "SK"
      |    ],
      |    [
      |        "Sierra Leone",
      |        "SL"
      |    ],
      |    [
      |        "San Marino",
      |        "SM"
      |    ],
      |    [
      |        "Senegal",
      |        "SN"
      |    ],
      |    [
      |        "Somalia",
      |        "SO"
      |    ],
      |    [
      |        "Suriname",
      |        "SR"
      |    ],
      |    [
      |        "South Sudan",
      |        "SS"
      |    ],
      |    [
      |        "Sao Tome and Principe",
      |        "ST"
      |    ],
      |    [
      |        "El Salvador",
      |        "SV"
      |    ],
      |    [
      |        "Sint Maarten (Dutch part) - The island of Saint Martin is divided into the French northern part and the Dutch southern part",
      |        "SX"
      |    ],
      |    [
      |        "Syrian Arab Republic, Often referred to as Syria",
      |        "SY"
      |    ],
      |    [
      |        "Swaziland",
      |        "SZ"
      |    ],
      |    [
      |        "Turks and Caicos Islands (the)",
      |        "TC"
      |    ],
      |    [
      |        "Chad",
      |        "TD"
      |    ],
      |    [
      |        "French Southern Territories (the) - Including Kerguelen Islands, Amsterdam Island, Saint-Paul Island, Crozet Archipelago and French scattered Indian Ocean Islands formed by Bassas da India, Europa Island, Glorioso Islands, Juan de Nova Island and Tromelin Island",
      |        "TF"
      |    ],
      |    [
      |        "Togo",
      |        "TG"
      |    ],
      |    [
      |        "Thailand",
      |        "TH"
      |    ],
      |    [
      |        "Tajikistan",
      |        "TJ"
      |    ],
      |    [
      |        "Tokelau",
      |        "TK"
      |    ],
      |    [
      |        "Timor-Leste",
      |        "TL"
      |    ],
      |    [
      |        "Turkmenistan",
      |        "TM"
      |    ],
      |    [
      |        "Tunisia",
      |        "TN"
      |    ],
      |    [
      |        "Tonga",
      |        "TO"
      |    ],
      |    [
      |        "Turkey",
      |        "TR"
      |    ],
      |    [
      |        "Trinidad and Tobago",
      |        "TT"
      |    ],
      |    [
      |        "Tuvalu",
      |        "TV"
      |    ],
      |    [
      |        "Taiwan - Separate customs territory of Taiwan, Penghu, Kinmen and Matsu",
      |        "TW"
      |    ],
      |    [
      |        "Tanzania, United Republic of - Pemba, Zanzibar Island and Tanganyika",
      |        "TZ"
      |    ],
      |    [
      |        "Ukraine",
      |        "UA"
      |    ],
      |    [
      |        "Uganda",
      |        "UG"
      |    ],
      |    [
      |        "United States Minor Outlying Islands (the), Including Baker Island, Howland Island, Jarvis Island, Johnston Atoll, Kingman Reef, Midway Islands, Navassa Island, Palmyra Atoll and Wake Island",
      |        "UM"
      |    ],
      |    [
      |        "United States of America (the), Including Puerto Rico",
      |        "US"
      |    ],
      |    [
      |        "Uruguay",
      |        "UY"
      |    ],
      |    [
      |        "Uzbekistan",
      |        "UZ"
      |    ],
      |    [
      |        "Holy See (Vatican City State)",
      |        "VA"
      |    ],
      |    [
      |        "Saint Vincent and the Grenadines",
      |        "VC"
      |    ],
      |    [
      |        "Venezuela (Bolivarian Republic of), Often referred to as Venezuela",
      |        "VE"
      |    ],
      |    [
      |        "Virgin Islands (British)",
      |        "VG"
      |    ],
      |    [
      |        "Virgin Islands (U.S.)",
      |        "VI"
      |    ],
      |    [
      |        "Vietnam",
      |        "VN"
      |    ],
      |    [
      |        "Vanuatu",
      |        "VU"
      |    ],
      |    [
      |        "Wallis and Futuna, Including Alofi Island",
      |        "WF"
      |    ],
      |    [
      |        "Samoa, Formerly known as Western Samoa",
      |        "WS"
      |    ],
      |    [
      |        "Ceuta",
      |        "XC"
      |    ],
      |    [
      |        "Kosovo - As defined by United Nations Security Council Resolution 1244 of 10 June 1999",
      |        "XK"
      |    ],
      |    [
      |        "Melilla, Including Penon de Velez de la Gomera,Penon de Alhucemas and Chafarinas Islands",
      |        "XL"
      |    ],
      |    [
      |        "Serbia",
      |        "XS"
      |    ],
      |    [
      |        "Yemen, Formerly North Yemen and South Yemen",
      |        "YE"
      |    ],
      |    [
      |        "Mayotte - Grande-Terre and Pamandzi",
      |        "YT"
      |    ],
      |    [
      |        "South Africa",
      |        "ZA"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Belgian Sector",
      |        "ZB"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Greece Sector",
      |        "ZC"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Danish Sector",
      |        "ZD"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Irish Sector",
      |        "ZE"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - French Sector",
      |        "ZF"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - German Sector",
      |        "ZG"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Netherlands Sector",
      |        "ZH"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Cyprus Sector",
      |        "ZJ"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Finland Sector",
      |        "ZK"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Italy Sector",
      |        "ZL"
      |    ],
      |    [
      |        "Zambia",
      |        "ZM"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Norwegian Sector",
      |        "ZN"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - Sweden Sector",
      |        "ZS"
      |    ],
      |    [
      |        "Continental Shelf (NW European) - United Kingdom Sector",
      |        "ZU"
      |    ],
      |    [
      |        "Zimbabwe",
      |        "ZW"
      |    ]
      |]
      |""".stripMargin

  private val allCountries: List[Country] = {
    val countriesJson = Json.parse(countryMappings) match {
      case JsArray(countries) =>
        countries.toList.collect { case JsArray(scala.collection.Seq(name: JsString, code: JsString)) =>
          Country(name.value, code.value)
        }
      case _ => throw new IllegalArgumentException("Could not read JSON array of countries from string.")
    }

    countriesJson.sortBy(_.countryName)
  }

  private def getCountryCode(name: String): Option[String] =
    allCountries.find(country => country.countryName == name).map(_.countryCode)
}
