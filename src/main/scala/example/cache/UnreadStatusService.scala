package example.cache

import akka.actor.ActorRef

class UnreadStatus {

  val parent: ActorRef = ???

  def createTopicActor(topic: String)

  def createStatusActor(topic: String, user: String)

}
