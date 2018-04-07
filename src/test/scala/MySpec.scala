import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import example.service.{TestBatchUpdaterService, TopicService, UserService}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class MySpec() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val batchUpdaterService = new TestBatchUpdaterService(system, testActor)
  val userService = new UserService(system, batchUpdaterService)
  val topicService = new TopicService(system, userService)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo actor" must {
    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "hello world"
      expectMsg("hello world")
    }
  }
}