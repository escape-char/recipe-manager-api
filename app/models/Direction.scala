package models
import play.api.libs.json._

/**
  * represents a recipe's direction
  * @param direction_id unique id for direction
  * @param description description of direction
  * @param step  step number of direction
  * @param recipe_id associated recipe id of direction
  */
case class Direction(direction_id:Long=0, description:String="", step:Int=0, recipe_id:Long=0);

/**
  * Object for handling JSON reads of direction
  */
object Direction{
    implicit val jsonFormat: Format[Direction] = Json.format[Direction]
    def readJsonArray(json:String):List[Direction]= {
      val js:JsValue = Json.parse(json)
      (js).as[List[Direction]]
    }
}
