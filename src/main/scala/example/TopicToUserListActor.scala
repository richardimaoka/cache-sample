package example

import akka.actor.{Actor, Props}


object TopicToUserListActor {
  case class GetUserList(topic: String)

  val props: Props = Props(new TopicToUserListActor)
}

class TopicToUserListActor extends Actor {
  import TopicToUserListActor._

  var topicUserListMap: Map[String, List[String]] = Map(
    "user1" -> List("topicA", "topicB", "topicC")
  )

  def receive = {
    case GetUserList(topic) =>
      sender() ! topicUserListMap.get(topic).getOrElse(List.empty)
  }
}
