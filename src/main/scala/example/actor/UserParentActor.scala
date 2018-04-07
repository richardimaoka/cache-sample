package example.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import example.domain.User

object UserParentActor {
  /**
   * Messages which the corresponding Actor will receive.
   */
  sealed trait Message
  object Message {
    case class AddUser(user: User) extends Message
    case class GetUser(user: User) extends Message
  }

  def props(batchUpdater: ActorRef): Props = Props(new UserParentActor(batchUpdater))
  val name: String = "users"
}

class UserParentActor(batchUpdater: ActorRef) extends Actor with ActorLogging {
  import UserParentActor._

  var mapping: Map[User, ActorRef] = Map.empty

  def receive = {
    case Message.AddUser(user) =>
      log.debug(s"Adding child for ${user}")
      val ref = context.actorOf(UserUnreadCountActor.props(user, batchUpdater), user.userId)
      mapping = mapping.updated(user, ref)
    case Message.GetUser(user) =>
      sender() ! mapping.get(user)
  }
}
