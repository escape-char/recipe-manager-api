package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * represents an existing user
  * @param username username for user
  * @param firstname firstname for user
  * @param lastname  lastname for user
  * @param email  email for user
  * @param password password for user
  * @param isAdmin  whether user is an admin
  * @param id  unique id for user
  */
case class User(username:String="",
                      firstname:String="",
                      lastname:String="",
                      email:String = "",
                      password:Option[String]=None,
                      isAdmin:Boolean=false,
                      id: Long = 0
                     )

/**
  * model for authenticating a user
  * @param username  user's username for authentication
  * @param password  user's password for authentication
  */
case class AuthUser(username:String, password:String)

/**
  * Object for handling JSON reads/writes of User class
  */
object User {
  implicit val userWrites = new Writes[User]{
    def writes(user:User)=Json.obj(
      "username" -> user.username,
      "firstname" -> user.firstname,
      "lastname" ->user.lastname,
      "email" -> user.email,
      "password" -> user.password,
      "isAdmin" -> user.isAdmin,
      "id" -> user.id
    )
  }
  implicit val userReads: Reads[User] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "firstname").read[String] and
      (JsPath \ "lastname").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "password").formatNullable[String] and
      (JsPath \ "isAdmin").read[Boolean] and
      (JsPath \ "id").read[Long]
    )(User.apply _)
}