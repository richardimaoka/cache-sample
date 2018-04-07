package example.actor

import akka.actor.{FSM, Props}
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

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  val props: Props = Props(new BatchUpdaterActor)
}

class BatchUpdaterActor
  extends FSM[BatchUpdaterActor.State, BatchUpdaterActor.Data] {

  import BatchUpdaterActor._

  startWith(State.ColdStandBy, Map.empty)

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
    case Event(Message.Update(user, unreadCount), _: Data) =>
      goto(State.WarmStandBy) using Map(user -> unreadCount)
  }

  def updateFirebase(data: Data): Unit = ()
    //println(s"updateFirebase triggered $data")

  initialize()
}
