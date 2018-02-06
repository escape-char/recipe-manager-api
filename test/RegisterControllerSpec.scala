import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.mockito.Matchers._
import play.api.libs.json._
import controllers.RegisterController
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.{CategoryService, CategoryServiceResult, UserService, UserServiceResult}
import models.{CategoryBrief, User}

import scala.util.Try

/**
  * Spec for testing the RegisterController
  */
class RegisterControllerSpec extends PlaySpec with MockitoSugar with Results{
  "RegisterController#register" should {
    "not allow empty usernames " in {
      val user: User = User("", "first", "last", "test@test", Some("testing!!"))
      val mockUserService:UserService = mock[UserService]
      val mockCategoryService:CategoryService = mock[CategoryService]

      when(mockUserService.hashPassword(user.password)) thenReturn UserServiceResult(success = true, data = "##########")
      when(mockUserService.create(user)) thenReturn UserServiceResult(success = true, data = Try(user.copy(id = 1)))

      val controller = new RegisterController(mockUserService, mockCategoryService)
      var json: JsValue = Json.toJson(user)
      val req = FakeRequest().copyFakeRequest(body = json).withHeaders(("Content-Type", "application/json"))
      val result:Future[Result] = controller.register().apply(req)
      status(result) mustBe 400
    }
    "not allow empty passwords" in {

      val user: User = User("testuser", "first", "last", "test@test.not", Some(""))
      val mockUserService:UserService = mock[UserService]
      val mockCategoryService:CategoryService = mock[CategoryService]

      when(mockUserService.hashPassword(user.password)) thenReturn UserServiceResult(success = true, data = "##########")
      when(mockUserService.create(user)) thenReturn UserServiceResult(success = true, data = Try(user.copy(id = 1)))

      val controller = new RegisterController(mockUserService, mockCategoryService)
      var json: JsValue = Json.toJson(user)
      val req = FakeRequest().copyFakeRequest(body = json).withHeaders(("Content-Type", "application/json"))
      val result:Future[Result] = controller.register().apply(req)
      status(result) mustBe 400
    }
    "not allow non-complex passwords" in {

      val user: User = User("testuser", "first", "last", "test@test.not", Some("simplepassword"))
      val mockUserService:UserService = mock[UserService]
      val mockCategoryService:CategoryService = mock[CategoryService]

      when(mockUserService.hashPassword(user.password)) thenReturn UserServiceResult(success = true, data = "##########")
      when(mockUserService.create(user)) thenReturn UserServiceResult(success = true, data = Try(user.copy(id = 1)))

      val controller = new RegisterController(mockUserService, mockCategoryService)
      var json: JsValue = Json.toJson(user)
      val req = FakeRequest().copyFakeRequest(body = json).withHeaders(("Content-Type", "application/json"))
      val result:Future[Result] = controller.register().apply(req)
      status(result) mustBe 400
    }
    "register new user with JWT in authorization of response header" in {
      val user: User = User("testuser", "first", "last", "test@test.not", Some("MyPassword123$$$$"))
      val cat:CategoryBrief = CategoryBrief(0, "Testing", true)
      val mockUserService:UserService = mock[UserService]
      val mockCategoryService:CategoryService = mock[CategoryService]
      when(mockUserService.create(any[User])) thenReturn UserServiceResult(success = true, data = user)
      when(mockUserService.hashPassword(user.password)) thenReturn UserServiceResult(success = true, data = "##########")
      when(mockCategoryService.create(any[CategoryBrief], any[Long])) thenReturn CategoryServiceResult(success = true, data = cat)


      val controller = new RegisterController(mockUserService, mockCategoryService)
      var json: JsValue = Json.toJson(user)
      val req = FakeRequest().copyFakeRequest(body = json).withHeaders(("Content-Type", "application/json"))
      val result:Future[Result] = controller.register().apply(req)
      val statusInt:Int = status(result)
      val token:Option[String] = header("Authorization", result)
      val body:JsValue = contentAsJson(result)
      status(result) mustBe 201
      (body\"success").as[Boolean] mustBe true
      (body\\"username")(0).as[String] mustBe user.username
      println("token: " + token.get)
      token.isDefined mustBe true
      token.getOrElse("").contains("Bearer") mustBe true

    }
  }
}