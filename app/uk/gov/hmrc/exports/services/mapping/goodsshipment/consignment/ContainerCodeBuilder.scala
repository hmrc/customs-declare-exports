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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.Container
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.declaration_ds.dms._2.ConsignmentContainerCodeType

class ContainerCodeBuilder @Inject() () extends ModifyingBuilder[Seq[Container], GoodsShipment.Consignment] {
  override def buildThenAdd(containers: Seq[Container], consignment: GoodsShipment.Consignment): Unit = {
    val codeType = new ConsignmentContainerCodeType()
    codeType.setValue(extractContainerCode(containers))
    consignment.setContainerCode(codeType)
  }

  private def extractContainerCode(containers: Seq[Container]): String =
    if (containers.nonEmpty) "1" else "0"
}
