package services
import javax.inject.{Inject, Singleton}
import java.util.TimeZone
import java.text.{DateFormat, SimpleDateFormat}
import scala.util.Try
import anorm.{Row, SimpleSql, _}
import play.api.db.Database
import play.api.libs.json._
import models._
import play.api.Logger

/**
  * Service for handling recipes in database
  * @param db database which contains recipe relationships
  */
@Singleton
class RecipeRepository  @Inject()(db: Database) {
  private val logger = Logger(this.getClass())

  //query for recipes with associated ingredients, categories, and directions
  val QUERY:String = """select recipes.recipe_id, recipes.title, recipes.description, recipes.source,
    |recipes.url, recipes.difficulty, recipes.servings, recipes.notes, recipes.prep_time, recipes.cook_time,recipes.image,
    |recipes.create_datetime, to_json(array_agg(DISTINCT ingredients.*))::TEXT as ingredients,
    |to_json(array_agg(DISTINCT directions.*))::TEXT as directions, to_json(array_agg(DISTINCT categories.*))::TEXT as categories,
    |(select username from users where users.user_id=recipes.created_by) as created_by,  COUNT(*) OVER() AS total
  |from recipes inner join directions on recipes.recipe_id=directions.recipe_id
  |inner join ingredients on ingredients.recipe_id=recipes.recipe_id
  |left outer join category_recipe on category_recipe.recipe_id=recipes.recipe_id
  |left outer join categories on category_recipe.category_id=categories.category_id
  |where (({user_id} is null or categories.user_id={user_id}) and ({recipe_id} is null or recipes.recipe_id = {recipe_id}) and
    |({title} is null or recipes.title={title}) and  ({category_id} is null or categories.category_id={category_id})) and
    |(({search_term} is null or LOWER(recipes.title) LIKE LOWER({search_term})) or
    |({search_term} is null or LOWER(recipes.description) LIKE LOWER({search_term})) or
    |({search_term} is null or LOWER(categories.name) LIKE LOWER({search_term})))
  | group by recipes.recipe_id ORDER BY recipes.create_datetime desc, recipes.title ASC
  | limit {limit} offset {offset}""".stripMargin.replace("\n", " ")

  //query inserting a recipe
  val INSERT_RECIPE:String = """INSERT INTO recipes (title, description, source, difficulty, servings, notes, prep_time,
    |cook_time, image, created_by)
    |VALUES({title}, {description}, {source}, {url}, {difficult}, {servings},
      |{notes}, {prep_time}, {cook_time}, {image}, {created_by})""".stripMargin.replace("\n", " ")
  //query for updating a recipe
  val UPDATE_RECIPE:String =
    """UPDATE recipes set title={title}, description={description}, source={source},
      |difficulty={difficulty}, servings={servings}, notes={notes}, prep_time={prep_time}, cook_time={cook_time},
      |image={image}, create_by={created_by} where recipes.recipe_id={recipe_id}
    """.stripMargin.replace("\n", " ")



  //crate temp tables queries to easily perform update, delete, and insert logic
  val CREATE_TMP_CATEGORIES:String = """CREATE TEMPORARY TABLE tmp_categories(tmp_id serial primary key,
    |category_id integer,name varchar(60) NOT NULL UNIQUE, is_default BOOLEAN NOT NULL DEFAULT FALSE,
     |user_id integer NOT NULL, recipe_id integer NOT NULL);""".stripMargin.replace("\n", " ")
  val CREATE_TMP_INGREDIENTS:String = """CREATE TEMPORARY TABLE tmp_ingredients(tmp_id serial primary key,
    |ingredient_id integer, item varchar(90) NOT NULL, amount integer NOT NULL,
    |unit_prefix ingredient_unit_prefix default NULL, unit_type ingredient_unit_type NOT NULL, recipe_id integer NOT NULL);"""
    .stripMargin.replace("\n", " ")
  val CREATE_TMP_DIRECTIONS:String = """CREATE TEMPORARY TABLE tmp_directions( tmp_id serial primary key,
    |direction_id integer,step integer not null, description text NOT NULL, recipe_id integer NOT NULL);"""
    .stripMargin.replace("\n", " ")

