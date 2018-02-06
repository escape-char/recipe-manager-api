import scala.concurrent.Future
import org.scalatestplus.play._
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.mockito.Matchers._
import play.api.libs.json._
import controllers.{AuthController, CategoriesController, NewCategory}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import services.{CategoryService, CategoryServiceResult, UserService, UserServiceResult}
import models.{Category, CategoryBrief, User}
import org.scalatest.{BeforeAndAfterAll, ConfigMap}
import play.api.libs.json.Json.{obj, toJson}

/**
  * Spec for testing the CategoriesController
  */
class CategoriesControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterAll{
  private var token:String = ""
  private val cats:List[Category] = List(
    Category(1, "Test1", false,  3),
    Category(2, "Test2", false, 3),
    Category(3, "Test3", false, 3)
  )
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
  "CategoriesController#fetch" should {
    "not allow unauthenticated requests" in {
      val mockCategoryService: CategoryService = mock[CategoryService]
      when(mockCategoryService.fetch(null, null, null, null, Some(this.authUser.id))) thenReturn CategoryServiceResult(success = true, data = cats)
      val controller = new CategoriesController(mockCategoryService)
      val req: Request[AnyContent] = FakeRequest()
      val result: Future[Result] = controller.fetchMine(null, null, null, null, null).apply(req)
      status(result) mustBe 401
    }
  }
}