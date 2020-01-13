package testonly

import featuretoggle.{Feature, FeatureConfig, FeatureToggleSupport}
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController

private class GetFeaturesController @Inject()(config: FeatureConfig) extends FrontendController with FeatureToggleSupport {

  def getAllFeatures(): Action[AnyContent] = Action {
    val features = Feature.allTogglableFeatures
    Ok(Json.toJson(features.map(feature => Json.obj(feature.key -> config.isEnabled(feature)))))
  }

}
