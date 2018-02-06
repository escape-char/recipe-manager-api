package services
import scala.util.{Failure, Success, Try}
import javax.inject.{Inject}
import play.api.Logger
import models.{Category, CategoryBrief}

/**
  * Reponse class for CategoryService
  * @param success whether the service returned successful
  * @param data  any data the service returned
  */
case class CategoryServiceResult(success:Boolean, data: Any)

/**
  * Service for management of categories
  * @param repo  database repository for category operations
  */
class CategoryService @Inject()(val repo: CategoryRepository) {
  private val logger = Logger(this.getClass())

  /**
    * query for categories
    * @param offset amount of categories to skip from the beginning
    * @param limit  limit amount of categories returned
    * @param searchName  fuzzy search for category name
    * @param name  search for exact category name
    * @param categoryId search for exact category id
    * @param userId search for categories which belong to this user id
    * @return list of categories
    */
  def fetch(offset: Option[Int]=Some(0),
            limit: Option[Int] = None,
            searchName:Option[String] = None,
            name: Option[String] = None,
            categoryId: Option[Long] = None,
            userId:Option[Long] = None):CategoryServiceResult = {
    val result:Try[List[Category]] = repo.queryByUserId(
      offset,
      limit,
      searchName,
      name,
      categoryId,
      userId)
    result match{
      case Success(v) => CategoryServiceResult(true, v)
      case Failure(e) => CategoryServiceResult(false, e.toString)
    }
  }

  /**
    * create a new category for a user
    * @param category new category to be created
    * @param userId unique id of user which this category belongs to
    * @return unique id of newly created category
    */
  def create(category:CategoryBrief, userId:Long): CategoryServiceResult = {
    assert(userId !=0)
    val result2 = repo.insert(category, userId)
    result2 match{
      case Success(v) =>{
        return CategoryServiceResult(true, v)
      }
      case Failure(e) => CategoryServiceResult(false, e.toString())
    }
  }

  /**
    * updates an existing category for a user
    * @param category  category to update
    * @param userId unique id for user
    * @return unique id of category which was updated
    */
  def update(category: CategoryBrief, userId:Long): CategoryServiceResult = {
    assert(userId !=0)
    val result: Try[Int] = repo.update(category, userId)
    result match{
      case Success(v) => CategoryServiceResult(true, v)
      case Failure(e) => CategoryServiceResult(false, e)
    }
  }

  /**
    * deletes an existing category for a user
    * @param categoryId unique id of category to be deleted
    * @param userId user id of the user
    * @return unique id of category which was deleted
    */
  def delete(categoryId: Long, userId:Long): CategoryServiceResult = {
    assert(userId !=0)
    val result: Try[Int] = repo.delete(categoryId, userId)
    result match{
      case Success(v) => CategoryServiceResult(true, v)
      case Failure(e) =>  CategoryServiceResult(false, e.toString)
    }
  }

}
