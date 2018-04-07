package example

import java.io.{PrintWriter, StringWriter}

import akka.actor.ActorRef
import example.ProcessorActor.{NewEvent, ReadAllComments}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = akka.actor.ActorSystem()
    implicit val ec = system.dispatcher

    try {
      val topicUserListActor = system.actorOf(TopicToUserListActor.props)
      val processActor = system.actorOf(ProcessorActor.props(topicUserListActor))

      val users = for {
        i <- 1 to 10
      } yield "user" + i

      val topics = List(
        "topicA", "topicB", "topicC"
      )

      var userToCountActor: Map[String, ActorRef] = Map.empty
      users.foreach{
        user => 
          val ref = system.actorOf(UserUnreadCountActor.props(user))
          userToCountActor = userToCountActor.updated(user, ref)
      }

      for {
        user <- users
        topic <- topics
      }  {
        val couterRef = userToCountActor.get(user)
        couterRef.foreach{ ref =>
          system.actorOf(UserUnreadFlagActor.props(user, topic, ref))
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