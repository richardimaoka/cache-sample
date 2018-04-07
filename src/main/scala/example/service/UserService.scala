package example.service

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import example.actor.UserUnreadCountActor
import example.domain.User

class UserService(system: ActorSystem, batchUpdaterService: BatchUpdaterService) {
  val serviceName: String = getClass.getSimpleName
  var mapping: Map[User, ActorRef] = Map.empty
  val logger: LoggingAdapter = system.log

  /**
   * Add a user who can count the unread topics
   */
  def addUser(user: User): Unit = {
    logger.debug("{}: Adding {}", serviceName, user)
    val ref = system.actorOf(
      UserUnreadCountActor.props(user, batchUpdaterService.batchUpdaterRef)
    )
    mapping = mapping + (user -> ref)
  }


  def userRef(user: User): Option[ActorRef] = mapping.get(user)
}
