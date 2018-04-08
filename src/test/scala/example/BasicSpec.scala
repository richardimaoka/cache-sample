package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import example.actor.BatchUpdaterActor
import example.domain.{Topic, User}
import example.service.{TestBatchUpdaterService, TopicService, UserService}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class BasicSpec() extends TestKit(ActorSystem("BasicSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val batchUpdaterService = new TestBatchUpdaterService(system, testActor)
  val userService = new UserService(system, batchUpdaterService)
  val topicService = new TopicService(system, userService)

  val users  = for { i <- 1 to 10 } yield User("user" + i)
  val topics = for { c <- List("A", "B", "C", "D", "E", "F", "G") } yield Topic("topic" + c)

  override def beforeAll(): Unit = {
    users.foreach { user => userService.addUser(user)}
    topics.foreach { topic => topicService.addTopic(topic)}
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  /**
   * Mapping from user to list of subscribed topics and its read/unread status (true = unread)
   */
  private def getTopicSubscriptionStatus(user: User): List[(Topic, Boolean)] = {
    try {
      val i = user.userId.split("user")(1).toInt
      (i % 10) match {
        case 1 => List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicC"), true))
        case 2 => List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicE"), true), (Topic("topicG"), true))
        case 3 => List((Topic("topicD"), true),  (Topic("topicE"), true))
        case 4 => List((Topic("topicA"), true),  (Topic("topicC"), false), (Topic("topicG"), true))
        case 5 => List((Topic("topicB"), true),  (Topic("topicC"), true),  (Topic("topicF"), true), (Topic("topicG"), true))
        case 6 => List((Topic("topicB"), true),  (Topic("topicD"), false), (Topic("topicE"), true))
        case 7 => List((Topic("topicC"), false), (Topic("topicE"), true))
        case 8 => List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicC"), true), (Topic("topicE"), true))
        case 9 => List((Topic("topicD"), true),  (Topic("topicF"), true),  (Topic("topicG"), false))
        case 0 => List((Topic("topicA"), true))
      }
    } catch {
      case e: Exception =>
        println(s"error on parsing ${user}")
        throw e
    }
  }

  private def countUnreadTopics(l: List[(Topic, Boolean)]): Int =
    l.count{ case (_, unread) => unread }

  private def constructSubscription(users: Seq[User]): Map[User, List[(Topic, Boolean)]] =
    users.map {
      user => user -> getTopicSubscriptionStatus(user)
    }.toMap

  private def constructCountData(subscriptions: Map[User, List[(Topic, Boolean)]]): BatchUpdaterActor.Data =
    subscriptions.map {
      case (user, list) => user -> countUnreadTopics(list)
    }

  /**
   * In case the whole cache process is restarted, we should be able to query into SQL DB,
   * and pass all topic-read/unread status for all users, then initialize the cache accordingly
   *
   * `val subscriptions` represents the data from SQL DB.
   */
  "SetUnread" must {
    "initialize unread status correctly" in {
      val subscriptions: Map[User, List[(Topic, Boolean)]] = constructSubscription(users)
      val expected = constructCountData(subscriptions)

      for {
        user <- users
        (topic, unreadFlag) <- subscriptions.get(user).get
      } {
        topicService.subscribeTo(topic, user)
        if (unreadFlag) topicService.setUnread(topic, user)
      }

      expectMsg(200.milliseconds, expected)
    }
  }


  "Subscribe" when {
    "invoked single time" must {
      "Increase the unread count for a user/topic" in {
        /**
         * Only the diff from the previous batch update should be reported
         */
          val subscriptions: Map[User, List[(Topic, Boolean)]] = constructSubscription(users)

          val user3 = User("user3")
          topicService.subscribeTo(Topic("topicA"), user3)
          topicService.setUnread(Topic("topicA"), user3)

          val user3Unread = countUnreadTopics(subscriptions.get(user3).get)
          val expected = Map(user3 -> (user3Unread + 1))

          expectMsg(200.milliseconds, expected)

      }
      "does not change anything for already subscribed topic" in {
        //expectNoMsg
      }
    }

    "invoked multiple times" must {
      "Increase the unread count for multiple users/topics" in {
      }
    }
  }

  "AllRead" when {
    "invoked single time" must {
      "Decrease the unread count for a user/topic" in {

      }
      "does not change anything for already subscribed topic" in {
        //expectNoMsg
      }
    }

    "invoked multiple times" must {
      "Decrease the unread count for multiple users/topics" in {

      }
    }
  }

  "NewComment" when {
    "invoked single time" must {
      "Increase the unread count for all subscribers except the updating user" in {

      }
      "does not do anything if all subscribers had unread" in {

      }
    }

    "invoked multiple times" must {
      "Increase the unread count for all subscribers except the updating user" in {

      }
    }
  }

}