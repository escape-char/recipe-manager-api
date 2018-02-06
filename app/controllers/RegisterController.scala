package controllers
import javax.inject.{Inject}

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Controller
import play.api.mvc._
import pdi.jwt.JwtSession._
import play.api.Logger
import play.api.libs.json._
import models.{User, CategoryBrief}
import services.{UserService,
              UserServiceResult,
              CategoryService,
              Utils}

/**
  * class for handling registration data
  * @param username - username for user
  * @param password - password for user
  * @param email - email for user
  * @param firstname - firstname for user
  * @param lastname - lastname for user
  */
case class Register(username:String, password:String, email:String, firstname:String, lastname:String)

/**
  * Controller for handling user registration
  *
  * @param userService - service for creating the new user
  * @param categoryService - service for populating new users with default categories
  */
class RegisterController @Inject()(val userService:UserService, val categoryService:CategoryService) extends Controller {
  private val DEFAULT_ALL_CAT = "All"
  private val DEFAULT_FAV_CAT = "Favorites"

  private val logger = Logger(this.getClass())
  logger.debug("RegisterController()")

  //form for validating registration JSON data
  private val registerForm = Form(
    mapping(
      "username" ->(text.verifying("Username is required.", {!_.isEmpty})
                    verifying("Username is invalid.", {_.matches("^[A-z]+\\w+$")})
                    verifying("Username must be less than 50 characters", {_.length < 50})
        ),
      "password" -> (text.verifying("Password is required.", {!_.isEmpty})
                    verifying("Password must be at least 8 characters.", {_.length > 8})
                    verifying("Password must at least one uppercase letter, lowercase letter, and a symbol.",
                    (p)=>Utils.checkPasswordComplexity(p))

      ),
      "email" -> (text.verifying("Email is required.", {!_.isEmpty})
                  verifying("Email is invalid.", (e)=>Utils.checkEmail(e))
        ),
      "firstname" -> text.verifying("First name is required.", {!_.isEmpty}),
      "lastname" ->text.verifying("Last name is required.", {!_.isEmpty})
    )(Register.apply)(Register.unapply)
  )

  /**
    * handles registration of a new user
    *
    * @return JWT token  in Authorization response header and new user with unique ID
    *         in the  response JSON body if successful
    */
  def register() = Action(parse.json) { implicit request =>
    logger.debug("register()")
    registerForm.bindFromRequest.fold(
      formWithErrors =>{
        BadRequest(Json.toJson(Json.obj("success"->false,
                "error" ->Utils.flattenFormErrors(formWithErrors.errors))))
      },
      reg => {
        val username:String = reg.username
        val password:String = reg.password
        val user:User = User(
          username=reg.username,
          password=Some(userService.hashPassword(Some(reg.password)).data.asInstanceOf[String]),
          firstname=reg.firstname,
          lastname=reg.lastname,
          email=reg.email
        )
        val result:UserServiceResult = userService.create(user)
        result.success match {
          case true => {
            val newUser: User = result.data.asInstanceOf[User]
            //initialize new user with default categories
            val defCat:CategoryBrief = CategoryBrief(0, DEFAULT_ALL_CAT, true)
            val defCat2:CategoryBrief = CategoryBrief(0, DEFAULT_FAV_CAT, true)
            categoryService.create(defCat, newUser.id)
            categoryService.create(defCat2, newUser.id)
            Created(Json.toJson(Json.obj("success"->true, "user"->Json.toJson(newUser))))
              .addingToJwtSession("user", Json.toJson[User](newUser))
          }

          case false =>{
            val message:String = result.data.asInstanceOf[String]
            Unauthorized(Json.toJson(Json.obj("success"->false, "error" -> message)))
          }

        }
      }
    )
  }
}