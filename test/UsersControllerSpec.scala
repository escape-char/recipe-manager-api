import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{JsValue}
import play.api.libs.json.Json.{obj, toJson}
import controllers.{AuthController, UsersController}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, ConfigMap}
import services.{UserService, UserServiceResult}
import models.User

/**
  * Spec for testing the UsersController
  */
class UsersControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterAll{
  private val users: List[User] = List(
        User("iamuser1", "first", "last", "test1@test.not", Some("makeoneup"), false, 1),
        User("iamuser2", "first", "last", "test2@test.not", Some("makeoneup"), false, 2),
        User("iamuser2", "first", "last", "test2@test.not", Some("makeoneup"), false, 3))

  private var token:String = "";

  private val authUser:User = User("usertester", "first", "last", "test@test", Some("Testing!1"), false, 1)

  override def beforeAll(configMap: ConfigMap): Unit = {
    //authenticate to get the token
    val user: User = User("thisshouldwork", "first", "last", "test@test", Some("Testing!1"), false, 2)
    val mockUserService:UserService = mock[UserService]
    when(mockUserService.validate(user.username, user.password.getOrElse(""))) thenReturn UserServiceResult(success=true, data = user)
    val controller = new AuthController(mockUserService)
    var json: JsValue =toJson(obj("username"->user.username, "password" -> user.password))
    val req:Request[JsValue]= FakeRequest().withBody(json).withHeaders(("Content-Type", "application/json"))
    val result:Future[Result] =  controller.auth().apply(req)
    val token:Option[String] = header("Authorization", result)
    val body:JsValue = contentAsJson(result)
    this.token = token.getOrElse("")
  }
  "UsersController#fetch" should {
    "not allow unauthenticated users" in {
      val mockUserService: UserService = mock[UserService]
      when(mockUserService.fetch(null, null, null, null, null)) thenReturn UserServiceResult(success = true, data = users)
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest()
      val result: Future[Result] = controller.fetch(null, null, null, null, null).apply(req)
      status(result) mustBe 401

    }
    "return all users if no offset or limit" in {
      val mockUserService: UserService = mock[UserService]
      when(mockUserService.fetch(null, null, null, null, null)) thenReturn UserServiceResult(success = true, data = users)
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest().withHeaders(("Authorization", this.token))
      val result: Future[Result] = controller.fetch(null, null, null, null, null).apply(req)
      val body: JsValue = contentAsJson(result)

      status(result) mustBe 200
      (body\"success").as[Boolean] mustBe true
      (body\"users").as[List[User]].length mustBe 3

    }
    "return users based on offset and limit" in {
      val OFFSET:Int = 1;
      val LIMIT:Int = 2;
      val mockUserService: UserService = mock[UserService]
      when(mockUserService.fetch(Some(OFFSET), Some(LIMIT), null, null, null)) thenReturn UserServiceResult(success = true,
        data = users.drop(OFFSET).take(LIMIT))
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest().withHeaders(("Authorization", this.token))
      val result: Future[Result] = controller.fetch(Some(OFFSET), Some(LIMIT), null, null, null).apply(req)
      val body: JsValue = contentAsJson(result)

      status(result) mustBe 200
      (body\"success").as[Boolean] mustBe true
      (body\"users").as[List[User]].length mustBe 2

    }
  }
  "UsersController#isUsernameTaken" should {
    "return true if username is taken" in {
      val mockUserService: UserService = mock[UserService]
      when(mockUserService.fetch(Some(0), None,  Some(this.users(0).username), None, None)) thenReturn UserServiceResult(success = true, data = List(users(0)))
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest().withHeaders(("Authorization", this.token))
      val result: Future[Result] = controller.isUsernameTaken(this.users(0).username).apply(req)
      val body: JsValue = contentAsJson(result)

      status(result) mustBe 200
      (body\"success").as[Boolean] mustBe true
      (body\"isTaken").as[Boolean] mustBe true

    }
    "return false if username is not taken" in {
      val mockUserService: UserService = mock[UserService]
      val NOT_TAKEN:String = "imnottaken"
      when(mockUserService.fetch(Some(0), None,  Some(NOT_TAKEN), None, None)) thenReturn UserServiceResult(success = true, data = List())
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest().withHeaders(("Authorization", this.token))
      val result: Future[Result] = controller.isUsernameTaken(NOT_TAKEN).apply(req)
      val body: JsValue = contentAsJson(result)

      status(result) mustBe 200
      (body\"success").as[Boolean] mustBe true
      (body\"isTaken").as[Boolean] mustBe false
    }
  }
  "UsersController#isEmailTaken" should {
    "return true if email is taken" in {
      val mockUserService: UserService = mock[UserService]
      when(mockUserService.fetch(Some(0), None, None, Some(this.users(0).email), None)) thenReturn UserServiceResult(success = true, data = List(users(0)))
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest().withHeaders(("Authorization", this.token))
      val result: Future[Result] = controller.isEmailTaken(this.users(0).email).apply(req)
      val body: JsValue = contentAsJson(result)

      status(result) mustBe 200
      (body \ "success").as[Boolean] mustBe true
      (body \ "isTaken").as[Boolean] mustBe true

    }
    "return false if email is not taken" in {
      val mockUserService: UserService = mock[UserService]
      val NOT_TAKEN: String = "imnottaken@notaken"
      when(mockUserService.fetch(Some(0), None, None, Some(NOT_TAKEN),None)) thenReturn UserServiceResult(success = true, data = List())
      val controller = new UsersController(mockUserService)
      val req: Request[AnyContent] = FakeRequest().withHeaders(("Authorization", this.token))
      val result: Future[Result] = controller.isEmailTaken(NOT_TAKEN).apply(req)
      val body: JsValue = contentAsJson(result)

      status(result) mustBe 200
      (body \ "success").as[Boolean] mustBe true
      (body \ "isTaken").as[Boolean] mustBe false
    }
  }
}