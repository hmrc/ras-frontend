package testonly

import featuretoggle.{Feature, FeatureConfig, FeatureToggleSupport}
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController

class ResetFeaturesController @Inject()(config: FeatureConfig) extends FrontendController with FeatureToggleSupport {

  def reset(): Action[AnyContent] = Action {
    val features = Feature.allTogglableFeatures
    features.foreach(removeOverride)
    Ok(Json.toJson(features.map(feature => Json.obj(feature.key -> config.isEnabled(feature)))))
  }

}
