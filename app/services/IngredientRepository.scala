package services
import javax.inject.{Inject, Singleton}
import play.api.Logger
import scala.util.Try
import anorm._
import play.api.db.Database

import models.{Ingredient, IngredientUnitType, IngredientUnitPrefix}

/**
  * Service for managing ingredients in database
  * @param db database which contains ingredient table
  */
@Singleton
class IngredientRepository  @Inject()(db: Database) {
  private val logger = Logger(this.getClass())

  //insert query for an ingredient
  val INSERT:String = "INSERT INTO ingredients(item, amount, unit_prefix, unit_type, recipe_id) " +
    " VALUES({item}, {amount}, {unit_prefix}, {unit_type}, {recipe_id})"
  //search query for an ingredient
  val QUERY:String = "select ingredient_id, item, amount, unit_prefix, unit_type, recipe_id" +
    "from ingredients where " +
    "({item) is NULL or item={item}) AND " +
    "({searchTerm} is NULL or item LIKE '%{searchTerm}%') AND " +
    "({ingredient_id} is NULL or ingredient_id={ingredient_id})"
  //update query for an ingredient
  val UPDATE_QUERY:String = "update ingredients set " +
    " item={description}, amount={amount}, unit_prefix={unit_prefix}, unit_type={unit_type} " +
    " where ingredient_id={ingredient_id}"
  //delete query for an ingredient
  val DELETE_QUERY:String = "DELETE ingredients where ingredient_id={ingredient_id}"
  //query for checking if an ingredient exists  in a list of ingredient items
  val CONTAINS_QUERY:String = "SELECT * from ingredients where item in {items}"

  //transform sql row to Ingredient class
  private val IngredientClassParser: RowParser[Ingredient] = (
    SqlParser.long("ingredient_id") ~
      SqlParser.str("item")  ~
      SqlParser.int("amount")  ~
      SqlParser.str("unit_prefix") ~
      SqlParser.str("unit_type") ~
      SqlParser.long("recipe_id")
    ) map {
    case columnvalue1 ~
        columnvalue2 ~
        columnvalue3 ~
        columnvalue4 ~
        columnvalue5 ~
        columnvalue6
        => // etc...
      Ingredient(columnvalue1,
          columnvalue2,
          columnvalue3,
          IngredientUnitPrefix.withName(columnvalue4),
          IngredientUnitType.withName(columnvalue5),
          columnvalue6) // etc...
  }
  //transform sql rows to list of Ingredient classes
  private val allIngredientsParser: ResultSetParser[List[Ingredient]] = IngredientClassParser.*

  /**
    * search for ingredient items which are in list of items
    * @param items  list of ingredient items
    * @return list of ingredients which matched list of items
    */
  def containsQuery(items:List[String]): Try[List[Ingredient]]={
      Try(db.withConnection {
        implicit c => {
          SQL(CONTAINS_QUERY).on(
            "items" -> items
          ).as(allIngredientsParser)
        }
      })
  }

  /**
    * query for ingredients
    * @param ingredientId search for ingredient by unique id
    * @param item  search for ingredient by item name
    * @param searchTerm fuzzy search item name
    * @return list of ingredients based on query
    */
  def query(ingredientId:Option[Long],
            item: Option[String],
            searchTerm: Option[String]
             ): Try[List[Ingredient]]={
    Try(db.withConnection {
      implicit c => {
        SQL(QUERY).on(
          "ingredient_id" -> ingredientId,
          "searchTerm" -> searchTerm,
          "item" -> item
        ).as(allIngredientsParser)
      }
    })
  }

  /**
    * Delete an existing ingredient
    * @param ingredientId unique id of ingredient to delete
    * @return unique id of deleted ingredient
    */
  def delete(ingredientId: Long): Try[Int] = {
    Try(db.withConnection {
      implicit c => {
        SQL(DELETE_QUERY)
          .on("id" -> ingredientId).executeUpdate()
      }
    })
  }

  /**
    * create a new ingredient
    * @param ingredient ingredient to be created
    * @param recipeId recipe id for ingredient
    * @return unique id of ingredient
    */
  def create(ingredient:Ingredient, recipeId:Long): Try[Option[Long]] = {
    Try(db.withConnection {
      implicit c => {
        SQL(INSERT)
          .on("item" -> ingredient.item,
               "amount" -> ingredient.amount,
               "unit_prefix" -> ingredient.unit_prefix.toString,
               "unit_type" -> ingredient.unit_prefix.toString,
              "recipe_id" -> recipeId).executeInsert()
      }
    })
  }

  /**
    * update an existing ingredient
    * @param ingredient ingredient to be updated
    * @return unique id of updated ingredient
    */
  def update(ingredient:Ingredient): Try[Int] = {
    Try(db.withConnection {
      implicit c => {
        SQL(UPDATE_QUERY)
          .on("ingredient_id" -> ingredient.ingredient_id,
              "item" -> ingredient.item,
              "amount" -> ingredient.amount,
              "unit_prefix" -> ingredient.unit_prefix.toString,
              "unit_type" -> ingredient.unit_type.toString).executeUpdate()
      }
    })
  }

}
