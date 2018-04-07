package example.actor

import akka.actor.{ActorRef, Props}
import example.actor.BatchUpdaterActor.Data

object TestBatchUpdaterActor {
  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(testReporter: ActorRef): Props = Props(new TestBatchUpdaterActor(testReporter))
}

class TestBatchUpdaterActor(testReporter: ActorRef) extends BatchUpdaterActor {
  override def updateFirebase(data: Data) = {
    println(data)
    testReporter ! data
  }
}
