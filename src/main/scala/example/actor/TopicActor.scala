package example.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import example.domain.{Topic, User}
import example.service.UserService
import example.actor.TopicUserStatusActor.{Message => StatusMessage}

import scala.util.{Success, Failure}

object TopicActor {
  /**
   * Messages which the corresponding Actor will receive.
   */
  sealed trait Message
  object Message {
    case class Subscribe(user: User) extends Message
    case class Unsubscribe(user: User) extends Message
    case class AllRead(user: User) extends Message
    case class NewComment(updatingUser: User) extends Message
    case class SetUnread(user: User) extends Message
  }

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(topic: Topic, userService: UserService): Props = Props(new TopicActor(topic, userService))
}

class TopicActor(topic: Topic, userService: UserService) extends Actor with ActorLogging {
  import TopicActor._

  implicit val ec = context.dispatcher
  var mapping: Map[User, ActorRef] = Map.empty

  override def preStart() {
    super.preStart()
    log.debug(s"starting up TopicActor($topic)")
  }

  // FIXME: when a topic is removed, all subscribing user's unread status should be updated
  override def postStop() {
    super.postStop()
    log.debug(s"stopped TopicActor($topic)")
  }

  def receive = {
    /**
     * Create a new child actor which remembers read/unread status for the topic and the user.
     * Assumption is that `userService` returns an `ActorRef` for the `user`,
     * otherwise, log an error and do not create the child actor.
     */
    case Message.Subscribe(user) =>
      log.debug("Adding an actor representing {}'s subscription to {}", user, topic)
      val statusRef = context.actorOf(TopicUserStatusActor.props(topic, user), user.userId)
      mapping = mapping + (user -> statusRef)
      userService.userRef(user).onComplete {
        case Success(result) => result match {
          case Some(userRef) =>
            statusRef ! StatusMessage.SetUserRef(userRef)
          case None =>
            log.error("{} cannot subscribe to {} as the user count actor does not exist.", user, topic)
        }
        case Failure(e) =>
          log.error(e, "ActorRef for {} could not be retrieved.", user, topic)
      }


    /**
     * Removing the child actor which was created to remember read/unread status for the topic and the user.
     * Assumption is that the child actor already exists, otherwise, log an error.
     */
    case Message.Unsubscribe(user) =>
      log.debug("Removing an actor representing {}'s subscription to {}", user, topic)
      mapping.get(user) match {
        case Some(ref) =>
          context.stop(ref)
          mapping = mapping - user
        case None =>
          log.error("{} cannot unsubscribe from {} as the user did not subscribe to the topic.", user, topic)
      }

    /**
     * Notify all children (i.e.) users who are subscribing to the topic,
     * except the user who originated the new comment
     */
    case Message.NewComment(updatingUser) =>
      log.debug("NewComment from {} received for {}", updatingUser, topic)
      mapping.get(updatingUser) match {
        case Some(toSkipRef) =>
          context.children.foreach {
            child => if(child != toSkipRef) child ! StatusMessage.NewComment
          }
        case None =>
          log.warning("NewComment from {} received but the user did not subscribe to {}", updatingUser, topic)
          context.children.foreach {
            _ ! StatusMessage.NewComment
          }
      }

    /**
     * Notify only the child which is corresponding to the user
     * who read all the comments for the topic
     */
    case Message.AllRead(user) =>
      mapping.get(user) match {
        case Some(child) =>
          child ! StatusMessage.AllRead
        case None =>
          log.warning("ReadAll received for {} but the user did not subscribe to {}", user, topic)
      }

    /**
     * Notify unread items to only one child(i.e. user)
     * It must be used only in the initialization phase of the cache for the user
     */
    case Message.SetUnread(user) =>
      mapping.get(user) match {
        case Some(child) =>
          child ! StatusMessage.NewComment
        case None =>
          log.warning("SetUnread received for {} but the user did not subscribe to {}", user, topic)
      }
  }
}
