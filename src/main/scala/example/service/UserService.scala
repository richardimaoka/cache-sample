package example.service

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import example.actor.UserUnreadCountActor
import example.domain.User

import scala.collection.mutable
import scala.concurrent.Future

class UserService(system: ActorSystem, batchUpdaterService: BatchUpdaterService) {
  val serviceName: String = getClass.getSimpleName
  val mapping: mutable.Map[User, ActorRef] = mutable.Map.empty
  val logger: LoggingAdapter = system.log

  /**
   * Add a user who can count the unread topics
   */
  def addUser(user: User): Unit = {
    logger.debug("{}: Adding {}", serviceName, user)
    val ref = system.actorOf(
      UserUnreadCountActor.props(user, batchUpdaterService.batchUpdaterRef)
    )
    mapping.update(user, ref)
  }


  def userRef(user: User): Future[Option[ActorRef]] = Future.successful{mapping.get(user)}
}
