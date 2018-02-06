import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{JsDefined, JsUndefined, JsValue}
import play.api.libs.json.Json.{obj, toJson}
import controllers.AuthController
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.{UserService, UserServiceResult}
import models.User

/**
  * Spec for testing AuthController
  */
class AuthenticationControllerSpec extends PlaySpec with MockitoSugar with Results{
  "AuthenticationController#auth" should {
    "not allow failed authentication" in {
      val user: User = User("idunnomypassword", "first", "last", "test@test", Some("makeoneup"), false)
      val mockUserService:UserService = mock[UserService]

      when(mockUserService.validate(user.username, user.password.getOrElse(""))) thenReturn UserServiceResult(success = false, data = "Denied!")
      val controller = new AuthController(mockUserService)
      var json: JsValue =toJson(obj("username"->user.username, "password" -> user.password))
      val req:Request[JsValue]= FakeRequest().withBody(json).withHeaders(("Content-Type", "application/json"))
      val result:Future[Result] =  controller.auth().apply(req)
      val token:Option[String] = header("Authorization", result)
      val body:JsValue = contentAsJson(result)

      status(result) mustBe 400
      token.isDefined mustBe false
      (body\"user").isInstanceOf[JsUndefined] mustBe true
    }
    "allow successful authentication with JWT and full user details" in {
      val user: User = User("thisshouldwork", "first", "last", "test@test", Some("Testing!1"), false, 2)
      val mockUserService:UserService = mock[UserService]

      when(mockUserService.validate(user.username, user.password.getOrElse(""))) thenReturn UserServiceResult(success=true, data = user)
      val controller = new AuthController(mockUserService)
      var json: JsValue =toJson(obj("username"->user.username, "password" -> user.password))
      val req:Request[JsValue]= FakeRequest().withBody(json).withHeaders(("Content-Type", "application/json"))
      val result:Future[Result] =  controller.auth().apply(req)
      val token:Option[String] = header("Authorization", result)
      val body:JsValue = contentAsJson(result)

      status(result) mustBe 200
      token.isDefined mustBe true
      token.getOrElse("").contains("Bearer") mustBe true
      (body\"user").isInstanceOf[JsDefined] mustBe true
      (body\"user"\"id").as[Long] mustBe user.id
    }
  }
}