  //temp table insert queries
  val INSERT_TMP_INGREDIENTS:String = """INSERT INTO tmp_ingredients(ingredient_id, item, amount, unit_prefix, unit_type,
    |recipe_id) VALUES({ingredient_id}, {item}, {amount}, {unit_prefix}, {unit_type}, {recipe_id})"""
    .stripMargin.replace("\n", " ")
  val INSERT_TMP_CATEGORIES:String = """INSERT INTO tmp_categories(category_id, name, is_default, user_id, recipe_id)
   |VALUES({category_id}, {name}, {is_default}, {user_id}, {recipe_id});"""
    .stripMargin.replace("\n", " ")
  val INSERT_TMP_DIRECTIONS:String= """INSERT INTO tmp_directions(direction_id, step, description, recipe_id) VALUES
   |({direction_id}, {step}, {description}, {recipe_id})"""
    .stripMargin.replace("\n", " ")

  //category queries
  val INSERT_CATEGORIES:String = """INSERT INTO categories(name, is_default, user_id)
    |SELECT name, is_default, user_id FROM tmp_categories WHERE NOT EXISTS
    |(SELECT 1 FROM categories inner join tmp_categories on categories.category_id=tmp_categories.category_id);"""
    .stripMargin.replace("\n", " ")
  val INSERT_CATEGORY_RECIPE:String = """SELECT category_id, recipe_id INTO category_recipe from tmp_categories
    |where tmp_categories.category_id = null or tmp_category.category_id = 0;"""
    .stripMargin.replace("\n", " ")
  val UPDATE_CATEGORY_RECIPE:String = """UPDATE category_recipe SET category_id=tmp_categories.cateogry_id,
     |recipe_id = tmp_categories.recipe_id from tmp_categories where tmp_categories.category_id = tmp_categories.category_id;"""
    .stripMargin.replace("\n", " ")
  val DELETE_CATEGORY_RECIPE:String = """DELETE FROM category_recipe WHERE categories.recipe_id={recipe_id} AND
     |NOT EXISTS (select 1 FROM tmp_categories where tmp_categories.category_id = category_recipe.category_id);"""
     .stripMargin.replace("\n", " ")

  //ingredient queries
  val INSERT_INGREDIENTS:String = "INSERT INTO ingredients(item, amount, unit_prefix, unit_type, recipe_id)" +
   "SELECT item, amount, unit_prefix, unit_type, recipe_id from tmp_ingredients where " +
    "tmp_ingredients.ingredient_id=null or tmp_ingredients.ingredient_id=0;"
  val UPDATE_INGREDIENTS:String = "UPDATE ingredients SET item=tmp_ingredients.item, amount=tmp_ingredients.amount," +
    "unit_prefix=tmp_ingredients.unit_prefix, unit_type = tmp_ingredients.unit_type, recipe_id = tmp_ingredients.recipe_id " +
    "from tmp_ingredients where tmp_ingredients.ingredient_id = ingredients.ingredient_id;"
  val DELETE_INGREDIENTS:String = "DELETE FROM ingredients WHERE ingredients.recipe_id={recipe_id} AND " +
    "NOT EXISTS (select 1 FROM tmp_ingredients where tmp_ingredients.ingredient_id = ingredients.ingredient_id);"

  //direction queries
  val INSERT_DIRECTIONS:String ="INSERT INTO directions(step, description, recipe_id)" +
    "SELECT step, description, recipe_id from tmp_directions where tmp_directions.direction_id=null or tmp_directions.direction_id=0;"
  val UPDATE_DIRECTIONS:String = "UPDATE directions SET step=tmp_directions.step, description = tmp_directions.description, "  +
    "recipe_id = tmp_directions.recipe_id from tmp_directions where tmp_directions.direction_id = directions.direction_id; "
  val DELETE_DIRECTIONS:String = "DELETE FROM directions WHERE directions.recipe_id={recipe_id} AND "
    "NOT EXISTS (select 1 FROM tmp_directions where tmp_directions.direction_id = directions.direction_id);"

