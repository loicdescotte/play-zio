package wiring

import akka.actor.ActorSystem
import com.github.ghik.silencer.silent
import com.softwaremill.macwire._
import controllers.UserController
import play.api.ApplicationLoader.Context
import play.api._
import router.Routes
import users.{AkkaEventStore, InMemoryUserRepository, UserService}

@silent("never used")
class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {

  private implicit val as: ActorSystem = actorSystem

  private lazy val eventStore     = wire[AkkaEventStore]
  private lazy val userRepository = wire[InMemoryUserRepository]

  private lazy val userService = wire[UserService]

  // Controllers
  private lazy val userController = wire[UserController]

  // Router
  lazy val router: Routes = {
    val routePrefix: String = "/"
    wire[Routes]
  }

}
