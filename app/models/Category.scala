package models

import play.api.libs.json.{Format, Writes, Reads, JsValue, Json}

/**
  * schema for detailed Category data
  * @param id  number for category's unique id
  * @param name - name of category
  * @param isDefault - whether category is a default category populated automatically on user creation
  * @param total  - total amount of categories returned in query
  * @param totalRecipes - total amount of recipes associated with the category
  */
case class Category(id:Long=0, name:String="", isDefault:Boolean=false, total:Int=0, totalRecipes:Int=0)

/**
  * schema for brief category data
  * @param id unique id of category
  * @param name name of catagory
  * @param isDefault whether category is a default category populated automatically on user creation
  */
case class CategoryBrief(id:Long=0, name:String="", isDefault:Boolean=false)

/**
  * schema for a category inside a recipe
  * @param category_id unique id of category
  * @param name name of catagory
  * @param is_default whether category is a default category populated automatically on user creation
  */
case class RecipeCategory(category_id:Long=0, name:String="", is_default:Boolean=false)

/**
  * Object for handling JSON write of Category
  */
object Category{
  implicit val categoryWrites = new Writes[Category] {
    def writes(c: Category) = Json.obj(
      "name" -> c.name,
      "id" -> c.id,
      "isDefault" -> c.isDefault,
      "total" -> c.total,
      "totalRecipes" -> c.totalRecipes
    )
  }
}

/**
  * Object for handling JSON reads of CategoryBrief
  */
object CategoryBrief {
  implicit val jsonFormat: Format[CategoryBrief] = Json.format[CategoryBrief]

  def readJsonArray(json: String): List[CategoryBrief] = {
    val js: JsValue = Json.parse(json)
    (js).as[List[CategoryBrief]]
  }
}
/**
  * Object for handling JSON reads of RecipeCategory
  */
object RecipeCategory {
  implicit val jsonFormat: Format[RecipeCategory] = Json.format[RecipeCategory]

  def readJsonArray(json: String): List[RecipeCategory] = {
    val js: JsValue = Json.parse(json)
    (js).as[List[RecipeCategory]]
  }
}