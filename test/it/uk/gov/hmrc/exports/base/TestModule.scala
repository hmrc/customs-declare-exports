package uk.gov.hmrc.exports.base

import com.google.inject.AbstractModule
import play.api.inject.guice.GuiceableModule

object TestModule extends AbstractModule {

  override def configure(): Unit = ()

  def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
}
