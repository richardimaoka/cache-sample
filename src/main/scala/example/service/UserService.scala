package example.service

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import example.actor.UserUnreadCountActor
import example.domain.User

class UserService(system: ActorSystem) {
  var mapping: Map[User, ActorRef] = Map.empty
  val logger: LoggingAdapter = system.log

  /**
   * Create a per-user aggregator actor
   */
  def addUser(user: User): Unit = {
    logger.debug("Adding an actor for {}", user)
    val ref = system.actorOf(UserUnreadCountActor.props(user))
    mapping = mapping + (user -> ref)
  }
}
