package testonly

import featuretoggle.Feature
import play.api.mvc.QueryStringBindable

import scala.util.Try

object FeatureQueryBinder {

  implicit def queryBinder: QueryStringBindable[List[(Feature, Boolean)]] = new QueryStringBindable[List[(Feature, Boolean)]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, List[(Feature, Boolean)]]] = {
      val requestedMaybeFeatures: List[Option[Feature]] = params.keySet.map(Feature.fromQuery).toList
      if (requestedMaybeFeatures.exists(_.isEmpty)) {
        Some(Left("contains unknown feature"))
      } else {
        val featuresWithMaybeSettings: List[(Feature, Option[Seq[String]])] =
          requestedMaybeFeatures.collect {
            case Some(feature) => (feature, params.get(params.keySet.find(_.toLowerCase == feature.key.toLowerCase).get))
          }

        if (featuresWithMaybeSettings.exists(_._2.isEmpty)) {
          Some(Left("has missing settings"))
        } else if (featuresWithMaybeSettings.exists(_._2.exists(_.size > 1))) {
          Some(Left("contains duplicate settings for the same feature"))
        } else {
          val featuresWithMaybeSetting: List[(Feature, Option[Boolean])] =
            featuresWithMaybeSettings.collect { case (feature, Some(maybeBool)) => (feature, Try(maybeBool.head.toBoolean).toOption) }

          if (featuresWithMaybeSetting.exists(_._2.isEmpty)) {
            Some(Left("contains none boolean settings"))
          } else {
            Some(Right(featuresWithMaybeSetting.collect { case (feature, Some(bool)) => (feature, bool) }))
          }
        }
      }
    }

    override def unbind(key: String, features: List[(Feature, Boolean)]): String =
      s"""$key=${features.map { case (feature, enable) => s"${feature.key}=$enable" }.mkString("&")}"""

  }

}
