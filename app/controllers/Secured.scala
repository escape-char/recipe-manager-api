package controllers
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ActionBuilder, _}
import play.api.mvc.Results._
import pdi.jwt.JwtSession._
import models.User
import play.api.Logger

/**
  * Wrapper around request for dealing with authentication
  * @param user - user session associated with the request
  * @param request - request to API
  */
class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

/**
  * Mixin trait for use with controllers to handle authenticated or admin only requests
  */
trait Secured {
  def Authenticated = AuthenticatedAction
  def Admin = AdminAction
}

/**
  * Action Builder for checking if action is authenticated
  */
object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
  val logger = Logger(this.getClass())

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]) =
    request.jwtSession.getAs[User]("user") match {
      case Some(user) => {
        block(new AuthenticatedRequest(user, request)).map(_.refreshJwtSession(request))
      }
      case _ => {
        Future.successful(Unauthorized)
      }
    }
}

/**
  * Action Builder for checking if action is authenticated and user is an admin
  */
object AdminAction extends ActionBuilder[AuthenticatedRequest] {
  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]) =
    request.jwtSession.getAs[User]("user") match {
      case Some(user) if user.isAdmin => block(new AuthenticatedRequest(user, request)).map(_.refreshJwtSession(request))
      case Some(_) => Future.successful(Forbidden.refreshJwtSession(request))
      case _ => Future.successful(Unauthorized)
    }
}


