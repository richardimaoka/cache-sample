package example.service

import akka.actor.{ActorRef, ActorSystem}
import example.actor.{BatchUpdaterActor, TestBatchUpdaterActor}

class TestBatchUpdaterService(system: ActorSystem, testReporter: ActorRef)
  extends BatchUpdaterService {

  override val batchUpdaterRef =
    system.actorOf(TestBatchUpdaterActor.props(testReporter), BatchUpdaterActor.name)
}
