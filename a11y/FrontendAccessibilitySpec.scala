import config.ApplicationConfig
import forms._
import models.FileUploadStatus
import play.twirl.api.Html
import uk.gov.hmrc.scalatestaccessibilitylinter.views.AutomaticAccessibilitySpec
import views.html._
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.mvc.RequestHeader

class FrontendAccessibilitySpec
  extends AutomaticAccessibilitySpec {

  private val appConfig: ApplicationConfig                = app.injector.instanceOf[ApplicationConfig]
  implicit val arbRequestHeader: Arbitrary[RequestHeader] = fixed(fakeRequest)
  implicit val arbAppConfig: Arbitrary[ApplicationConfig] = fixed(appConfig)

  implicit val arbMemberDateOfBirth:
    Arbitrary[Form[models.MemberDateOfBirth]]             = fixed(MemberDateOfBirthForm.apply(Some("name")))
  implicit val arbMemberName:
    Arbitrary[Form[models.MemberName]]                    = fixed(MemberNameForm.form)
  implicit val arbMemberNINO:
    Arbitrary[Form[models.MemberNino]]                    = fixed(MemberNinoForm.apply(Some("firstName")))
  implicit val arbFileUploadStatus:
    Arbitrary[FileUploadStatus.Value]                     = fixed(models.FileUploadStatus.NoFileSession)


  val viewPackageName = "views.html"

  override def renderViewByClass: PartialFunction[Any, Html] =
  {
    case error: error                                               => render(error)
    case cannot_upload_another_file: cannot_upload_another_file     => render(cannot_upload_another_file)
    case choose_an_option: choose_an_option                         => render(choose_an_option)
    case file_not_available: file_not_available                     => render(file_not_available)
    case file_ready: file_ready                                     => render(file_ready)
    case file_upload: file_upload                                   => render(file_upload)
    case file_upload_successful: file_upload_successful             => render(file_upload_successful)
    case global_error: global_error                                 => render(global_error)
    case global_page_not_found: global_page_not_found               => render(global_page_not_found)
    case match_found: match_found                                   => render(match_found)
    case match_not_found: match_not_found                           => render(match_not_found)
    case member_dob: member_dob                                     => render(member_dob)
    case member_name: member_name                                   => render(member_name)
    case member_nino: member_nino                                   => render(member_nino)
    case no_results_available: no_results_available                 => render(no_results_available)
    case problem_uploading_file: problem_uploading_file             => render(problem_uploading_file)
    case results_not_available_yet: results_not_available_yet       => render(results_not_available_yet)
    case sorry_you_need_to_start_again:
      sorry_you_need_to_start_again                                 => render(sorry_you_need_to_start_again)
    case unauthorised: unauthorised                                 => render(unauthorised)
    case upload_result: upload_result => render(upload_result)
  }

  runAccessibilityTests()

  override def layoutClasses: Seq[Class[govuk_wrapper]] = Seq(classOf[govuk_wrapper])
}

