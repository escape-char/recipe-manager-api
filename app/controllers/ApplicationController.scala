package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.Logger

/** home page for the recipe manager API
  * @constructor creates a new controller with the play configuration
  * @param config the play api configuration
  */
class ApplicationController @Inject()(config: play.api.Configuration) extends Controller {
  private val logger = Logger(this.getClass())


  /**
    * Home page for the recipe manager API
    *
    * @todo  make this a template with API documentation
    */
  def index = Action {
    Ok("The API is running")
  }

}
