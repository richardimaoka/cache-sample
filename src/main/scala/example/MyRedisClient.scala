package example

import akka.util.ByteString
import redis.{ByteStringFormatter, RedisClient}

import scala.concurrent.{ExecutionContext, Future}

case class TopicUpdate(team: String, userUnreadCount: Seq[(String, Long)])

object StringSerializer {
  implicit val byteStringFormatter = new ByteStringFormatter[String] {
    def serialize(data: String): ByteString = ByteString(data)

    def deserialize(bs: ByteString): String = bs.utf8String
  }
}

class MyRedisClient(val redisClient: RedisClient)(implicit ec: ExecutionContext) {
  import StringSerializer._

  def updateTopic(team: String, topic: String, updatingUser: String): Future[TopicUpdate] = {
    for{
      subscribers <- redisClient.smembers[String](topic)
      filteredSubscribers = subscribers.filter(_ != updatingUser)
      userUnreadCount <- Future.traverse(filteredSubscribers)(getUserUnreadCount)
      data = TopicUpdate(team, userUnreadCount)
    } yield data
  }

  def setUnreadCount(user: String): Unit =
    redisClient.set(user, 10)

  def getUserUnreadCount(user: String): Future[(String, Long)] =
    for {
      count <- incrementUnreadCount(user)
    } yield (user, count)

  def incrementUnreadCount(user: String): Future[Long] =
    redisClient.incr(user)

  def decrementUnreadCount(user: String): Future[Long] =
    redisClient.decr(user)

  def addUserToTopic(topic: String, user: String): Unit = {
    redisClient.sadd(topic, user)
  }

}
