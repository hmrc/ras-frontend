package featuretoggle

trait FeatureToggleSupport {

  def setFeature(feature: Feature, enable: Boolean): Unit =
    sys.props += (feature.toString -> enable.toString)

  def removeOverride(feature: Feature): Unit =
    sys.props -= feature.toString

  def enable(feature: Feature): Unit = setFeature(feature, enable = true)

  def disable(feature: Feature): Unit = setFeature(feature, enable = false)

}
