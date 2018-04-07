package example.service

import akka.actor.{ActorRef, ActorSystem}
import example.actor.BatchUpdaterActor

object BatchUpdaterService {
  val batchUpdaterName = "batchUpdater"
}

trait BatchUpdaterService {
  def batchUpdaterRef: ActorRef
}

class ProductionBatchUpdaterService(system: ActorSystem)
  extends BatchUpdaterService {
  import BatchUpdaterService._

  val batchUpdaterRef = system.actorOf(BatchUpdaterActor.props, batchUpdaterName)
}
