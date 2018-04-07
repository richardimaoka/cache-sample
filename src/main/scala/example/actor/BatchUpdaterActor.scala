package example.actor

import akka.actor.FSM
import example.domain.User
import scala.concurrent.duration._

object BatchUpdaterActor {
  /**
   * Messages which the corresponding Actor will receive.
   */
  sealed trait Message
  object Message {
    case class Update(user: User, unreadCount: Int)
  }

  sealed trait State
  object State {
    case object ColdStandBy extends State
    case object WarmStandBy extends State
  }

  type Data = Map[User, Int]
}

class BatchUpdaterActor
  extends FSM[BatchUpdaterActor.State, BatchUpdaterActor.Data] {

  import BatchUpdaterActor._

  when(State.WarmStandBy, stateTimeout = 100.milliseconds) {
    case Event(Message.Update(user, unreadCount), data: Data) =>
      stay using data.updated(user, unreadCount)
    case Event(StateTimeout, _) =>
      goto(State.ColdStandBy)
  }

  onTransition {
    case State.WarmStandBy -> State.ColdStandBy =>
      updateFirebase(stateData)
  }

  when(State.ColdStandBy) {
    case Event(Message.Update(user, unreadCount), data: Data) =>
      goto(State.WarmStandBy) using Map(user -> unreadCount)
  }

  private def updateFirebase(data: Data): Unit =
    println("updateFirebase triggered")
}
