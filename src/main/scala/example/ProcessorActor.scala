package example

import akka.actor.{Actor, ActorRef, ActorSelection, Props}
import akka.pattern.ask
import akka.util.Timeout
import example.TopicToUserListActor.GetUserList

import scala.concurrent.duration._

object ProcessorActor {

  sealed trait UpdateType
  case object NewComment extends UpdateType
  case object ReadAllComments extends UpdateType

  case class NewEvent(topic: String, updateType: UpdateType)

  def props(topicToUserListActor: ActorRef): Props = Props(new ProcessorActor(topicToUserListActor))
}

class ProcessorActor(topicToUserListActor: ActorRef) extends Actor {
  import ProcessorActor._

  implicit val ec = context.dispatcher
  implicit val timeout : Timeout = 3.second

  def receive = {
    case NewEvent(topic, updateType) =>
      val userList = (topicToUserListActor ? GetUserList(topic)).mapTo[List[String]]

      // use ListT ??
      userList.map(
        users => for{
          user <- users
        } yield getActorSelection(user) ! updateMessage(updateType)
      )
  }

  def getActorSelection(user: String): ActorSelection =
    context.actorSelection(UserUnreadFlagActor.pathName(user))

  def updateMessage(updateType: UpdateType): UserUnreadFlagActor.Message =
    updateType match {
      case NewComment => UserUnreadFlagActor.NewComment
      case ReadAllComments => UserUnreadFlagActor.ReadAllComments
    }
}
