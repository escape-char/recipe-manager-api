package models
import play.api.libs.json._
import services.{EnumUtils}

/**
  * Enumeration for Recipe Difficulties
  */
object RecipeDifficulty extends Enumeration {
  type RecipeDifficulty = Value
  val EASY = Value("Easy")
  val MEDIUM = Value("Medium")
  val DIFFICULT = Value("Difficult")

  implicit val enumReads: Reads[RecipeDifficulty] = EnumUtils.enumJsonReads(RecipeDifficulty)
  implicit def enumWrites: Writes[RecipeDifficulty] = EnumUtils.enumJsonWrites
}

/**
  * Class for handling new recipe forms
  * @param title title of recipe
  * @param description description of recipe
  * @param source source of where recipe came from
  * @param url url for source recipe
  * @param difficulty difficult of recipe
  * @param servings number of servings recipe contains
  * @param notes  additional notes about the recipe
  * @param prepTime  total amount of preparation time
  * @param cookTime  total amount of cook time
  * @param image   image of recipe
  * @param ingredients  list of ingredients for recipe
  * @param directions  list of directions for recipe
  * @param categories  list of categories for recipe
  */
case class NewRecipeForm (
                    title:String="",
                    description:String="",
                    source:String="",
                    url:String="",
                    difficulty:RecipeDifficulty.RecipeDifficulty = RecipeDifficulty.EASY,
                    servings:Int = 0,
                    notes:String = "",
                    prepTime:String="",
                    cookTime:String="",
                    image:String="",
                    ingredients:List[Ingredient] = null,
                    directions:List[Direction] = null,
                    categories:List[RecipeCategory] = null
                  )

/**
  * Class representing an existing recipe
  * @param id unique id of recipe
  * @param title title of recipe
  * @param description description of recipe
  * @param source source of where recipe came from
  * @param url url for source recipe
  * @param difficulty difficult of recipe
  * @param servings number of servings recipe contains
  * @param notes  additional notes about the recipe
  * @param prepTime  total amount of preparation time
  * @param cookTime  total amount of cook time
  * @param image   image of recipe
  * @param createDateTime datetime of recipe creation
  * @param createdBy unique id  or username of user who created this recipe
  * @param ingredients  list of ingredients for recipe
  * @param directions  list of directions for recipe
  * @param categories  list of categories for recipe
  */
case class Recipe (
      id:Long=0,
      title:String="",
      description:String="",
      source:String="",
      url:String="",
      difficulty:RecipeDifficulty.RecipeDifficulty = RecipeDifficulty.EASY,
      servings:Int=0,
      notes:String="",
      prepTime:String="",
      cookTime:String="",
      image:String="",
      createDateTime:String="" ,
      createdBy:Any=0,
      ingredients:List[Ingredient] = null,
      directions:List[Direction] = null,
      categories:List[RecipeCategory] = null,
      total:Int=0
)

/**
  * Object for handling JSON writes of Recipe
  */
object Recipe{
  implicit val recipeWrites = new Writes[Recipe] {
    def writes(recipe: Recipe) = Json.obj(
          "id" -> recipe.id,
           "title" -> recipe.title,
           "description" -> recipe.description,
           "source" -> recipe.source,
           "url" -> recipe.url,
           "difficulty" -> recipe.difficulty,
           "servings" -> recipe.servings,
           "notes" -> recipe.notes,
           "prepTime" -> recipe.prepTime,
            "cookTime" -> recipe.cookTime,
           "image" -> recipe.image,
           "createDateTime" -> recipe.createDateTime,
           "createdBy" -> recipe.createdBy.toString(),
            "directions" -> recipe.directions,
          "ingredients" -> recipe.ingredients,
          "categories" -> recipe.categories,
          "total" -> recipe.total
    )
  }
}
