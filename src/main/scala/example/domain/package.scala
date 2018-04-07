package example

package object domain {
  case class User(val userId: String) extends AnyVal
  case class Topic(val topicId: String) extends AnyVal
}
