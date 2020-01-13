package featuretoggle

import javax.inject.Inject
import play.api.Environment
import uk.gov.hmrc.play.config.ServicesConfig

import scala.util.Try

class FeatureConfig @Inject()(override val runModeConfiguration: play.api.Configuration,
                              environment: Environment
                             ) extends ServicesConfig {

  override protected def mode: play.api.Mode.Mode = environment.mode

  def isEnabled(feature: Feature): Boolean =
    sys.props.get(feature.toString).flatMap(prop => Try(prop.toBoolean).toOption)
      .orElse(runModeConfiguration.getBoolean(feature.toString))
      .getOrElse(false)

}
