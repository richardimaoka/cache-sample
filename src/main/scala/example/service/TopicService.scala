package example.service

import akka.actor.{ActorRef, ActorSystem}
import example.actor.{TopicActor, TopicParentActor}
import example.domain.{Topic, User}

class TopicService(system: ActorSystem, userService: UserService) {
  import TopicParentActor._
  val topicParent: ActorRef =
    system.actorOf(TopicParentActor.props(userService), TopicParentActor.name)

  /**
   * Create a topic
   */
  def addTopic(topic: Topic): Unit =
    topicParent ! Message.AddTopic(topic)

  /**
   * Remove a topic and all corresponding subscriptions
   */
  def removeTopic(topic: Topic): Unit =
    topicParent ! Message.RemoveTopic(topic)

  /**
   * Let user subscribe to the topic
   * If the topic is not initialized inside this service, log an error message.
   */
  def subscribeTo(topic: Topic, user: User): Unit =
    topicParent ! Message.Subscribe(topic, user)

  /**
   * Let user unsubscribe from the topic
   * If the topic is not initialized inside this service, log an error message.
   */
  def unsubscribeFrom(topic: Topic, user: User): Unit =
    topicParent ! Message.Unsubscribe(topic, user)

  /**
   * Let the user send a new message in for the topic.
   * If the topic is not initialized inside this service, log an error message.
   */
  def newMessage(topic: Topic, updatingUser: User): Unit =
    topicParent ! Message.NewComment(topic, updatingUser)

  /**
   * Let the user mark all-read for the topic.
   * If the topic is not initialized inside this service, log an error message.
   */
  def allRead(topic: Topic, user: User): Unit =
    topicParent ! Message.AllRead(topic, user)

  /**
   * Set the unread status for the topic and the user.
   * It must be used only in the initialization phase of the cache for the user.
   */
  def setUnread(topic: Topic, user: User): Unit =
    topicParent ! Message.SetUnread(topic, user)

}
