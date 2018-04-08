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
  val topics = for { c <- List("A", "B", "C", "D", "E", "F", "G", "H", "I") } yield Topic("topic" + c)

  //Boolean = true means unread, and false means already-read
  val initialSubscriptionMap: Map[User, List[(Topic, Boolean)]] = Map(
    User("user1") -> List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicC"), false), (Topic("topicH"), true)),
    User("user2") -> List((Topic("topicA"), true),  (Topic("topicB"), false), (Topic("topicD"), true)),
    User("user3") -> List((Topic("topicD"), true),  (Topic("topicE"), true)),
    User("user4") -> List((Topic("topicA"), true),  (Topic("topicC"), false), (Topic("topicG"), false), (Topic("topicH"), false), (Topic("topicI"), false)),
    User("user5") -> List((Topic("topicB"), true),  (Topic("topicC"), true),  (Topic("topicF"), true),  (Topic("topicG"), true),  (Topic("topicI"), false)),
    User("user6") -> List((Topic("topicB"), true),  (Topic("topicD"), false), (Topic("topicE"), true),  (Topic("topicI"), false)),
    User("user7") -> List((Topic("topicC"), false), (Topic("topicE"), true)),
    User("user8") -> List((Topic("topicA"), true),  (Topic("topicB"), true),  (Topic("topicC"), true),  (Topic("topicE"), true), (Topic("topicH"), true)),
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
        topicService.newComment(topicE, user3)  //user3 should send a new comment, not user 2

        val expected = Map(user2 -> 3)
        expectMsg(200.milliseconds, expected)
      }
    }
  }

  "AllRead" must {
    "not do anything" when {
      "the user already read the topic" in {
        val user4 = User("user4")
        val topicG = Topic("topicG")

        topicService.allRead(topicG, user4)

        expectNoMessage(200.milliseconds)
      }
    }

    "decrease the unread count" when {
      "the user hand unread items for the topic" in {
        val user4 = User("user4")
        val topicA = Topic("topicA")

        topicService.allRead(topicA, user4)

        val user4Unread = countUnreadTopics(initialSubscriptions(user4)) - 1 //decrease by 1
        val expected = Map(user4 -> user4Unread)

        expectMsg(200.milliseconds, expected)
      }
    }
  }

  "NewComment" must {
    "not do anything" when {
      "all subscribers, except the updating user had unread items for the topic" in {
        val user4 = User("user4") //all subscribers to topicH except user 4 has unread items
        val topicH = Topic("topicH")

        topicService.newComment(topicH, user4)

        expectNoMessage(200.milliseconds)
      }
    }

    "increase unread count, except the updatingUser" when {
      "there are subscribers who all read the topic until the NewComment" in {
        val user4 = User("user4") //user4, 5 and 6 initially read all of topicI
        val user5 = User("user5")
        val user6 = User("user6")
        val topicI = Topic("topicI")

        topicService.newComment(topicI, user4)
        val user5Unread = countUnreadTopics(initialSubscriptions(user5)) + 1 //increase by 1
        val user6Unread = countUnreadTopics(initialSubscriptions(user6)) + 1 //increase by 1
        val expected = Map(user5 -> user5Unread, user6 -> user6Unread) //except the updatingUser = user4

        expectMsg(200.milliseconds, expected)
      }
    }
  }

}