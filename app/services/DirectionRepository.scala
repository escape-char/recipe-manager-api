package services
import javax.inject.{Inject, Singleton}
import play.api.Logger
import scala.util.Try
import anorm.{SqlParser, _}
import play.api.db.Database
import models.Direction

/**
  * service for managing direction data in database
  * @param db database containing direction table
  */
@Singleton
class DirectionRepository  @Inject()(db: Database) {
  private val logger = Logger(this.getClass())

  //query for inserting a direction
  val INSERT:String = "INSERT INTO directions(step, description, recipe_id) VALUES({step}, {description}, {recipe_id})"
  //query for searching for a direction
  val QUERY:String = "select * from directions where " +
    "({description) is NULL or description={description}) AND " +
    "({searchTerm} is NULL or description LIKE '%{searchTerm}%') AND " +
    "({direction_id} is NULL or direction_id={direction_id}) AND " +
    "(recipe_id={recipe_id}) group by directions.recipe_id"
  //query for updating a direction
  val UPDATE_QUERY:String = "update directions set description={description}, recipe_id={recipe_id}, step={step} " + "" +
    "where direction_id={direction_id}"

  //query for deleting a direction
  val DELETE_QUERY:String = "DELETE directions where direction_id={direction_id}"

  //transform sql row into a Direction class
  private val DirectionClassParser: RowParser[Direction] = (
    SqlParser.long("direction_id") ~
      SqlParser.str("description") ~
      SqlParser.int("step") ~
      SqlParser.long("recipe_id")
    ) map {
    case columnvalue1 ~ columnvalue2 ~ columnvalue3  ~ columnvalue4 =>
      Direction(columnvalue1, columnvalue2, columnvalue3, columnvalue4)
  }
  //transform sql rows into a list of Direction classes
  private val allDirectionsParser: ResultSetParser[List[Direction]] = DirectionClassParser.*

  /**
    * query for directions
    * @param recipeId recipe id of which direction belongs to
    * @param directionId  search by unique id of the direction
    * @param description search for exact description of the direction
    * @param searchTerm fuzzy search direction's description
    * @param step search for direction's step number
    * @return list of directions
    */
  def query(recipeId:Long,
            directionId:Option[Long],
            description: Option[String],
            searchTerm: Option[String],
            step:Option[Int]
           ): Try[List[Direction]]={
    Try(db.withConnection {
      implicit c => {
        SQL(QUERY).on(
          "recipe_id" -> recipeId,
          "direction_id" -> directionId,
          "searchTerm" -> searchTerm,
          "step" -> step,
          "description" -> description
        ).as(allDirectionsParser)
      }
    })
  }

  /**
    * delete an existing direction
    * @param directionId unique id of direction to delete
    * @return unique id of deleted direction
    */
  def delete(directionId: Long): Try[Int] = {
    Try(db.withConnection {
      implicit c => {
        SQL(DELETE_QUERY)
          .on("direction_id" -> directionId).executeUpdate()
      }
    })
  }

  /**
    * update an existing direction
    * @param direction direction to be updated
    * @return unique id of direction which was updated
    */
  def update(direction:Direction): Try[Int] = {
    Try(db.withConnection {
      implicit c => {
        SQL(UPDATE_QUERY)
          .on("direction_id" -> direction.direction_id,
              "step" -> direction.step,
              "recipe_id" -> direction.recipe_id,
            "description" -> direction.description).executeUpdate()
      }
    })
  }

}
