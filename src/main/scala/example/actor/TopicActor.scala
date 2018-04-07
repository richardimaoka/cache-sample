package example.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import example.domain.{Topic, User}
import example.service.UserService

object TopicActor {
  /**
   * Messages which the corresponding Actor will receive.
   */
  sealed trait Message
  object Message {
    case class Subscribe(user: User) extends Message
    case class Unsubscribe(user: User) extends Message
  }

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(topic: Topic, userService: UserService): Props = Props(new TopicActor(topic, userService))
}

class TopicActor(topic: Topic, userService: UserService) extends Actor with ActorLogging {
  import TopicActor._

  var mapping: Map[User, ActorRef] = Map.empty

  def receive = {
    case Message.Subscribe(user) =>
      log.debug("Adding an actor representing {}'s subscription to {}", user, topic)
      userService.userRef(user) match {
        case Some(userRef) =>
          val statusRef =
            context.actorOf(UserUnreadStatusActor.props(topic, user, userRef), user.userId)
          mapping = mapping + (user -> statusRef)
        case None =>
          log.error("{} cannot subscribe to {} as the user count actor does not exist.", user, topic)
      }

    case Message.Unsubscribe(user) =>
      log.debug("Removing an actor representing {}'s subscription to {}", user, topic)
      mapping.get(user) match {
        case Some(ref) =>
          context.stop(ref)
          mapping = mapping - user
        case None =>
          log.error("{} cannot unsubscribe from {} as the user did not subscribe to the topic.", user, topic)
      }
  }
}
