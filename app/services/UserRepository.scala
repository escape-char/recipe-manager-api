package services
import javax.inject.{Inject, Singleton}
import play.api.Logger
import scala.util.Try
import anorm._
import play.api.db.Database
import models.User

/**
  * Service for handling users in database
  * @param db database which contains user relationships
  */
@Singleton
class UserRepository  @Inject()(db: Database) {
  private val logger = Logger(this.getClass())

  /**
    * transform sql row to User class without the hashed password
    */
  private val UserClassParser: RowParser[User] = (

    SqlParser.str("username") ~
      SqlParser.str("firstname") ~
      SqlParser.str("lastname") ~
      SqlParser.str("email") ~
      SqlParser.bool("is_admin") ~
      SqlParser.long("user_id")
    ) map {
    case columnvalue1 ~ columnvalue2 ~ columnvalue3 ~ columnvalue4 ~ columnvalue5 ~ columnvalue6 => // etc...
      User(columnvalue1, columnvalue2, columnvalue3, columnvalue4, None, columnvalue5, columnvalue6) // etc...
  }
  /**
    * transform sql row to User class with the hashed password for validation
    */
  private val UserPasswordClassParser: RowParser[User] = (
    SqlParser.str("username") ~
      SqlParser.str("firstname") ~
      SqlParser.str("lastname") ~
      SqlParser.str("email") ~
      SqlParser.str("password") ~
      SqlParser.bool("is_admin") ~
      SqlParser.long("user_id")
    ) map {
    case columnvalue1 ~ columnvalue2 ~ columnvalue3 ~ columnvalue4 ~ columnvalue5 ~ columnvalue6 ~ columnvalue7 => // etc...
      User(columnvalue1, columnvalue2, columnvalue3, columnvalue4, Some(columnvalue5),  columnvalue6, columnvalue7) // etc...
  }
  //convert sql rows to list of User classes without hashed password
  private val allUsersParser: ResultSetParser[List[User]] = UserClassParser.*
  //convert sql rows to list of User classes with hashed password
  private val allUsersPasswordParser: ResultSetParser[List[User]] = UserPasswordClassParser.*


  private val SQL_EXCEPTION_MSG = "An exception occured during SQL query."
  //query for retrieving users without hashed password
  private val GET_USERS_SQL: String =
    """
      |SELECT username, firstname, lastname, email, is_admin, user_id FROM users
      |   WHERE ({username} is NULL OR username={username}) AND
      |   ({email} is NULL OR email={email}) AND
      |   ({user_id} is NULL OR user_id={user_id})
      |    LIMIT {limit} OFFSET {offset}
      |""".stripMargin
  //query for retrieving user with hashed password by username
  private val GET_USER_WITH_PASSWORD_SQL: String =
    """
      |SELECT username, firstname, lastname, email, password, is_admin, user_id FROM users
      | WHERE username={username}
      |""".stripMargin


  //query for inserting a new user
  private val INSERT_USERS_SQL: String =
    """
      |INSERT INTO users(username, password, firstname, lastname, is_admin, email)
      |values ({username}, {password}, {firstname}, {lastname}, {is_admin}, {email})
    """.stripMargin
  //query for updating an existing user's password
  private val UPDATE_USERS_PASSWORD_SQL: String =
    """
      |UPDATE users SET username={username},
      | password='{password}',
      | WHERE id={id})
    """.stripMargin
  //query for updating an existing user
  private val UPDATE_USERS_SQL: String =
    """
      |UPDATE users SET username={username},
      | firstname={firstname},
      | lastname={lastname},
      | is_admin={is_admin},
      | email={email},
      | WHERE user_id={id})
    """.stripMargin

  //query for deleting a user
  private val DELETE_USERS_SQL: String =
    """
      | DELETE FROM users WHERE id={id}
    """.stripMargin

  /**
    * handles creating a new user
    * @param user new user to be created
    * @return unique id of created user
    */
  def insert(user: User): Try[Option[Long]] = {
    Try(db.withConnection {
        implicit c => {
           SQL(INSERT_USERS_SQL)
              .on("username" -> user.username,
              "password" -> user.password.getOrElse(""),
              "firstname" -> user.firstname,
              "lastname" -> user.lastname,
              "is_admin" -> user.isAdmin,
                "email" -> user.email).executeInsert()
        }
      }
    )
  }

  /**
    * handles changing a user's password
    * @param userId  unique id of user for password change
    * @param password new password of user
    * @return unique id of updated user
    */
  def updatePassword(userId:Long, password:String):Try[Int] ={
    val query:SimpleSql[Row] = SQL(UPDATE_USERS_PASSWORD_SQL).on(
    "password" -> password
    )
    Try(db.withConnection {
      implicit c => {
        query.executeUpdate()
      }
    })
  }

  /**
    * handles updating an existing user
    * @param user user to be updated
    * @return unique id of updated user
    */
  def update(user: User): Try[Int] ={
    val query:SimpleSql[Row] = SQL(UPDATE_USERS_SQL).on(
        "username" -> user.username,
        "firstname" -> user.firstname,
        "lastname" -> user.lastname,
        "is_admin" -> user.isAdmin,
        "id" -> user.id,
        "email" -> user.email)
    Try(db.withConnection {
      implicit c => {
        query.executeUpdate()
      }
    })
  }

  /**
    * handles deletion of an existing user
    * @param userId unique id of user to be deleted
    * @return unique id of deleted user
    */
  def delete(userId: Long): Try[Int] = {
    require(userId > 0)
     Try(db.withConnection {
      implicit c => {
        SQL(DELETE_USERS_SQL)
            .on("id" -> userId).executeUpdate()
      }
    })
  }

  /**
    * query for users
    * @param offset amount of users to skip from beginning
    * @param limit limit amount of users returned
    * @param username search string for exact username
    * @param email search strign for exact email
    * @param id search number for user's unique id
    * @return list of users based on query
    */
  def query(offset:Option[Int],
            limit:Option[Int],
            username:Option[String],
            email:Option[String],
            id:Option[Long]): Try[List[User]] = {
    logger.debug("query()")

    val limit2:Int = limit.getOrElse(0)
      //filter out any parameters which are null or zero . except for
      // offset for except which can default to zero
      val params:Seq[NamedParameter] = Seq(
        NamedParameter("username", ParameterValue.toParameterValue[Option[String]](username)),
        NamedParameter("email", ParameterValue.toParameterValue[Option[String]](email)),
        NamedParameter("user_id", ParameterValue.toParameterValue[Option[Long]](id)),
        NamedParameter("limit", ParameterValue.toParameterValue[Option[Int]](limit)),
        NamedParameter("offset",ParameterValue.toParameterValue[Option[Int]](offset)))

      Try(db.withConnection {
        implicit c => {
          SQL(GET_USERS_SQL).on(params:_*).as(allUsersParser)
        }
      })

  }

  /**
    * query by only unique  user id
    * @param userId unique id of user
    * @return list of users matching query
    */
  def queryById(userId:Long): Try[List[User]] = {
    Try(db.withConnection {
      implicit c => {
        SQL(GET_USERS_SQL).
          on("id" -> userId).as(allUsersParser)
      }
    })
  }

  /**
    * query by username and include the hashed password in the result
    *
    * useful username and password authentication
    *
    * @param username username of user to query for
    * @return list of users which match query
    */
  def queryByUsernameWithPassword(username:String): Try[List[User]] = {
      Try(db.withConnection {
        implicit c => {
          SQL(GET_USER_WITH_PASSWORD_SQL).
            on("username" -> username).as(allUsersPasswordParser)
        }
      })
  }
}

