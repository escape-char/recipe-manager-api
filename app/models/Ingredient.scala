package models
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import services.EnumUtils

/**
  * Enumeration for supported ingredient unit types
  */
object IngredientUnitType extends Enumeration {
  type IngredientUnitType = Value

  val WEIGHT= Value("Weight")
  val VOLUME= Value("Volume")
  val LENGTH = Value("Length")
  val QUANTITY = Value("Quantity")
  implicit val enumReads: Reads[IngredientUnitType] = EnumUtils.enumJsonReads(IngredientUnitType)
  implicit def enumWrites: Writes[IngredientUnitType] = EnumUtils.enumJsonWrites
}

/**
  * Enumeration for supported ingredient unit prefixes of ingredient unit types
  */
object IngredientUnitPrefix extends Enumeration {
  type IngredientUnitPrefix = Value
  //Weight
  val POUND= Value("Pound (lb)")
  val OUNCE= Value("Ounce (oz)")
  val MILLIGRAM = Value("Milligram (mg)")
  val GRAM = Value("Gram (g)")
  val KILOGRAM = Value("Kilogram (kg)")

  //Volume
  val TEASPOON= Value("Teaspoon (t or tsp)")
  val TABLESPOON= Value("Tablespoon (T,tbl., tbs, or tbsp)")
  val FLUID_OUNCE= Value("Fluid Ounce (fl oz)")
  val GILL = Value("Gill (1/2 cup)")
  val CUP = Value("Cup (c)")
  val PINT = Value("Pint (p, pt, or fl pt)")
  val QUART = Value("Quart (q, qt, fl qt)")
  val GALLON = Value("Gallon (g or gal)")
  val MILLILETER = Value("Millileter (ml, cc, mL)")
  val LITER = Value("Liter (l, L)")
  val DECILITER = Value("Decileter (dL)")

  //Length
  val MILLIMETER = Value("Millimeter (lb)")
  val CENTIMETER = Value("Centimeter (cm)")
  val METER = Value("Meter (m)")
  val INCH = Value("Inch (in)")

  implicit val enumReads: Reads[IngredientUnitPrefix] = EnumUtils.enumJsonReads(IngredientUnitPrefix)
  implicit def enumWrites: Writes[IngredientUnitPrefix] = EnumUtils.enumJsonWrites

}

/**
  * represents an ingredient for a recipe
  * @param ingredient_id unique id of the ingredient
  * @param item  name of the ingredient
  * @param amount  amount of the ingredient
  * @param unit_prefix unit prefix of the ingredient
  * @param unit_type  unit type of the ingredient
  * @param recipe_id associated recipe which the ingredient belongs to
  */
case class Ingredient(ingredient_id:Long=0,
                      item:String = "",
                      amount:Int=0,
                      unit_prefix:IngredientUnitPrefix.IngredientUnitPrefix = null,
                      unit_type: IngredientUnitType.IngredientUnitType = IngredientUnitType.QUANTITY,
                      recipe_id:Long=0)


/**
  * Object for handling JSON reads of an Ingredient
  */
object Ingredient{
  implicit val jsonFormat: Format[Ingredient] = Json.format[Ingredient]
  implicit val ingredientReads: Reads[Ingredient] = (
    (__ \ "ingredient_id").read[Long] and
      (__ \ "item").read[String] and
      (__ \ "amount").read[Int] and
      (__ \ "unit_prefix").read[IngredientUnitPrefix.IngredientUnitPrefix] and
      (__  \ "unit_type").read[IngredientUnitType.IngredientUnitType] and
      (__ \ "recipe_id").read[Long]
    )(Ingredient.apply _)

  implicit val readJsonList: Reads[List[Ingredient]] = Reads.list[Ingredient](ingredientReads)

}
