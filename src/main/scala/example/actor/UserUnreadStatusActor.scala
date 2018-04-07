package example.actor

import akka.actor.{ActorRef, FSM, Props}
import example.UserUnreadCountActor

object UserUnreadStatusActor {
  /**
   * Events which the corresponding actor will receive.
   */
  sealed trait Message
  object Message {
    case object NewComment extends Message
    case object ReadAllComments extends Message
  }

  sealed trait State
  object State {
    case object Read extends State
    case object Unread extends State
  }

  sealed trait Data
  object Data {
    case object Dummy extends Data
  }

  def pathName(user: String): String = "/user/unread/" + user

  /**
   * Use this to create an instance of TopicEventProcessorActor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(user: String, topic: String): Props =
    Props(new UserUnreadCountActor(user, topic))
}

class UserUnreadCountActor(user: String, topic: String)
  extends FSM[UserUnreadStatusActor.State, UserUnreadStatusActor.Data] {

  import UserUnreadStatusActor._

  startWith(State.Read, Data.Dummy)

  when(State.Read) {
    case Event(Message.NewComment, Data.Dummy) ⇒
      goto(State.Unread)
  }

  when(State.Unread) {
    case Event(Message.ReadAllComments, Data.Dummy) ⇒
      goto(State.Read)
  }

  onTransition {
    case State.Read -> State.Unread ⇒
      batchUpdater ! UserUnreadCountActor.Message.Increment
    case State.Unread -> State.Read ⇒
      batchUpdater ! UserUnreadCountActor.Message.Decrement
  }
}
