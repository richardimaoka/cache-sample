package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import example.actor.BatchUpdaterActor
import example.domain.{Topic, User}
import example.service.{TestBatchUpdaterService, TopicService, UserService}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class ScaleSpec() extends TestKit(ActorSystem("ScaleSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val batchUpdaterService = new TestBatchUpdaterService(system, testActor)
  val userService = new UserService(system, batchUpdaterService)
  val topicService = new TopicService(system, userService)

  val groupSize = 100
  val nGroups = 100
  val groupTopicSize = 10
  val userSize  = groupSize * nGroups

  val users  = for { i <- 1 to userSize } yield User("user" + i)
  val topics = for {
    groupN <- 1 to nGroups
    topicN <- 1 to groupTopicSize
  } yield Topic(topicName(groupN, topicN))

  override def beforeAll(): Unit = {
    users.foreach { user => userService.addUser(user)}
    topics.foreach { topic => topicService.addTopic(topic)}
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private def topicName(groupNumber: Int, topicNumber: Int): String =
    "group-" + groupNumber + "-topic-" + topicNumber

  private def getTopicSubscriptionStatus(user: User): List[(Topic, Boolean)] = {
    try {
      val userDigit = user.userId.split("user")(1).toInt
      val groupN = (userDigit - 1) / groupSize + 1
      val seq = for {
        topicN <- 1 to groupTopicSize
      } yield (Topic(topicName(groupN, topicN)), true)
      seq.toList
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

  "The Cache process" must {
    s"scale to $userSize users, and finish initialization in 1000 millisec" in {
      val subscriptions: Map[User, List[(Topic, Boolean)]] = constructSubscription(users)
      val expected = constructCountData(subscriptions)

      for {
        user <- users
        (topic, unreadFlag) <- subscriptions.get(user).get
      } {
        topicService.subscribeTo(topic, user)
        if(unreadFlag) topicService.setUnread(topic, user)
      }

      expectMsg(5000.milliseconds, expected)
    }

    s"scale to ${groupSize*groupTopicSize} topics, and process " in {
      for {
        groupN <- 1 to nGroups
        topicN <- 1 to groupTopicSize
      } topicService.newMessage(Topic(topicName(groupN, topicN)), User("user" + (groupN * groupSize)))
    }
  }
}