package services

import play.api.db.{Database, Databases}

/**
  * Service for making tester easier
  */
object TestUtils {
  def withMyDatabase[T](block: Database => T) = {
    Databases.withDatabase(
      driver = "com.mysql.jdbc.Driver",
      url = "jdbc:mysql://localhost/test",
      name = "mydatabase",
      config = Map(
        "user" -> "test",
        "password" -> "secret"
      )
    )(block)
  }
}