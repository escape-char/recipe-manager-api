package services
import org.mindrot.jbcrypt.BCrypt
import scala.util.{Failure, Success, Try}
import javax.inject.{Inject, Singleton}
import play.api.Logger

import models.User

/**
  * Response class for UserService
  * @param success whether call was successful
  * @param data any data returned by the call
  */
case class UserServiceResult(success:Boolean, data: Any)

/**
  * service for handling management of users
  * @param repo
  */
@Singleton
class UserService @Inject()(val repo: UserRepository) {
  private val logger = Logger(this.getClass())

  val VALIDATION_ERROR:String  = "Incorrect username or password."

  /**
    * hashes a password with a salt
    * @param p password string to be hashed
    * @return hashed password
    */
  def hashPassword(p:Option[String]): UserServiceResult= {
    UserServiceResult(true, Some(BCrypt.hashpw(p.getOrElse(""), BCrypt.gensalt())))
  }

  /**
    * checks if unhashed password and hashed password is a match
    * @param password  unhashed password
    * @param hashedPW  hashed password
    * @return true if the hashed and unhashed password are a match
    */
  def checkPassword(password:String, hashedPW:String):Boolean ={
    BCrypt.checkpw(password, hashedPW)
  }

  /**
    * query for users
    * @param offset amount to skip users from the beginning
    * @param limit  amount to limit the amount of users returned
    * @param username search string for exact username
    * @param email search string for exact email
    * @param id search number for exact unique user id
    * @return list of users based on query
    */
  def fetch(offset: Option[Int]=Some(0),
            limit: Option[Int],
            username: Option[String],
            email: Option[String],
            id: Option[Long]):UserServiceResult = {
    val result:Try[List[User]] = repo.query(offset,
                                            limit,
                                           username,
                                          email,
                                          id)
    result match{
      case Success(v) => UserServiceResult(true, v)
      case Failure(e) => UserServiceResult(false, e)
    }
  }

  /**
    * validate user name and password for authentication
    * @param username user's username
    * @param password user's password
    * @return success=true and user information if validation is successful
    */
  def validate(username:String, password:String): UserServiceResult= {
    val result: Try[List[User]] = repo.queryByUsernameWithPassword(username)
    result match{
      case Success(v) =>
        if(v.isEmpty){
          UserServiceResult(false, VALIDATION_ERROR)
       }else {
        val user:User=  v.head
        if(!this.checkPassword(password, user.password.getOrElse("")))  {
          UserServiceResult(false, VALIDATION_ERROR)
        }else {
          UserServiceResult(true,  user)
        }
       }
      case Failure(e) => UserServiceResult(false, e.toString)
    }
  }

  /**
    * handles creation of a new user
    * @param user  user to be created
    * @return new user with unique id
    */
  def create(user:User): UserServiceResult = {
    val result: Try[Option[Long]] = repo.insert(user)
    result match{
      case Success(v) => {
        val newUser:User = User(
          username=user.username,
          firstname=user.firstname,
          lastname=user.lastname,
          id=v.getOrElse(0)
        )
        UserServiceResult(true, newUser)
      }
      case Failure(e) => UserServiceResult(false, e)
    }
  }

  /**
    * handles changing a user's password
    * @param userId  unique id of user requesting password change
    * @param password  new password for user
    * @return unique id of updated user
    */
  def changePassword(userId: Long, password:String): UserServiceResult ={
    val result: Try[Int] = repo.updatePassword(userId, password)
    result match{
      case Success(v) => UserServiceResult(true, v)
      case Failure(e) => UserServiceResult(false, e)
    }
  }

  /**
    * handles updating an existing user
    * @param user user to be upated
    * @return unique id for updated user
    */
  def update(user: User): UserServiceResult = {
    val result: Try[Int] = repo.update(user)
    result match{
      case Success(v) => UserServiceResult(true, v)
      case Failure(e) => UserServiceResult(false, e)
    }

  }

  /**
    * handles deletion of a user
    * @param userId unique id of user to be deleted
    * @return unique id of deleted user
    */
  def delete(userId: Long): UserServiceResult = {
    val result: Try[Int] = repo.delete(userId)
    result match{
      case Success(v) => UserServiceResult(true, v)
      case Failure(e) => UserServiceResult(false, e)
    }

  }
}
