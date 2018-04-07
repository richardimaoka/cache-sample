package example.actor

import akka.actor.{Actor, ActorSelection, Props}
import example.actor.TopicEventProcessorActor.{Event => StatusEvent}
import example.domain.{Topic, User}

object TopicEventProcessorActor {
  /**
   * Messages which TopicEventProcessorActor will receive.
   */
  sealed trait Event
  object Event{
    case class NewComment(topic: Topic, updatingUser: User) extends Event
    case class ReadAllComments(topic: Topic, user: User) extends Event
  }

  /**
   * Use this to create an instance of TopicEventProcessorActor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  val props: Props = Props(new TopicEventProcessorActor)
}

class TopicEventProcessorActor extends Actor {
  import TopicEventProcessorActor._

  var topicUserMap: Map[Topic, List[User]] = ???

  def receive = {
    /**
     * Update all users subscribing to the topic, except the user who made the NewComment
     */
    case Event.NewComment(topic, updatingUser) =>
      topicUserMap.get(topic).foreach { userList =>
        for {
          user <- userList.filter(_ != updatingUser)
        } getActorSelection(topic, user) ! StatusEvent.NewComment
      }

    /**
     * Update only the user who ReadAllComments
     */
    case Event.ReadAllComments(topic, user) =>
      getActorSelection(topic, user) !  StatusEvent.ReadAllComments
  }

  def getActorSelection(topic: Topic, user: User): ActorSelection = ???
}
