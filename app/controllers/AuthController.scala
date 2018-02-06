package controllers
import javax.inject._

import play.api.mvc._
import play.api.data._
import play.api.Logger
import play.api.data.Forms._
import play.api.libs.json.Json
import pdi.jwt.JwtSession._
import models.{AuthUser, User}
import services.{UserService, UserServiceResult, Utils}


/**
  * Controller for handling authentication such as signing in or signing out
  *
  * This API uses JWT for authentication handling. The JWT will appear in the response Authorization header.
  *
  * @constructor  creates a new controller with userService as a parameter
  * @param userService a service used for user authentication handling
  *
  */
class AuthController @Inject()(val userService:UserService) extends Controller with Secured{

  //form for validating JSON with username and password
  private val userForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(AuthUser.apply)(AuthUser.unapply)
  )
  private val logger = Logger(this.getClass())
  logger.debug("AuthController()")

  /** Authenticates user for access to the rest of the API with a JWT
    *
    * JWT token will be in the Authorization header and user details with be in the JSON body
    * if successful. The json body should contain the username and password attributes.
    *
    * @return JWT token in Authorization header and user details in JSON body if successful.
    */
  def auth= Action(parse.json){ implicit request =>
    logger.debug("auth()")
    userForm.bindFromRequest.fold(
      formWithErrors =>{
        BadRequest(Json.toJson(Json.obj("success"->false,
          "error" ->Utils.flattenFormErrors(formWithErrors.errors))))      },
      user => {
        val result:UserServiceResult = userService.validate(user.username, user.password)
        result.success match{
          case true =>{
            val me: User = result.data.asInstanceOf[User]
            logger.debug("successfully authenticated user")
            Ok(Json.toJson(Json.obj("success"->false, "user"->me)))
              .addingToJwtSession("user", me)
          }
          case false => {
            logger.error("Validation Failed.")
            val message:String = result.data.asInstanceOf[String]
            BadRequest(Json.toJson(Json.obj("success"->false,  "error"-> message)))
          }
        }
      }
    )

  }
 /**  Signs user out and makes JWT invalid.
  */
  def signout= Authenticated { implicit request =>
    Ok(Json.toJson(Json.obj("success" -> true))).withoutJwtSession
  }
}