package example.service

import akka.actor.ActorSystem
import example.actor.BatchUpdaterActor

class BatchUpdaterService(system: ActorSystem) {
  val batchUpdaterRef = system.actorOf(BatchUpdaterActor.props, "batchUpdater")
}
