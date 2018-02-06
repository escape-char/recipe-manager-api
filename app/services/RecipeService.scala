package services
import scala.util.{Failure, Success, Try}
import javax.inject.Inject
import play.api.Logger
import models.{Recipe}

/**
  * Response class for RecipeService
  * @param success whether the service call was successful
  * @param data returned data associated with service call
  */
case class RecipeServiceResult(success:Boolean, data: Any)

/**
  * Service for managing recipes
  * @param recRepo repository for handling recipes in database
  */
class RecipeService @Inject()(val recRepo: RecipeRepository) {
  private val logger = Logger(this.getClass())

  /**
    * query for recipes
    * @param offset amount of recipes to skip from beginning
    * @param limit limit the amount of recipes returned
    * @param searchTerm fuzzy search string for recipe title or description
    * @param title  search string for exact recipe title
    * @param userId search number for recipes which belong to this user id
    * @param recipeId search number for unique recipe id
    * @param categoryId search number for unique category id
    * @return list of recipes based on query
    */
  def fetch(offset:Option[Int],
            limit:Option[Int],
            searchTerm:Option[String],
            title:Option[String],
            userId:Option[Long],
            recipeId:Option[Long],
            categoryId:Option[Long]
           ): RecipeServiceResult = {
    val result:Try[List[Recipe]] = recRepo.query(
      offset,
      limit,
      searchTerm,
      title,
      userId,
      recipeId,
      categoryId)
    result match{
      case Success(v) => RecipeServiceResult(true, v)
      case Failure(e) => RecipeServiceResult(false, e.toString)
    }
  }

  /**
    * Create a new recipe for a user
    * @param recipe new recipe to create
    * @param userId unique id of which this new recipe belongs to
    * @return unique id of new recipe
    */
  def create(recipe:Recipe, userId:Long): RecipeServiceResult ={
    assert(recipe.id == 0 || recipe.id == null)
    val recResult = recRepo.upsert(recipe, userId)
    recResult match{
      case Success(v) => RecipeServiceResult(true, v)
      case Failure(e) => RecipeServiceResult(false, e.toString)
    }
  }

  /**
    * Update an existing recipe
    * @param recipe recipe to update
    * @param userId unique id of user performing this update
    * @return unique id of updated recipe
    */
  def update(recipe:Recipe, userId:Long): RecipeServiceResult ={
    assert(recipe.id != 0 && recipe.id != null)
    val recResult = recRepo.upsert(recipe, userId)
    recResult match{
      case Success(v) => RecipeServiceResult(true, v)
      case Failure(e) => RecipeServiceResult(false, e.toString)
    }
  }

}
