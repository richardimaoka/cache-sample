package example.actor

import akka.actor.{ActorRef, FSM, Props, Stash}
import example.domain.{Topic, User}

object TopicUserStatusActor {
  /**
   * Messages which the corresponding actor will receive.
   */
  sealed trait Message
  object Message {
    case class SetUserRef(userRef: ActorRef) extends Message
    case object NewComment extends Message
    case object ReadAllComments extends Message
  }

  sealed trait State
  object State {
    case object Uninitialized extends State
    case object Read extends State
    case object Unread extends State
  }

  case class Data(userUnreadCounter: ActorRef)

  def pathName(user: String): String = "/user/unread/" + user

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(topic: Topic, user: User): Props =
    Props(new TopicUserStatusActor(topic, user))
}

class TopicUserStatusActor(topic: Topic, user: User)
  extends FSM[TopicUserStatusActor.State, TopicUserStatusActor.Data]
  with Stash {
  import TopicUserStatusActor._

  startWith(State.Uninitialized, Data(null))

  when(State.Uninitialized) {
    case Event(Message.SetUserRef(userRef), _) ⇒
      log.debug("ReadAllComments received for {} and {}", topic, user)
      goto(State.Read) using Data(userRef)
    case Event(msg, _) ⇒
      log.debug("Stashing a message {} for {} and {}", msg, topic, user)
      stash()
      stay
  }

  when(State.Unread) {
    case Event(Message.ReadAllComments, _) ⇒
      log.debug("ReadAllComments received for {} and {}", topic, user)
      goto(State.Read)
    case Event(Message.NewComment, _) ⇒
      log.debug("NewComment received for {} and {} while the status is Unread", topic, user)
      stay
  }

  when(State.Read) {
    case Event(Message.NewComment, _) ⇒
      log.debug("NewComment received for {} and {}", topic, user)
      goto(State.Unread)
    case Event(Message.ReadAllComments, _) ⇒
      log.debug("ReadAllComments received for {} and {} while the status is Read", topic, user)
      stay
  }

  onTransition {
    case State.Uninitialized -> State.Read ⇒
      unstashAll()
    case State.Read -> State.Unread ⇒
      stateData.userUnreadCounter ! UserUnreadCountActor.Message.Increment
    case State.Unread -> State.Read ⇒
      stateData.userUnreadCounter ! UserUnreadCountActor.Message.Decrement
  }

  initialize()
}
