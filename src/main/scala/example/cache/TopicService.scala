package example.cache

class TopicService {
  /**
    * Create a topic
    */
  def addTopic(topic: String) = ???

  /**
    * Create a user/topic state machine actor
    */
  def subscribeTo(topic: String, user: String) = ???

  /**
    * Remove a user/topic state machine actor
    */
  def unsubscribeFrom(topic: String, user: String) = ???
}
