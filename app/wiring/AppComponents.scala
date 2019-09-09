package wiring

import com.github.ghik.silencer.silent
import controllers.UserController
import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api._
import router.Routes
import users.{AkkaEventStore, InMemoryUserRepository, UserService}

@silent("never used")
class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {

  private implicit val as = actorSystem

  private lazy val eventStore     = wire[AkkaEventStore]
  private lazy val userRepository = wire[InMemoryUserRepository]

  private lazy val userService = wire[UserService]

  // Controllers
  private lazy val userController = wire[UserController]

  // Router
  lazy val router = {
    val routePrefix: String = "/"
    wire[Routes]
  }

}
