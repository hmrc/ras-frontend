package featuretoggle

sealed abstract class Feature(val key: String) {
  require(key.nonEmpty)

  override val toString = s"${Feature.prefix}.$key"
}

object Feature {

  val prefix = "feature-toggles"

  def allTogglableFeatures: Set[Feature] = Set(
    isBefore6April
  )

  def fromQuery(key: String): Option[Feature] =
    allTogglableFeatures.collectFirst {
      case feature if feature.key.toLowerCase == key.toLowerCase => feature
    }

}

case object isBefore6April extends Feature("isBefore6April")
