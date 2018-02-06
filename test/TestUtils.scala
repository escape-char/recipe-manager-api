import play.api.db.{Database, Databases}

object TestUtils {
  def withMyDatabase[T](block: Database => T) = {
    Databases.withDatabase(
      driver = "org.postgresql.Driver",
      url =  scala.util.Properties.envOrElse("RECIPE_MANAGER_API_TEST_POSTGRES_JDBC", null),
      name = scala.util.Properties.envOrElse("RECIPE_MANAGER_API_TEST_POSTGRES_DB", null),
      config = Map(
        "user" -> scala.util.Properties.envOrElse("RECIPE_MANAGER_API_TEST_POSTGRES_USER", null),
        "password" -> scala.util.Properties.envOrElse("RECIPE_MANAGER_API_TEST_POSTGRES_PASSWORD", null)
      )
    )(block)
  }
}