  //transform each sql result into Recipe class
  private val RecipeClassParser: RowParser[Recipe] = (
    SqlParser.long("recipe_id") ~
      SqlParser.str("title") ~
      SqlParser.str("description") ~
      SqlParser.str("source") ~
      SqlParser.str("url") ~
      SqlParser.str("difficulty") ~
      SqlParser.int("servings") ~
      SqlParser.str("notes") ~
      SqlParser.str("prep_time") ~
      SqlParser.str("cook_time") ~
      SqlParser.str("image")~
      SqlParser.date("create_datetime") ~
      SqlParser.str("created_by") ~
      SqlParser.str("ingredients") ~
      SqlParser.str("directions") ~
      SqlParser.str("categories") ~
      SqlParser.int("total")
    ) map {
    case columnvalue1 ~
         columnvalue2 ~
         columnvalue3 ~
         columnvalue4 ~
         columnvalue5 ~
         columnvalue6 ~
         columnvalue7 ~
         columnvalue8 ~
         columnvalue9 ~
         columnvalue10 ~
         columnvalue11 ~
         columnvalue12 ~
         columnvalue13 ~
         columnvalue14 ~
         columnvalue15 ~
         columnvalue16 ~
         columnvalue17
    => {
      //database time contains Time Zone Offset... Ensure it is UTC format
      val tz: TimeZone = TimeZone.getTimeZone("UTC")
      val df: DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
      df.setTimeZone(tz);
      val isoStr:String = df.format(columnvalue12)
      val ingJson:JsValue = Json.parse(columnvalue14)
      val jsResult:JsResult[List[Ingredient]] = ingJson.validate[List[Ingredient]](Ingredient.readJsonList)
      // Pattern matching
      jsResult match {
        case s: JsSuccess[List[Ingredient]] => println("Name: " + s.get)
        case e: JsError => println("Errors: " + JsError.toJson(e).toString())
      }


      Recipe(
        columnvalue1,
        columnvalue2,
        columnvalue3,
        columnvalue4,
        columnvalue5,
        RecipeDifficulty.withName(columnvalue6),
        columnvalue7,
        columnvalue8,
        columnvalue9,
        columnvalue10,
        columnvalue11,
        isoStr,
        columnvalue13,
      ingJson.validate[List[Ingredient]](Ingredient.readJsonList).get,
      Direction.readJsonArray(columnvalue15),
        RecipeCategory.readJsonArray(columnvalue16),
        columnvalue17)
    }
  }

  //transform all sql results to list of Recipe classes
  private val allRecipesParser: ResultSetParser[List[Recipe]] = RecipeClassParser.*

  /**
    * query for recipes
    * @param offset amount of recipe to skip from the beginning
    * @param limit limit the amount of recipes returned
    * @param searchTerm  fuzzy search string for recipe title and recipe description
    * @param title  search string for exact recipe title
    * @param userId search number for recipes which belong to unique user id
    * @param recipeId  search number for unique recipe id
    * @param categoryId search number for unique category id
    * @return list of recipes based on query
    */
  def query(offset:Option[Int],
                    limit:Option[Int],
                    searchTerm:Option[String],
                    title:Option[String],
                    userId:Option[Long],
                    recipeId:Option[Long],
                    categoryId:Option[Long]
                   ): Try[List[Recipe]] = {
    logger.debug("query()")

    val search:Option[String]= if(!searchTerm.isDefined || searchTerm.isEmpty){
      searchTerm
    }else{
      Some("%" + searchTerm.get + "%")
    }
    logger.debug(s"searchTerm: $search")

    val limit2:Int = limit.getOrElse(0)
    //filter out any parameters which are null or zero . except for
    // offset for except which can default to zero
    val params:Seq[NamedParameter] = Seq(
      NamedParameter("limit", ParameterValue.toParameterValue[Option[Int]](limit)),
      NamedParameter("search_term", ParameterValue.toParameterValue[Option[String]](search)),
      NamedParameter("title", ParameterValue.toParameterValue[Option[String]](title)),
      NamedParameter("user_id", ParameterValue.toParameterValue[Option[Long]](userId)),
      NamedParameter("recipe_id", ParameterValue.toParameterValue[Option[Long]](recipeId)),
      NamedParameter("category_id", ParameterValue.toParameterValue[Option[Long]](categoryId)),
      NamedParameter("offset", ParameterValue.toParameterValue[Int](offset.getOrElse(0))))

    Try(db.withConnection {
      implicit c => {
        SQL(QUERY).on(params:_*).as(allRecipesParser)
      }
    })
  }

