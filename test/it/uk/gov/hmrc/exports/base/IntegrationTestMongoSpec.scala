package uk.gov.hmrc.exports.base

import com.kenshoo.play.metrics.PlayModule
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.reflect.ClassTag

trait IntegrationTestMongoSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite with ExportsDeclarationBuilder {

  val databaseName = "test-customs-declare-exports"

  val configuration: Configuration =
    Configuration.from(Map(s"mongodb.uri" -> s"mongodb://localhost/$databaseName"))

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().configure(configuration).disable[PlayModule].build

  def getRepository[T](implicit classTag: ClassTag[T]): T = app.injector.instanceOf[T]
}
