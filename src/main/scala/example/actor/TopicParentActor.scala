package example.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import example.domain.{Topic, User}
import example.service.UserService

object TopicParentActor {
  sealed trait Message
  object Message {
    case class AddTopic(topic: Topic) extends Message
    case class RemoveTopic(topic: Topic) extends Message
    case class Subscribe(topic: Topic, user: User) extends Message
    case class Unsubscribe(topic: Topic, user: User) extends Message
    case class AllRead(topic: Topic, user: User) extends Message
    case class NewComment(topic: Topic, updatingUser: User) extends Message
    case class SetUnread(topic: Topic, user: User) extends Message
  }

  def props(userService: UserService): Props = Props(new TopicParentActor(userService))
  val name: String = "topics"
}

class TopicParentActor(userService: UserService) extends Actor with ActorLogging {
  import TopicParentActor._

  var mapping: Map[Topic, ActorRef] = Map.empty

  override def preStart() {
    super.preStart()
    log.debug(s"starting up TopicParentActor")
  }

  override def postStop() {
    super.postStop()
    log.debug(s"stopped TopicParentActor")
  }

  def receive = {
    /**
     * When a `topic` is added
     */
    case Message.AddTopic(topic) =>
      log.debug(s"Adding child for ${topic}")
      val ref = context.actorOf(TopicActor.props(topic, userService), topic.topicId)
      mapping = mapping.updated(topic, ref)

    /**
     * When a `topic` is removed.
     * All subscribed users should see the unread status updated.
     */
    case Message.RemoveTopic(topic) =>
      log.debug(s"Removing child for ${topic}")
      mapping.get(topic) match {
        case Some(ref) =>
          context.stop(ref)
          mapping = mapping - topic
        case None =>
          log.error("{} is not initialized yet", topic)
      }

    /**
     * When a `user` subscribes to the `topic`.
     */
    case Message.Subscribe(topic, user) =>
      log.debug("{} subscribing to {}", user, topic)
      mapping.get(topic) match {
        case Some(topicRef) =>
          topicRef ! TopicActor.Message.Subscribe(user)
        case None =>
          log.error("{} is not initialized yet", topic)
      }

    /**
     * When a `user` unsubscribes from the `topic`.
     */
    case Message.Unsubscribe(topic, user) =>
      log.debug("{} unsubscribing from {}", user, topic)
      mapping.get(topic) match {
        case Some(topicRef) =>
          topicRef ! TopicActor.Message.Unsubscribe(user)
        case None =>
          log.error("{} is not initialized yet", topic)
      }

    /**
     * When a `user` read all comments of the topic.
     * The unread status for the `user` and the `topic` should be set to "read".
     */
    case Message.AllRead(topic, user) =>
      log.debug("{} subscribing to {}", user, topic)
      mapping.get(topic) match {
        case Some(topicRef) =>
          topicRef ! TopicActor.Message.AllRead(user)
        case None =>
          log.error("{} is not initialized yet", topic)
      }

    /**
     * When a new comment for the `topic` is made by the `updatingUser`.
     * All subscribing users need to be notified except the `updatingUser`.
     */
    case Message.NewComment(topic, updatingUser) =>
      log.debug("{} add a new comment to {}", updatingUser, topic)
      mapping.get(topic) match {
        case Some(topicRef) =>
          topicRef ! TopicActor.Message.NewComment(updatingUser)
        case None =>
          log.error("{} is not initialized yet", topic)
      }

    /**
     * When the cache process is started up, or when the unread status needs to be refreshed
     * SetUnread is used to set the unread status for the user and the topic
     */
    case Message.SetUnread(topic, user) =>
      log.debug("{} set unread to {}", user, topic)
      mapping.get(topic) match {
        case Some(topicRef) =>
          topicRef ! TopicActor.Message.SetUnread(user)
        case None =>
          log.error("{} is not initialized yet", topic)
      }
  }

}
