package example.service

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import example.actor.TopicActor
import example.domain.{Topic, User}

class TopicService(system: ActorSystem, userService: UserService) {
  val serviceName: String = getClass.getSimpleName
  var mapping: Map[Topic, ActorRef] = Map.empty
  val logger: LoggingAdapter = system.log

  /**
   * Create a topic
   */
  def addTopic(topic: Topic): Unit = {
    logger.debug("{}: Adding a topic for {}", serviceName, topic)
    val ref = system.actorOf(TopicActor.props(topic, userService), topic.topicId)
    mapping += (topic -> ref)
  }

  /**
   * Let user subscribe to the topic
   * If the topic is not initialized inside this service, log an error message.
   */
  def subscribeTo(topic: Topic, user: User): Unit = {
    logger.debug("{}: {} is subscribing to {}", serviceName, user, topic)
    mapping.get(topic) match {
      case Some(topicRef) =>
        topicRef ! TopicActor.Message.Subscribe(user)
      case None =>
        logger.error("{}[subscribeTo]: {} is not initialized yet", serviceName,  topic)
    }
  }

  /**
   * Let user unsubscribe from the topic
   * If the topic is not initialized inside this service, log an error message.
   */
  def unsubscribeFrom(topic: Topic, user: User): Unit = {
    logger.debug("{}: {} is unsubscribed from {}", serviceName, user, topic)
    mapping.get(topic) match {
      case Some(topicRef) =>
        topicRef ! TopicActor.Message.Unsubscribe(user)
      case None =>
        logger.error("{}[unsubscribeFrom]: {} is not initialized yet", serviceName, topic)
    }
  }

  /**
   * Let the user send a new message in for the topic.
   * If the topic is not initialized inside this service, log an error message.
   */
  def newMessage(topic: Topic, updatingUser: User): Unit = {
    logger.debug("{}: {} sends a new message for {}", serviceName, updatingUser, topic)
    mapping.get(topic) match {
      case Some(topicRef) =>
        topicRef ! TopicActor.Message.NewComment(updatingUser)
      case None =>
        logger.error("{}[newMessage]: {} is not initialized yet", serviceName, topic)
    }
  }

  /**
   * Let the user mark all-read for the topic.
   * If the topic is not initialized inside this service, log an error message.
   */
  def allRead(topic: Topic, user: User): Unit = {
    logger.debug("{}: {} read all messages in {}", serviceName, user, topic)
    mapping.get(topic) match {
      case Some(topicRef) =>
        topicRef ! TopicActor.Message.ReadAll(user)
      case None =>
        logger.error("{}[allRead]: {} is not initialized yet", serviceName, topic)
    }
  }

  /**
   * Set the unread status for the topic and the user.
   * It must be used only in the initialization phase of the cache for the user.
   */
  def setUnread(topic: Topic, user: User): Unit = {
    logger.debug("{}: {} sets the status = unread for {}", serviceName, user, topic)
    mapping.get(topic) match {
      case Some(topicRef) =>
        topicRef ! TopicActor.Message.SetUnread(user)
      case None =>
        logger.error("{}[setUnread]: {} is not initialized yet", serviceName,  topic)
    }
  }

}
