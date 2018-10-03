package uk.gov.hmrc.exports.controllers

@Singleton
class NotificationsController @Inject()(
                                         appConfig: AppConfig,
                                         authConnector: AuthConnector
                                       ) extends ExportController(authConnector) {

}
