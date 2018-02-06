import org.scalatestplus.play._
import org.mockito.Mockito._
import scala.util.Try
import org.scalatest.mock.MockitoSugar

import models.User
import services.{UserService, UserRepository, UserServiceResult}

/**
  * Spec for testing the UserService
  */
class UserServiceSpec extends PlaySpec with MockitoSugar {

  "UserService#create" should {
    "return newly created user's id" in {
      val user: User = User("test@test", "test", "last", "email@test", Some("password"), false)
      val NEW_USER_ID = 22
      val mockRepo = mock[UserRepository]
      when(mockRepo.insert(user)) thenReturn Try(Some(NEW_USER_ID.toLong))
      val service = new UserService(mockRepo)
      val result: UserServiceResult = service.create(user)

      result.success mustBe true
      result.data.asInstanceOf[User].id mustBe NEW_USER_ID
    }
    "return unsuccessful in an exception occurs" in {

      val NEW_USER_ID:Long = 22
      val mockRepo = mock[UserRepository]
      val user: User = User("test@test", "test", "last", "email@test", Some("testing"), false)
      when(mockRepo.insert(user)) thenReturn Try(Some((2 / 0).toLong))
      val service = new UserService(mockRepo)
      val result: UserServiceResult = service.create(user)
      result.success mustBe false
    }
  }
  "UserService#checkPassword" should {
    "successfully validate a hashed password" in {
      val PASSWORD = "testing"
      val mockRepo:UserRepository = mock[UserRepository]
      val service = new UserService(mockRepo)
      val resultHash:String = service.hashPassword(Some(PASSWORD)).data.asInstanceOf[Some[String]].get
      service.checkPassword(PASSWORD, resultHash) mustBe true
    }
  }
  "UserService#update" should {
    "return return successful with user's id on update" in {
      val UPDATE_USER_ID:Int = 5
      val user: User = User( "test@test", "test", "last", "email@test", Some("password"), false, UPDATE_USER_ID)
      val mockRepo = mock[UserRepository]
      when(mockRepo.update(user)) thenReturn Try(UPDATE_USER_ID)
      val service = new UserService(mockRepo)
      val result: UserServiceResult = service.update(user)

      result.success mustBe true
      result.data.asInstanceOf[Int] mustBe UPDATE_USER_ID
    }
    "return unsuccessful when an exception occurs" in {

      val NEW_USER_ID = 22
      val mockRepo = mock[UserRepository]
      val user: User = User("test@test", "test", "last", "email@test", Some("testing"), false)
      when(mockRepo.update(user)) thenReturn Try((2 / 0))
      val service = new UserService(mockRepo)
      val result: UserServiceResult = service.update(user)
      result.success mustBe false
    }
    "UserService#delete" should {
      "return return successful with user's id on update" in {
        val UPDATE_USER_ID:Int = 5
        val user: User = User( "test@test", "test", "last", "email@test", Some("password"), false, UPDATE_USER_ID)
        val mockRepo = mock[UserRepository]
        when(mockRepo.update(user)) thenReturn Try(UPDATE_USER_ID)
        val service = new UserService(mockRepo)
        val result: UserServiceResult = service.update(user)

        result.success mustBe true
        result.data.asInstanceOf[Int] mustBe UPDATE_USER_ID
      }
      "return unsuccessful when an exception occurs" in {

        val NEW_USER_ID = 22
        val mockRepo = mock[UserRepository]
        val user: User = User("test@test", "test", "last", "email@test", Some("testing"), false)
        when(mockRepo.update(user)) thenReturn Try((2 / 0))
        val service = new UserService(mockRepo)
        val result: UserServiceResult = service.update(user)
        result.success mustBe false
      }
    }
  }
  "UserService#fetch" should{
    "return list of users when offset and limit are null" in{
      val userList:List[User] = List(
           User("test@test2", "test", "last", "email@test", Some("password"), false),
           User("test@test2", "test", "last", "email@test", Some("password"), false),
           User("test@test2", "test", "last", "email@test", Some("password"), false)
      )

      val mockRepo = mock[UserRepository]
      when(mockRepo.query(null, null, null, null, null)) thenReturn Try(userList)
      val service = new UserService(mockRepo)
      val result: UserServiceResult = service.fetch(null, null, null, null, null)
      result.success mustBe true
      result.data.asInstanceOf[List[User]].size mustBe 3
    }
    "return list of users take into consideration offset and limit" in{
      val userList:List[User] = List(
        User("test@test1", "test", "last", "email@test", Some("password"), false),
        User("test@test2", "test", "last", "email@test", Some("password"), false),
        User("test@test3", "test", "last", "email@test", Some("password"), false),
        User("test@test4", "test", "last", "email@test", Some("password"), false)

      )
      val OFFSET = 2
      val LIMIT = 2
      val mockRepo = mock[UserRepository]
      when(mockRepo.query(Some(OFFSET), Some(LIMIT), null, null, null)) thenReturn Try(userList.drop(OFFSET).take(LIMIT))
      val service = new UserService(mockRepo)
      val result: UserServiceResult = service.fetch(Some(OFFSET), Some(LIMIT), null, null, null)
      result.success mustBe true
      result.data.asInstanceOf[List[User]].size mustBe 2
      result.data.asInstanceOf[List[User]].head.username mustBe "test@test3"
    }
  }
}