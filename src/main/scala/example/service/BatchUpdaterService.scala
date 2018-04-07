package example.service

import akka.actor.{ActorRef, ActorSystem}
import example.actor.BatchUpdaterActor

trait BatchUpdaterService {
  def batchUpdaterRef: ActorRef
}

class ProductionBatchUpdaterService(system: ActorSystem)
  extends BatchUpdaterService {

  val batchUpdaterRef = system.actorOf(BatchUpdaterActor.props, BatchUpdaterActor.name)
}
