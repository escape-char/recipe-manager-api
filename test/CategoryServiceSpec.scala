/**
  * Created by jordan on 8/15/17.
  */
import org.scalatestplus.play._
import org.mockito.Mockito._
import scala.util.Try
import org.scalatest.mock.MockitoSugar
import models.{Category, CategoryBrief}
import services.{CategoryService, CategoryRepository, CategoryServiceResult}

/**
  * Spec for testing the CategoryService
  */
class CategoryServiceSpec extends PlaySpec with MockitoSugar {
  "CategoryService#fetch" should {
    "return full list of categories when offset and limit are null" in {
      val USER_ID:Long = 22
      val catList:List[Category] = List(
        Category(1, "breakfast", false, 3, 1),
        Category(2, "lunch", false, 3, 1),
        Category(3, "lunch", false, 3, 3)
      )
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.queryByUserId(null, null, null, null, null,Some(USER_ID))) thenReturn Try(catList)
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.fetch(null,null,null,null,null, Some(USER_ID))
      result.success mustBe true
      result.data.asInstanceOf[List[Category]].length mustBe 3

    }
    "should take into consideration offset and limit" in{
      val USER_ID:Long = 22
      val catList:List[Category] = List(
        Category(1, "breakfast", false, 3, 1),
        Category(2, "lunch", false, 3, 1),
        Category(3, "lunch", false, 3, 3)
      )
      val OFFSET = 1
      val LIMIT = 2
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.queryByUserId(Some(OFFSET), Some(LIMIT), null, null, null, Some(USER_ID))) thenReturn Try(catList.drop(OFFSET).take(LIMIT))
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.fetch(Some(OFFSET), Some(LIMIT), null, null, null, Some(USER_ID))
      result.success mustBe true
      result.data.asInstanceOf[List[Category]].size mustBe 2
      result.data.asInstanceOf[List[Category]].head.id mustBe 2
    }
    "should return unsuccessful in an exception occurs" in {
      val NEW_CATEGORY_ID:Long=2
      val cat: CategoryBrief = CategoryBrief(0, "testing", false)
      val USER_ID:Long = 22
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.insert(cat, USER_ID)) thenReturn Try(Some((2 / 0).toLong))
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.create(cat, USER_ID)
        result.success mustBe false
      }
    }
  "CategoryService#create" should {
    "return newly created category's id" in {
      val cat: CategoryBrief = CategoryBrief(0, "testing", false)
      val USER_ID:Long = 22
      val NEW_CATEGORY_ID:Long=2
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.insert(cat, USER_ID)) thenReturn Try(Some(NEW_CATEGORY_ID))
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.create(cat, USER_ID)

      result.success mustBe true
      result.data.asInstanceOf[Option[Long]].get mustBe NEW_CATEGORY_ID
    }
    "return unsuccessful in an exception occurs" in {
      val NEW_CATEGORY_ID:Long=2
      val cat: CategoryBrief = CategoryBrief(0, "testing", false)
      val USER_ID:Long = 22
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.insert(cat, USER_ID)) thenReturn Try(Some((2 / 0).toLong))
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.create(cat, USER_ID)
      result.success mustBe false
    }
  }
  "CategoryService#update" should {
    "return updated id on success" in {
      val UPDATE_CATEGORY_ID:Long=2

      val cat: CategoryBrief = CategoryBrief(UPDATE_CATEGORY_ID, "testing", false)
      val USER_ID:Long = 22
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.update(cat, USER_ID)) thenReturn Try(UPDATE_CATEGORY_ID.toInt)
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.update(cat, USER_ID)

      result.success mustBe true
      result.data.asInstanceOf[Int] mustBe UPDATE_CATEGORY_ID.toInt
    }
    "return unsuccessful in an exception occurs" in {
      val UPDATE_CATEGORY_ID:Long=2
      val cat: CategoryBrief = CategoryBrief(UPDATE_CATEGORY_ID, "testing", false)
      val USER_ID:Long = 22
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.update(cat, USER_ID)) thenReturn Try((2 / 0))
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.update(cat, USER_ID)
      result.success mustBe false
    }
    }
  "CategoryService#delete" should {
    "return deleted id on success" in {
      val DELETE_CATEGORY_ID:Long=2
      val USER_ID:Long = 22
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.delete(DELETE_CATEGORY_ID, USER_ID)) thenReturn Try(DELETE_CATEGORY_ID.toInt)
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.delete(DELETE_CATEGORY_ID, USER_ID)

      result.success mustBe true
      result.data.asInstanceOf[Int] mustBe DELETE_CATEGORY_ID.toInt
    }
    "return unsuccessful in an exception occurs" in {
      val DELETE_CATEGORY_ID:Long=2
      val USER_ID:Long = 22
      val mockRepo = mock[CategoryRepository]
      when(mockRepo.delete(DELETE_CATEGORY_ID, USER_ID)) thenReturn Try((2 / 0))
      val service = new CategoryService(mockRepo)
      val result: CategoryServiceResult = service.delete(DELETE_CATEGORY_ID, USER_ID)
      result.success mustBe false
    }
  }
}