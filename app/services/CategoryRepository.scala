package services
import javax.inject.{Inject, Singleton}
import play.api.Logger
import scala.util.Try
import anorm._
import play.api.db.Database
import models.{Category, CategoryBrief}


/**
  * Handles management of categories in database
  * @param db database which categories exist in
  */
@Singleton
class CategoryRepository  @Inject()(db: Database) {
  private val logger = Logger(this.getClass())

  //update query for category by id
  val UPDATE_CATEGORY_BY_ID:String = "UPDATE categories SET name={name}, is_default={is_default}" +
    " where category_id={id} and user_id={user_id}"

  //delete query for category by id
  val DELETE_CATEGORY_BY_ID:String = "DELETE FROM categories where category_id={id} and user_id={user_id}"

  //query for all user's category containing total recipes associated with each category and
  //the total amount of categories in the resulting query
  val QUERY_BY_USER:String = "SELECT categories.category_id, categories.name, categories.is_default," +
    " COUNT(category_recipe.recipe_id) AS total_recipes, COUNT(*) OVER() AS total FROM categories " +
    " LEFT OUTER JOIN category_recipe ON category_recipe.category_id=categories.category_id" +
    " WHERE categories.user_id={user_id} " +
    " AND ({search_name} IS NULL OR categories.name LIKE '%{search_name}%')" +
     " AND ({category_name} IS NULL OR categories.name={category_name})" +
     " AND ({category_id} IS NULL OR categories.category_id={category_id})" +
     "  GROUP BY categories.category_id ORDER BY categories.name offset {offset} limit {limit}"


  //query for inserting a new category
  val INSERT_CATEGORY:String = "INSERT INTO categories(name, is_default, user_id) VALUES({name},{is_default}, {user_id})"

  //query for checking if any categories are in a list of category names
  val QUERY_CONTAINS:String = "SELECT * from categories where name in ({names}) and categories.user_id={user_id}"


  //handles transforming a SQL row of a category into a Category class
  private val CategoryClassParser: RowParser[Category] = (
    SqlParser.long("category_id") ~
      SqlParser.str("name") ~
      SqlParser.bool("is_default") ~
      SqlParser.int("total")~
      SqlParser.int("total_recipes")
    ) map {
    case columnvalue1 ~ columnvalue2 ~ columnvalue3 ~ columnvalue4 ~ columnvalue5  => // etc...
      Category(columnvalue1, columnvalue2, columnvalue3, columnvalue4, columnvalue5) // etc...
  }
  //handles transforming a SQL row of a category into a CategoryBrief class
  private val CategoryBriefClassParser: RowParser[CategoryBrief] = (
    SqlParser.long("category_id") ~
      SqlParser.str("name") ~
      SqlParser.bool("is_default")
    ) map {
    case columnvalue1 ~ columnvalue2 ~ columnvalue3  => // etc...
      CategoryBrief(columnvalue1, columnvalue2, columnvalue3) // etc...
  }

  //transform sql rows into a list of Category
  private val allCategoriesParser: ResultSetParser[List[Category]] = CategoryClassParser.*
  //transform sql rows into a list of CategoryBrief
  private val allCategoriesBriefParser: ResultSetParser[List[CategoryBrief]] = CategoryBriefClassParser.*

  /**
    * returns categories that match a list of provided category names
    * @param names list of category names to see if other categories have it
    * @param userId user's unique id
    * @return list of categories which match the provided list of names
    */
  def containsQuery(names:List[String], userId:Long): Try[List[CategoryBrief]]  ={
    logger.debug("containsQuery")
    Try(db.withConnection {
      implicit c => {
        SQL(QUERY_CONTAINS).on(
          "names" -> names,
          "user_id" -> userId
        ).as(allCategoriesBriefParser)
      }
    })
  }

  /**
    * queries for a  list of categories for a user
    * @param offset amount of categories to skip from the beginning
    * @param limit  limit the total amount of categories returned
    * @param searchName string fuzzy search category name
    * @param name string search for exact category name
    * @param categoryId number search for exact category id
    * @param userId  user's unique id in which categories belong to
    * @return list of categories
    */
  def queryByUserId(offset:Option[Int],
            limit:Option[Int],
            searchName:Option[String],
            name:Option[String],
            categoryId:Option[Long],
            userId:Option[Long]
     ): Try[List[Category]] = {
    logger.debug("query()")
    val limit2:Int = limit.getOrElse(0)
    //filter out any parameters which are null or zero . except for
    // offset for except which can default to zero
    val params:Seq[NamedParameter] = Seq(
      NamedParameter("category_name", ParameterValue.toParameterValue[Option[String]](name)),
      NamedParameter("category_id", ParameterValue.toParameterValue[Option[Long]](categoryId)),
      NamedParameter("limit", ParameterValue.toParameterValue[Option[Int]](limit)),
      NamedParameter("user_id", ParameterValue.toParameterValue[Option[Long]](userId)),
      NamedParameter("search_name", ParameterValue.toParameterValue[Option[String]](searchName)),
      NamedParameter("offset", ParameterValue.toParameterValue[Int](offset.getOrElse(0))))
    logger.debug("Params length: " + params.length);

    Try(db.withConnection {
      implicit c => {
        SQL(QUERY_BY_USER).on(params:_*).as(allCategoriesParser)
      }
    })
  }

  /**
    * Insert a new category into table
    * @param category category to insert
    * @param userId user id of which this category will belong to
    * @return inserted category's unique id
    */
  def insert(category: CategoryBrief, userId:Long): Try[Option[Long]] = {
    logger.debug("insert()")
    Try(db.withConnection {
      implicit c => {
        SQL(INSERT_CATEGORY)
          .on("name" -> category.name,
            "is_default" -> category.isDefault,
            "user_id" ->userId).executeInsert()
      }
    })
  }

  /**
    * update an existing category
    * @param category category to update
    * @param userId userId which the category belongs to
    * @return category's unique id which was updated
    */
  def update(category: CategoryBrief, userId:Long): Try[Int] = {
    val query:SimpleSql[Row] = SQL(UPDATE_CATEGORY_BY_ID).on(
      "name" -> category.name,
      "is_default" -> category.isDefault,
      "id" -> category.id,
      "user_id" -> userId)
    Try(db.withConnection {
      implicit c => {
        query.executeUpdate()
      }
    })
  }

  /**
    * Delete an existing category
    * @param categoryId unique id of category to delete
    * @param userId  userId which the category belongs to
    * @return category's unique id which was deleted
    */
  def delete(categoryId: Long, userId:Long): Try[Int] = {
    require(categoryId > 0)
    Try(db.withConnection {
      implicit c => {
        SQL(DELETE_CATEGORY_BY_ID)
          .on("id" -> categoryId, "user_id" ->userId).executeUpdate()
      }
    })
  }


}
