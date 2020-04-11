package users

import java.time.{LocalDate, Period}

import akka.actor.ActorSystem
import play.api.libs.json.{Json, OFormat}
import users.Event.{UserCreated, UserDeleted, UserUpdated}
import zio.{IO, UIO}

import scala.collection.concurrent.TrieMap

case class User(email: String, name: String, birthDate: LocalDate, drivingLicenceDate: Option[LocalDate]) {
  def age: Int = Period.between(birthDate, LocalDate.now()).getYears
}

object User {
  implicit val format: OFormat[User] = Json.format
}

sealed trait Event

object Event {

  case class UserCreated(user: User) extends Event

  case class UserUpdated(id: String, user: User) extends Event

  case class UserDeleted(id: String) extends Event

}

abstract class AppError(val message: String)
case class DataValidationError(override val message: String) extends AppError(message)
case class DatabaseAccessError(override val message: String) extends AppError(message)

trait UserRepository {

  def get(id: String): IO[DatabaseAccessError, Option[User]]

  def set(id: String, user: User): IO[DatabaseAccessError, Unit]

  def delete(id: String): IO[DatabaseAccessError, Unit]

  def list: IO[DatabaseAccessError, Seq[User]]

}

trait EventStore {
  def publish(event: Event): IO[DatabaseAccessError, Unit]
}

class InMemoryUserRepository extends UserRepository {
  private val data = TrieMap.empty[String, User]

  override def get(id: String): UIO[Option[User]] = IO.succeed(data.get(id))

  override def set(id: String, user: User): UIO[Unit] = IO.succeed(data.update(id, user))

  override def delete(id: String): UIO[Unit] = IO.succeed(data.remove(id).fold(())(_ => ()))

  override def list: UIO[Seq[User]] = IO.succeed(data.values.toSeq)
}

class AkkaEventStore(implicit system: ActorSystem) extends EventStore {
  override def publish(event: Event): UIO[Unit] = IO.succeed(system.eventStream.publish(event))
}

class UserService(userRepository: UserRepository, eventStore: EventStore) {

  def createUser(user: User): IO[AppError, User] =
    for {
      _         <- IO.fromEither(validateDrivingLicence(user))
      mayBeUser <- userRepository.get(user.email)
      _         <- mayBeUser.map(_ => IO.fail(DataValidationError("User already exist"))).getOrElse(IO.succeed(()))
      _         <- userRepository.set(user.email, user)
      _         <- eventStore.publish(UserCreated(user))
    } yield user

  def updateUser(id: String, user: User): IO[AppError, User] =
    for {
      _         <- IO.fromEither(validateDrivingLicence(user))
      mayBeUser <- userRepository.get(user.email)
      _         <- mayBeUser.map(IO.succeed(_)).getOrElse(IO.fail(DataValidationError("User not exist, can't be updated")))
      _         <- userRepository.set(user.email, user)
      _         <- eventStore.publish(UserUpdated(id, user))
    } yield user

  def deleteUser(id: String): IO[DatabaseAccessError, Unit] =
    userRepository
      .delete(id)
      .flatMap(_ => eventStore.publish(UserDeleted(id)))

  def get(id: String): IO[DatabaseAccessError, Option[User]] = userRepository.get(id)

  def list: IO[DatabaseAccessError, Seq[User]] = userRepository.list

  private def validateDrivingLicence(user: User): Either[DataValidationError, User] =
    user.drivingLicenceDate.fold[Either[DataValidationError, User]](Right(user)) { licenceDate =>
      val isValidLicence = {
        val adultAge: Long    = 18
        val licenceMinimumAge = user.birthDate.plusYears(adultAge)
        user.age >= adultAge && licenceDate.isAfter(licenceMinimumAge)
      }

      if (isValidLicence) Right(user) else Left(DataValidationError("Too young to get a licence"))
    }

}
