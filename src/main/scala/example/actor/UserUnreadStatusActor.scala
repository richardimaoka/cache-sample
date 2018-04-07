package example.actor

import akka.actor.{ActorRef, FSM, Props}
import example.domain.{Topic, User}

object UserUnreadStatusActor {
  /**
   * Messages which TopicEventProcessorActor will receive.
   */
  sealed trait Message
  object Message {
    case object NewComment extends Message
    case object ReadAllComments extends Message

    //only used for self-messaging from UserUnreadStatusActor
    private[UserUnreadStatusActor] case class SetBatchUpdater(batchUpdater: ActorRef) extends Message
  }

  sealed trait State
  object State {
    case object Uninitialized extends State
    case object Read extends State
    case object Unread extends State
  }

  case class Data(batchUpdater: ActorRef)

  def pathName(user: String): String = "/user/unread/" + user

  /**
   * Use this to create an instance of TopicEventProcessorActor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(topic: Topic, user: User): Props =
    Props(new UserUnreadStatusActor(topic, user))
}

class UserUnreadStatusActor(topic: Topic, user: User)
  extends FSM[UserUnreadStatusActor.State, UserUnreadStatusActor.Data] {
  import UserUnreadStatusActor._

  //batchUpdater is intentionally null as it will be set upon transition to Unread
  startWith(State.Uninitialized, Data(batchUpdater = null))

  when(State.Uninitialized) {
    case Event(Message.SetBatchUpdater(batchUpdater), _) =>
      goto(State.Unread) using Data(batchUpdater)
  }

  when(State.Read) {
    case Event(Message.NewComment, _) ⇒
      goto(State.Unread)
  }

  when(State.Unread) {
    case Event(Message.ReadAllComments, _) ⇒
      goto(State.Read)
  }

  onTransition {
    case State.Read -> State.Unread ⇒
      stateData.batchUpdater ! UserUnreadCountActor.Message.Increment
    case State.Unread -> State.Read ⇒
      stateData.batchUpdater ! UserUnreadCountActor.Message.Decrement
  }
}
