package example.service

import akka.actor.{ActorRef, ActorSystem}
import example.actor.TestBatchUpdaterActor

class TestBatchUpdaterService(system: ActorSystem, testReporter: ActorRef)
  extends BatchUpdaterService {

  override val batchUpdaterRef =
    system.actorOf(TestBatchUpdaterActor.props(testReporter), BatchUpdaterService.batchUpdaterName)
}
