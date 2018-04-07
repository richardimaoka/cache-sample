package example

import java.io.{PrintWriter, StringWriter}

import akka.actor.ActorRef
import example.ProcessorActor.{NewEvent, ReadAllComments}
import example.domain.{Topic, User}

object Main {

  var n = 0
  def getTopics(): List[Topic] = {
    n += 0
    if (n % 3 == 0) {
      List(Topic("topicA"), Topic("topicB"), Topic("topicC"))
    }
    else if (n % 3 == 1) {
      List(Topic("topicA"), Topic("topicB"))
    }
    else {
      List(Topic("topicC"))
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val system = akka.actor.ActorSystem()
    implicit val ec = system.dispatcher

    try {
      val topicUserListActor = system.actorOf(TopicToUserListActor.props)
      val processActor = system.actorOf(ProcessorActor.props(topicUserListActor))
      val batchUpdator = system.actorOf(ProcessorActor.props(topicUserListActor))

      val users = for { i <- 1 to 10 } yield User("user" + i)

      users.foreach{ user =>
        userService.addUser(user)

        getTopics().foreach{ topic =>
          topicService.subscribeTo(topic, user)
        }
      }

      processActor ! NewEvent("topicA", NewEvent)
      processActor ! NewEvent("topicB", NewEvent)
      processActor ! NewEvent("topicC", NewEvent)
      processActor ! NewEvent("topicA", ReadAllComments)
      processActor ! NewEvent("topicB", ReadAllComments)
      processActor ! NewEvent("topicC", ReadAllComments)
      processActor ! NewEvent("topicA", NewEvent)
      processActor ! NewEvent("topicA", NewEvent)
      processActor ! NewEvent("topicA", NewEvent)

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