package controllers

import play.api.libs.json._
import play.api.mvc._
import users.{DataValidationError, User, UserService}
import zio.IO

class UserController(
    userService: UserService,
    controllerComponents: ControllerComponents
) extends AbstractController(controllerComponents) {

  private def jsonValidation[A](jsValue: JsValue)(implicit reads: Reads[A]) =
    IO.fromEither(jsValue.validate[A].asEither).mapError(e => DataValidationError(e.toString))

  import libs.http._

  // Action.zio takes a function returning a IO for a given request
  def createUser: Action[JsValue] = Action.zio(parse.json) { req =>
    val user = for {
      toCreate <- jsonValidation[User](req.body)
      created  <- userService.createUser(toCreate)
    } yield created

    // return a Bad Request status if an error is found, else return the user in Json format
    user.fold(e => BadRequest(Json.obj("error" -> e.message)), user => Ok(Json.toJson(user)))
  }

  def updateUser(id: String): Action[JsValue] =
    Action.zio(parse.json) { req =>
      val user = for {
        toUpdate <- jsonValidation[User](req.body)
        updated  <- userService.updateUser(id, toUpdate)
      } yield updated

      // you can also return different http codes depending on the error
      user.fold(
        {
          case e: DataValidationError => BadRequest(Json.obj("error"          -> e.message))
          case e                      => InternalServerError(Json.obj("error" -> e.message))
        },
        user => Ok(Json.toJson(user))
      )
    }

  def deleteUser(id: String): Action[AnyContent] = Action.zio { _ =>
    userService
      .deleteUser(id)
      .fold(e => BadRequest(Json.obj("error" -> e.message)), _ => NoContent)
  }

  def getUser(id: String): Action[AnyContent] = Action.zio { _ =>
    userService
      .get(id)
      .map {
        case Some(user) => Ok(Json.toJson(user))
        case None       => NotFound(Json.obj())
      }
      .recover(e => BadRequest(Json.obj("error" -> e.message)))
  }

  def listUser: Action[AnyContent] = Action.zio { _ =>
    userService.list
      .map(users => Ok(Json.toJson(users)))
      .recover(e => BadRequest(Json.obj("error" -> e.message)))
  }

}
