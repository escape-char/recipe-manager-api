package controllers
import javax.inject.{Inject}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Controller
import play.api.mvc._
import pdi.jwt.JwtSession._
import play.api.Logger
import play.api.libs.json._
import models.{
        NewRecipeForm,
        RecipeDifficulty,
        Recipe,
        User,
        IngredientUnitType,
        IngredientUnitPrefix,
        Ingredient,
        Direction,
        RecipeCategory }
import services.{RecipeService,
                RecipeServiceResult,
                EnumUtils,
                Utils}

/**
  * Controller for handling management of recipes
  *
  * @param recipeService - service for handling recipe operations
  */
class RecipesController  @Inject()(recipeService:RecipeService) extends Controller with Secured{
  private val logger = Logger(this.getClass())
  logger.debug("RecipeController()")

  //form for validating recipe JSON data
  private val createRecipeForm = Form(
    mapping(
      "title" ->(text.verifying("Title is required.", {!_.isEmpty})
        verifying("Title must be less than 100 characters", {_.length < 50})
        ),
      "description" -> (text.verifying("Description is required.", {!_.isEmpty})
        verifying("Description must be less than 500 characters", {_.length < 500})
        ),
      "source" -> text,
      "url" ->text,
      "difficulty" -> EnumUtils.enumForm(RecipeDifficulty),
      "servings" -> number,
      "notes" -> text,
      "prepTime" -> nonEmptyText,
      "cookTime" -> nonEmptyText,
      "image" -> text,
      "ingredients"-> list(mapping(
        "ingredient_id" ->longNumber,
        "item" -> nonEmptyText,
        "amount" -> number,
        "unit_prefix" -> EnumUtils.enumForm(IngredientUnitPrefix),
        "unit_type" -> EnumUtils.enumForm(IngredientUnitType),
        "recipe_id" -> longNumber
      )(Ingredient.apply)(Ingredient.unapply)),
      "directions"-> list(mapping(
        "direction_id" ->longNumber,
        "description" -> nonEmptyText,
        "step" -> number,
        "recipe_id" -> longNumber
      )(Direction.apply)(Direction.unapply)),
      "categories"-> list(mapping(
        "category_id" ->longNumber,
        "name" -> nonEmptyText,
        "is_default" -> boolean
      )(RecipeCategory.apply)(RecipeCategory.unapply))
    )(NewRecipeForm.apply)(NewRecipeForm.unapply)
  )

  /**
    * handles querying for a user's recipes
    * @param offset - number for amount to skip from beginning
    * @param limit  - number for limiting amount returned
    * @param searchTerm  - string for fuzzy searching recipe title and description
    * @param title  -  string search for exact title
    * @param recipeId - number search for exact recipe id
    * @param categoryId - number for searching by category id
    * @return list of recipes based on query
    */
  def fetchMine(offset:Option[Int],
                limit:Option[Int],
                searchTerm:Option[String],
                title:Option[String],
                recipeId:Option[Long],
                categoryId:Option[Long]
               ) = Authenticated { implicit request =>

    val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
    val result: RecipeServiceResult = this.recipeService.fetch(offset,
                                          limit,
                                          searchTerm,
                                          title,
                                          userId,
                                          recipeId,
                                          categoryId)
    result.success match {
      case true => {
        val recipes: List[Recipe] = result.data.asInstanceOf[List[Recipe]]
        Ok(Json.toJson(Json.obj("success" -> result.success, "recipes" -> recipes)))
      }
      case false => {
        val message: String = result.data.asInstanceOf[String]
        InternalServerError(Json.toJson(Json.obj("success" -> result.success, "error" -> message)))
      }
    }
  }

  /**
    * handles creation of a recipe for a user
    *
    * recipe data goes in JSON body of the request
    *
    * @return 201 (Created) with the recipe's unique id in the JSON body
    */
  def create() = Action(parse.json) { implicit request =>
    logger.debug("create()")
    createRecipeForm.bindFromRequest.fold(
      formWithErrors =>{
        BadRequest(Json.toJson(Json.obj("success"->false,
          "error" ->Utils.flattenFormErrors(formWithErrors.errors))))
      },
      rec => {
        val username:Option[String] = Some(request.jwtSession.getAs[User]("user").get.username)
        val userId:Option[Long] = Some(request.jwtSession.getAs[User]("user").get.id)
        val recipe:Recipe = Recipe(
          title=rec.title,
          description=rec.description,
          source=rec.source,
          url=rec.url,
          difficulty = rec.difficulty,
          servings = rec.servings,
          notes= rec.notes,
          prepTime = rec.prepTime,
          cookTime = rec.cookTime,
          image=rec.image,
          ingredients=rec.ingredients,
          directions=rec.directions,
          createdBy=userId.get,
          categories = rec.categories
        )
        val result:RecipeServiceResult = recipeService.create(recipe, userId.get)
        result.success match {
          case true => {
            val recipeId: Some[Long] = result.data.asInstanceOf[Some[Long]]
            Ok(Json.toJson(Json.obj("success"->true, "recipeId"-> recipeId)))
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