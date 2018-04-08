package example.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import example.domain.User

object UserParentActor {
  /**
   * Messages which the corresponding Actor will receive.
   */
  sealed trait Message
  object Message {
    case class GetUser(user: User) extends Message
    case class AddUser(user: User) extends Message
    case class RemoveUser(user: User) extends Message
  }

  def props(batchUpdater: ActorRef): Props = Props(new UserParentActor(batchUpdater))
  val name: String = "users"
}

class UserParentActor(batchUpdater: ActorRef) extends Actor with ActorLogging {
  import UserParentActor._

  def receive = {
    case Message.GetUser(user) =>
      sender() ! context.child(user.userId)

    case Message.AddUser(user) =>
      log.debug(s"Adding child for ${user}")
      context.actorOf(UserUnreadCountActor.props(user, batchUpdater), user.userId)

    case Message.RemoveUser(user) =>
      log.debug(s"Removing child for ${user}")
      context.child(user.userId) match {
        case Some(ref) =>
          context.stop(ref)
        case None =>
          log.error("{} is not initialized yet", user)
      }

  }
}
