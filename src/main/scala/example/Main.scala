package example

import java.io.{PrintWriter, StringWriter}

import example.actor.TopicMessageProcessorActor
import example.domain.{Topic, User}
import example.service.{BatchUpdaterService, TopicService, UserService}

object Main {
  def getTopics(i: Int): List[String] =
    if (i % 3 == 0)      List("topicA", "topicB", "topicC")
    else if (i % 3 == 1) List("topicB", "topicC")
    else                 List("topicC")

  def main(args: Array[String]): Unit = {
    implicit val system = akka.actor.ActorSystem()
    implicit val ec = system.dispatcher

    try {
      val batchUpdaterService = new BatchUpdaterService(system)
      val topicService = new TopicService(system)
      val userService = new UserService(system, batchUpdaterService)

      for {
        topicId <- List("topicA", "topicB", "topicC")
      } topicService.addTopic(Topic(topicId))

      for {
        i <- 1 to 10
      } userService.addUser(User("user" + i))

      for {
        i <- 1 to 10
        topicId <- getTopics(i)
      } topicService.subscribeTo(Topic(topicId), User("user" + i))


      val topicActor = system.actorOf(TopicMessageProcessorActor.props)

      Thread.sleep(1000)
    } catch {
      case t: Throwable =>
        val sw = new StringWriter
        t.printStackTrace(new PrintWriter(sw))
        println(t.getMessage)
        println(sw)

    } finally {
      system.terminate()
    }
  }
}