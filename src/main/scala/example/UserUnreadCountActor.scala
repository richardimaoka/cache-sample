package example

import akka.actor.{Actor, Props}
import example.UserUnreadCountActor.{Decrement, Increment, SetUnreadCount}

object UserUnreadCountActor {
  sealed trait Message
  case class SetUnreadCount(c: Int) extends Message
  case object Increment extends Message
  case object Decrement extends Message

  def props(user: String): Props = Props(new UserUnreadCountActor(user))
}

class UserUnreadCountActor(user: String) extends Actor {
  var unreadCount: Int = 0

  def receive = {
    case SetUnreadCount(count) =>
      unreadCount = count
      notifyFireBase()

    case Increment =>
      unreadCount += 1
      notifyFireBase()

    case Decrement =>
      if(unreadCount > 0) unreadCount -= 1
      notifyFireBase()
  }

  private def notifyFireBase(): Unit = ???
}
