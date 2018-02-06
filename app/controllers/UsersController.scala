package controllers
import javax.inject.{Inject}
import play.api.mvc.Controller
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import models.{User}
import services.{UserService, UserServiceResult}

/**
  * Controller for handling management of users
  * @param userService  service for handling users data
  */
class UsersController @Inject()(val userService:UserService) extends Controller with Secured{
  private val logger = Logger(this.getClass())
  logger.debug("UsersController()")

  /**
    * handles querying for users
    * @param offset  number of amount of users to skip from the beginning
    * @param limit  number for limiting the amount of users returned
    * @param username  string search for exact username of user
    * @param email string search for exact email of user
    * @param id id search for exact id of user
    * @return list of users if successful
    */
  def fetch(
                offset:Option[Int],
                limit:Option[Int],
                 username:Option[String],
                 email:Option[String],
                id:Option[Long]) = Authenticated { implicit request =>
    val result: UserServiceResult = userService.fetch(offset,
                                                      limit,
                                                      username,
                                                      email,
                                                      id)
    result.success match {
      case true => {
        val users:List[User] = result.data.asInstanceOf[List[User]]
        val length = users.length
        implicit val userWrites = Json.writes[User]
        Ok(Json.toJson(Json.obj("success" -> result.success, "users" -> users.toArray.map(u=>Json.toJson(u)))))
      }
      case false => {
        val message:String = result.data.asInstanceOf[String]
        InternalServerError(Json.toJson(Json.obj("success"->result.success, "error" -> message)))
      }
    }
  }

  /**
    * checks if username is already taken by another user
    * @param username - username to check if already taken
    * @return isTaken in JSON Body returns true if username is already taken
    */
  def isUsernameTaken(username:String) = Action{ implicit request =>
    logger.debug("isUsernameTaken()")
    logger.debug(s"username: $username")
    val result:UserServiceResult = userService.fetch(Some(0), None, Some(username), None, None)
    result.success match {
      case true => {
        val users: List[User] = result.data.asInstanceOf[List[User]]
        val length:Int = users.length
        logger.debug(s"length: $length")
        Ok(Json.toJson(Json.obj("success" -> result.success, "isTaken" -> users.nonEmpty)))
      }
      case false => {
        val message: String = result.data.asInstanceOf[String]
        InternalServerError(Json.toJson(Json.obj("success" -> result.success, "error" -> message)))
      }
    }
  }
  /**
    * checks if email is already taken by another user
    * @param email - email  to check if already taken
    * @return isTaken in JSON Body returns true if email is already taken
    */
  def isEmailTaken(email:String) = Action{ implicit request =>
    val result:UserServiceResult = userService.fetch(Some(0), None, None, Some(email), None)
    result.success match {
      case true => {
        val users: List[User] = result.data.asInstanceOf[List[User]]
        Ok(Json.toJson(Json.obj("success" -> result.success, "isTaken" -> users.nonEmpty)))
      }
      case false => {
        val message: String = result.data.asInstanceOf[String]
        InternalServerError(Json.toJson(Json.obj("success" -> result.success, "error" -> message)))
      }
    }
  }
}