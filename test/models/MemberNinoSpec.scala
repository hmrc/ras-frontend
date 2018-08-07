package models

import helpers.helpers.I18nHelper
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class MemberNinoSpec extends UnitSpec with I18nHelper with OneAppPerSuite  {

  "hasValue" should {

    "return false if nino is empty" in {
        val nino = MemberNino("")
        assert(nino.hasAValue() == false)
    }
  }

}