  /**
    * handles updating or inserting a query based on if the recipe id is null or zero
    *
    * Insert new recipe if recipeId is null or zero. Otherwise, update existing recipe
    *
    * @param recipe recipe to be updated or inserted
    * @param userId unique id of user creating/updating this recipe
    * @return unique id of updated recipe or newly inserted recipe
    */
  def upsert(recipe:Recipe, userId:Long): Try[Option[Long]] ={
    Try(db.withConnection {
      implicit c => {
        val recipe_id:Long = if(recipe.id == 0 || recipe.id == null) {
          //create new recipe use case
          val result: Option[Long] = SQL(INSERT_RECIPE)
            .on("title" -> recipe.title,
              "description" -> recipe.description,
              "source" -> recipe.source,
              "url" -> recipe.url,
              "difficulty" -> recipe.difficulty.toString(),
              "servings" -> recipe.servings,
              "notes" -> recipe.notes,
              "prep_time" -> recipe.prepTime,
              "cook_time" -> recipe.cookTime,
              "image" -> recipe.image,
              "created_by" -> userId
            ).executeInsert()
           result.getOrElse(0)
        }else{
          //update existing recipe use case
          SQL(UPDATE_RECIPE)
            .on("title" -> recipe.title,
              "description" -> recipe.description,
              "source" -> recipe.source,
              "url" -> recipe.url,
              "difficulty" -> recipe.difficulty.toString(),
              "servings" -> recipe.servings,
              "notes" -> recipe.notes,
              "prep_time" -> recipe.prepTime,
              "cook_time" -> recipe.cookTime,
              "image" -> recipe.image,
              "created_by" -> userId,
              "recipe_id" -> recipe.id
            ).executeUpdate()
          recipe.id
        }

        //we will use temporary tables to quickly look up
        //what needs to be create, deleted, or inserted
        SQL(CREATE_TMP_CATEGORIES).executeUpdate()
        SQL(CREATE_TMP_INGREDIENTS).executeUpdate()
        SQL(CREATE_TMP_DIRECTIONS).executeUpdate()
        recipe.categories.foreach(cat =>{
          SQL(INSERT_TMP_CATEGORIES).on(
            "category_id" -> cat.category_id,
            "name" -> cat.name,
            "user_id" -> userId,
            "recipe_id" -> recipe_id
          ).executeInsert()
        })
        recipe.ingredients.foreach(i => {
          SQL(INSERT_TMP_INGREDIENTS).on(
            "recipe_id" ->  recipe_id,
            "ingredient_id" ->  i.ingredient_id,
            "item" -> i.item,
            "amount" -> i.amount,
            "unit_prefix" -> i.unit_prefix.toString(),
            "unit_type" -> i.unit_type.toString()
          ).executeInsert()
        })
        recipe.directions.foreach(d => {
          SQL(INSERT_TMP_DIRECTIONS)
            .on(
              "direction_id" -> d.direction_id,
              "description" -> d.description,
              "step" ->d.step,
              "recipe_id" -> recipe_id).executeInsert()
        })

        //update/create/delete categories, ingredients, and directions based on
        //data in the temporary tables
        SQL(UPDATE_CATEGORY_RECIPE).executeUpdate();
        SQL(UPDATE_INGREDIENTS).executeUpdate();
        SQL(UPDATE_DIRECTIONS).executeUpdate();

        SQL(DELETE_CATEGORY_RECIPE).on("recipe_id" -> recipe.id).executeUpdate();
        SQL(DELETE_INGREDIENTS).on("recipe_id" -> recipe.id).executeUpdate();
        SQL(DELETE_DIRECTIONS).on("recipe_id" -> recipe.id).executeUpdate();

        SQL(INSERT_DIRECTIONS).executeInsert()
        SQL(INSERT_INGREDIENTS).executeInsert()
        SQL(INSERT_CATEGORY_RECIPE).executeInsert()
        Option(recipe_id)
      }
    })
  }




}
