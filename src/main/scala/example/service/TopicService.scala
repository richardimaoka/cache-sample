package example.service

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import example.actor.TopicActor
import example.domain.{Topic, User}

class TopicService(system: ActorSystem) {
  var mapping: Map[Topic, ActorRef] = Map.empty
  val logger: LoggingAdapter = system.log

  private def pathName(topic: Topic, user: User): String =
    s"/user/${topic.topicId}/${user.userId}"

  /**
   * Create a topic
   */
  def addTopic(topic: Topic): Unit = {
    logger.debug("Adding a topic actor for {}", topic)
    val ref = system.actorOf(TopicActor.props(topic), topic.topicId)
    mapping += (topic -> ref)
  }

  /**
   * Create a user/topic state machine actor
   */
  def subscribeTo(topic: Topic, user: User): Unit = {
    logger.debug("Adding an actor representing {}'s subscription to {}", user, topic)
    mapping.get(topic).foreach {
      ref => ref ! TopicActor.Message.Subscribe(user)
    }
  }

  /**
   * Remove a user/topic state machine actor
   */
  def unsubscribeFrom(topic: Topic, user: User): Unit = {
    logger.debug("Removing an actor representing {}'s subscription to {}", user, topic)
    mapping.get(topic).foreach {
      ref => ref ! TopicActor.Message.Unsubscribe(user)
    }
  }
}
