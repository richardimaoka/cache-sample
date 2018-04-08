package example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import example.actor.BatchUpdaterActor
import example.domain.{Topic, User}
import example.service.{TestBatchUpdaterService, TopicService, UserService}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.duration._

class BasicSpec()
  extends TestKit(ActorSystem("BasicSpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  val batchUpdaterService = new TestBatchUpdaterService(system, testActor)
  val userService = new UserService(system, batchUpdaterService)
  val topicService = new TopicService(system, userService)

  /************************************************************************************************
   * Test data section
   ***********************************************************************************************/
  val users  = for { i <- 1 to 10 } yield User("user" + i)
  val topics = for { c <- List("A", "B", "C", "D", "E", "F", "G") } yield Topic("topic" + c)

  //Boolean = true means unread, and false means already-read
  val initialSubscriptionMap: Map[User, List[(Topic, Boolean)]] = Map(
    User("user1") -> List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicC"), false)),
    User("user2") -> List((Topic("topicA"), true),  (Topic("topicB"), false), (Topic("topicD"), true)),
    User("user3") -> List((Topic("topicD"), true),  (Topic("topicE"), true)),
    User("user4") -> List((Topic("topicA"), true),  (Topic("topicC"), false), (Topic("topicG"), true)),
    User("user5") -> List((Topic("topicB"), true),  (Topic("topicC"), true),  (Topic("topicF"), true), (Topic("topicG"), true)),
    User("user6") -> List((Topic("topicB"), true),  (Topic("topicD"), false), (Topic("topicE"), true)),
    User("user7") -> List((Topic("topicC"), false), (Topic("topicE"), true)),
    User("user8") -> List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicC"), true), (Topic("topicE"), true)),
    User("user9") -> List((Topic("topicD"), true),  (Topic("topicF"), true),  (Topic("topicG"), false)),
    User("user10") -> List((Topic("topicA"), true))
  )

  def initialSubscriptions(user: User): List[(Topic, Boolean)] =
    initialSubscriptionMap.getOrElse(user, throw new Exception(s"$user does not exist"))
  
  private def countUnreadTopics(l: List[(Topic, Boolean)]): Int =
    l.count{ case (_, unread) => unread }

  private def constructCountData(subscriptions: Map[User, List[(Topic, Boolean)]]): BatchUpdaterActor.Data =
    subscriptions.map {
      case (user, list) => user -> countUnreadTopics(list)
    }

  def validateInitialSubscriptionMap(): Unit = {
    for {
      user <- users
      subscriptions = initialSubscriptionMap
        .getOrElse(user, throw new Exception(s"Validation failed!!: You need to define $user in `val initialSubscriptionMap`."))
      (topic, _) <- subscriptions
    } topics
      .find(_ == topic)
      .getOrElse(throw new Exception(s"Validation failed!!: $user subscribes to an invalid $topic. Update `val initialSubscriptionMap`."))
  }

  /************************************************************************************************
   * Test environment setup section
   ***********************************************************************************************/
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll(): Unit = {
    validateInitialSubscriptionMap()

    users.foreach { user => userService.addUser(user)}
    topics.foreach { topic => topicService.addTopic(topic)}

    for {
      user <- users
      (topic, unreadFlag) <- initialSubscriptions(user)
    } {
      topicService.subscribeTo(topic, user)
      if (unreadFlag) topicService.setUnread(topic, user)
    }
  }

  /************************************************************************************************
   * Test cases
   ***********************************************************************************************/

  /**
   * In case the whole cache process is restarted, we should be able to query into SQL DB,
   * and pass all topic-read/unread status for all users, then initialize the cache accordingly
   */
  "SetUnread" must {
    "initialize unread status correctly" when {
      "beforeAll initialized the test" in{
        val expected = constructCountData(initialSubscriptionMap)

        expectMsg(200.milliseconds, expected)
      }
    }

    "increase the unread count for user/topic" when {
      "sent for already-read user/topic" in{
        val user1 = User("user1")
        val topicC = Topic("topicC")
        topicService.setUnread(topicC, user1) //topic C is unread = false (i.e. already read) initially

        //Only the diff from the previous batch update should be reported
        val user1Unread = countUnreadTopics(initialSubscriptions(user1))
        val expected = Map(user1 -> (user1Unread + 1))

        expectMsg(200.milliseconds, expected)
      }
    }

    "does nothing" when {
      "sent for unread user/topic" in{
        val user1 = User("user1")
        val topicC = Topic("topicC")

        topicService.setUnread(topicC, user1) //topic C is unread = true as done in the above test case

        expectNoMessage(200.milliseconds)
      }
    }
  }


  "Subscribe" must {
    "not do anything" when {
      "user already subscribed to the toic" in {
        val user2 = User("user2")
        val topicA = Topic("topicA") //user2 already subscribed to topicA, and unread = true
        val topicB = Topic("topicB") //user2 already subscribed to topicA, and unread = false

        topicService.subscribeTo(topicA, user2)
        topicService.subscribeTo(topicB, user2)

        expectNoMessage(200.milliseconds)
      }
    }

    "let NewComment increase the unread count" when {
      "user newly subscribed to the topic" in {
        val user2 = User("user2")
        val user3 = User("user3")
        val topicE = Topic("topicE")            //user2 did not subscribe to topicE

        topicService.subscribeTo(topicE, user2) //user2
        topicService.newMessage(topicE, user3)  //user3 should send a new comment, not user 2

        val expected = Map(user2 -> 3)
        expectMsg(200.milliseconds, expected)
      }
    }
    
  }

  //  "AllRead" when {
  //    "invoked single time" must {
  //      "Decrease the unread count for a user/topic" in {
  //
  //      }
  //      "does not change anything for already subscribed topic" in {
  //        //expectNoMsg
  //      }
  //    }
  //
  //    "invoked multiple times" must {
  //      "Decrease the unread count for multiple users/topics" in {
  //
  //      }
  //    }
  //  }
  //
  //  "NewComment" when {
  //    "invoked single time" must {
  //      "Increase the unread count for all subscribers except the updating user" in {
  //
  //      }
  //      "does not do anything if all subscribers had unread" in {
  //
  //      }
  //    }
  //
  //    "invoked multiple times" must {
  //      "Increase the unread count for all subscribers except the updating user" in {
  //
  //      }
  //    }
  //  }

}