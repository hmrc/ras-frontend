package testonly

import featuretoggle.{Feature, FeatureConfig, FeatureToggleSupport}
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.controller.FrontendController

class SetFeaturesController @Inject()(config: FeatureConfig) extends FrontendController with FeatureToggleSupport {

  def set(features: List[(Feature, Boolean)]): Action[AnyContent] = Action {
    features.foreach { case (feature, setting) => setFeature(feature, setting) }
    Ok(Json.toJson(features.map { case (feature, _) => Json.obj(feature.key -> config.isEnabled(feature)) }))
  }

}
