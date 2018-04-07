package example.actor

import akka.actor.{Actor, ActorSelection, Props}
import example.actor.TopicMessageProcessorActor.{Message => StatusMessage}
import example.domain.{Topic, User}

object TopicMessageProcessorActor {
  /**
   * Messages which the corresponding actor will receive.
   */
  sealed trait Message
  object Message{
    case class NewComment(topic: Topic, updatingUser: User) extends Message
    case class ReadAllComments(topic: Topic, user: User) extends Message
    case class UpdateSubscription(topic: Topic, users: List[User])
  }

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  val props: Props = Props(new TopicMessageProcessorActor)
}

class TopicMessageProcessorActor extends Actor {
  import TopicMessageProcessorActor._

  var topicUserMap: Map[Topic, List[User]] = Map.empty

  def receive = {
    /**
     * Update all users subscribing to the topic, except the user who made the NewComment
     */
    case Message.NewComment(topic, updatingUser) =>
      topicUserMap.get(topic).foreach { userList =>
        for {
          user <- userList.filter(_ != updatingUser)
        } getActorSelection(topic, user) ! StatusMessage.NewComment
      }

    /**
     * Update only the user who ReadAllComments
     */
    case Message.ReadAllComments(topic, user) =>
      getActorSelection(topic, user) !  StatusMessage.ReadAllComments

    case Message.UpdateSubscription(topic, users) =>
      topicUserMap = topicUserMap + (topic -> users)
  }

  def getActorSelection(topic: Topic, user: User): ActorSelection = ???
}
