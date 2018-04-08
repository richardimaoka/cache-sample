package example.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import example.domain.User
import example.actor.BatchUpdaterActor.{Message => UpdaterMessage}

object UserUnreadCountActor {
  /**
   * Messages which the corresponding actor will receive.
   */
  sealed trait Message
  object Message {
    case object Increment extends Message
    case object Decrement extends Message
  }

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(user: User, batchUpdater: ActorRef): Props =
    Props(new UserUnreadCountActor(user, batchUpdater))
}

class UserUnreadCountActor(user: User, batchUpdater: ActorRef) extends Actor with ActorLogging {
  import UserUnreadCountActor._

  var unreadCount: Int = 0

  override def preStart() {
    super.preStart()
    log.debug(s"starting up UserUnreadCountActor($user)")
  }

  override def postStop() {
    super.postStop()
    log.debug(s"stopped UserUnreadCountActor($user)")
  }

  def receive: Receive = {
    case Message.Increment =>
      log.debug("Increment received for {}", user)
      unreadCount += 1
      batchUpdater ! UpdaterMessage.Update(user, unreadCount)

    case Message.Decrement =>
      log.debug("Decrement received for {}", user)
      if(unreadCount > 0) unreadCount -= 1
      batchUpdater ! UpdaterMessage.Update(user, unreadCount)
  }
}
