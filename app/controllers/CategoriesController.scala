package controllers
import javax.inject.{Inject}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Controller
import pdi.jwt.JwtSession._
import play.api.Logger
import play.api.libs.json._
import models.{Category, CategoryBrief, User}
import services.{CategoryService, CategoryServiceResult, Utils}

/**
  *  case class for adding new categories
  * @param name name of the new category
  */
case class NewCategory(name:String="")

/**
  * Controller for handling mananging of recipe categories
  * @param categoryService - service for managing category data
  */
class CategoriesController  @Inject()(categoryService:CategoryService) extends Controller with Secured{
  private val logger = Logger(this.getClass())
  private val CAT_NOT_FOUND:String = "Category does not exist."

  //form for validating new category JSON data
  private val newCategoryForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(NewCategory.apply)(NewCategory.unapply)
  )
  //form for handling update category JSON data
  private val updateCategoryForm = Form(
    mapping(
      "id" -> longNumber,
      "name" -> nonEmptyText,
      "is_default" -> boolean
    )(CategoryBrief.apply)(CategoryBrief.unapply)
  )
  logger.debug("CategoriesController()")

  /**
    * handles fetching of user's categories based on their query
    * @param offset - number of categories to skip starting at the first
    * @param limit  - number of categories to limit the response to
    * @param searchName - fuzzy search string for a category name
    * @param name  - search string for exact category name
    * @param categoryId - search for exact category id
    * @return list of categories based on query
    */
  def fetchMine(limit:Option[Int],
            offset:Option[Int],
            searchName:Option[String],
            name:Option[String],
            categoryId:Option[Long]
       ) = Authenticated { implicit request =>

    val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
    val result: CategoryServiceResult = this.categoryService.fetch(offset, limit, searchName, name, categoryId, userId)
    result.success match {
      case true => {
        val categories: List[Category] = result.data.asInstanceOf[List[Category]]
        implicit val catWrites = Json.writes[Category]
        Ok(Json.toJson(Json.obj("success" -> result.success, "categories" ->  categories)))
      }
      case false => {
        val message: String = result.data.asInstanceOf[String]
        InternalServerError(Json.toJson(Json.obj("success" -> result.success, "error" -> message)))
      }
    }
  }

  /**
    * Handles updating of a category
    *
    * Category data goes in the JSON body
    *
    * @param id - id of category to be updated
    * @return 200 (OK) when successful
    */
  def updateMine(id:Long) = Authenticated(parse.json) { implicit request =>
    val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
    updateCategoryForm.bindFromRequest.fold(
      formWithErrors =>{
        BadRequest(Json.toJson(Json.obj("success"->false,
          "error" ->Utils.flattenFormErrors(formWithErrors.errors))))
      },
      cat => {
        val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
        val result: CategoryServiceResult = this.categoryService.fetch(categoryId=Some(cat.id), userId=userId)
        result.success match {
          case true => {
            val categories: List[Category] = result.data.asInstanceOf[List[Category]]
            if(categories.isEmpty){
              NotFound(Json.toJson(Json.obj("success" -> result.success, "error" -> this.CAT_NOT_FOUND)))

            }else{
              val result2: CategoryServiceResult = this.categoryService.update(cat, userId.getOrElse(0))
              result2.success match {
                case true => {
                  Ok(Json.toJson(Json.obj("success" -> result.success)))
                }
                case false => {
                  val message: String = result.data.asInstanceOf[String]
                  InternalServerError(Json.toJson(Json.obj("success" -> result.success, "error" -> message)))
                }
              }
            }
          }
          case false => {
            val message: String = result.data.asInstanceOf[String]
            InternalServerError(Json.toJson(Json.obj("success" -> result.success, "error" -> message)))
          }
        }
      }
    )
  }

  /**
    * Handles deletion of a category
    * @param id - id of category to delete
    * @return 200 (OK) if successful
    */
  def deleteMine(id:Long) = Authenticated { implicit request =>
    val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
    val result2: CategoryServiceResult = this.categoryService.delete(id, userId.getOrElse(0))
    result2.success match {
      case true => {
        Ok(Json.toJson(Json.obj("success" -> result2.success)))
      }
      case false => {
        val message: String = result2.data.asInstanceOf[String]
        InternalServerError(Json.toJson(Json.obj("success" -> result2.success, "error" -> message)))
      }
    }
  }

  /**
    * Handles creation of a category
    *
    * Category data for creation goes in JSON body
    *
    * @return 201 (Created) if successful with new category populated with unique id in the
    *         JSON body
    */
  def create() = Authenticated(parse.json) { implicit request =>
    logger.debug("create()")
    newCategoryForm.bindFromRequest.fold(
      formWithErrors =>{
        BadRequest(Json.toJson(Json.obj("success"->false,
          "error" ->Utils.flattenFormErrors(formWithErrors.errors))))
      },
      cat => {
        val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
        val category:CategoryBrief = CategoryBrief(
          name=cat.name
        )
        val result:CategoryServiceResult = categoryService.create(category, userId.get)
        result.success match {
          case true => {
            val newCat:Long= result.data.asInstanceOf[Option[Long]].get
            logger.debug("successfully created category")
            Ok(Json.toJson(Json.obj("success"->true, "category"->newCat)))
          }
          case false =>{
            val message:String = result.data.asInstanceOf[String]
            InternalServerError(Json.toJson(Json.obj("success"->false, "error" -> message)))
          }

        }

      }
    )
  }

}
