package example.actor

import akka.actor.{Actor, ActorRef, Props}
import example.domain.{Topic, User}

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
  def props(topic: Topic): Props = Props(new TopicActor(topic: Topic))
}

class TopicActor(topic: Topic) extends Actor {
  import TopicActor._

  var mapping: Map[User, ActorRef] = Map.empty

  def receive = {
    case Message.Subscribe(user) =>
      val ref = context.actorOf(UserUnreadStatusActor.props(topic, user), user.userId)
      mapping = mapping + (user -> ref)

    case Message.Unsubscribe(user) =>
      mapping.get(user).foreach { ref =>
        context.stop(ref)
      }
      mapping = mapping - user
  }
}
