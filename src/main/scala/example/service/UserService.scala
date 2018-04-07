package example.service

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import example.actor.UserParentActor
import example.domain.User

import scala.concurrent.Future
import scala.concurrent.duration._

class UserService(system: ActorSystem, batchUpdaterService: BatchUpdaterService) {
  import UserParentActor._

  implicit val timeout: Timeout = 1.second

  val userParent: ActorRef =
    system.actorOf(UserParentActor.props(batchUpdaterService.batchUpdaterRef), UserParentActor.name)

  /**
   * Add a user who can count the unread topics
   */
  def addUser(user: User): Unit = {
    userParent ! Message.AddUser(user)
  }

  def userRef(user: User): Future[Option[ActorRef]] =
    (userParent ? Message.GetUser(user)).mapTo[Option[ActorRef]]
}
