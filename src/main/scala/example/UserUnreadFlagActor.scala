package example

import akka.actor.{ActorRef, FSM, Props}

object UserUnreadFlagActor {
  sealed trait Message
  case object NewComment extends Message
  case object ReadAllComments extends Message

  sealed trait State
  case object Read extends State
  case object Unread extends State

  sealed trait Data
  case object Dummy extends Data

  def pathName(user: String): String = "/user/unread/" + user

  def props(user: String, topic: String, batchUpdater: ActorRef): Props = Props(new UserUnreadFlagActor(user, topic, batchUpdater))
}

class UserUnreadFlagActor(user: String, topic: String, batchUpdater: ActorRef)
  extends FSM[UserUnreadFlagActor.State, UserUnreadFlagActor.Data] {

  import UserUnreadFlagActor._

  startWith(Read, Dummy)

  when(Read) {
    case Event(NewComment, Dummy) ⇒
      goto(Unread)
  }

  when(Unread) {
    case Event(ReadAllComments, Dummy) ⇒
      goto(Read)
  }

  onTransition {
    case Read -> Unread ⇒
      batchUpdater ! UserUnreadCountActor.Increment
    case Unread -> Read ⇒
      batchUpdater ! UserUnreadCountActor.Decrement
  }
}
