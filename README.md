
## RUN

以下の手順でテストケースを走らせることができます

```
> git clone https://github.com/richardimaoka/cache-sample.git
> cd cache-sample
> sbt
> test
```

```
[info] BasicSpec:
[info] SetUnread
[info]   must initialize unread status correctly
[info]   - when beforeAll initialized the test
[info]   must increase the unread count for user/topic
[info]   - when sent for already-read user/topic
[info]   must does nothing
[info]   - when sent for unread user/topic
[info] Subscribe
[info]   must not do anything
[info]   - when user already subscribed to the toic
[info]   must let NewComment increase the unread count
[info]   - when user newly subscribed to the topic
[info] AllRead
[info]   must not do anything
[info]   - when the user already read the topic
[info]   must decrease the unread count
[info]   - when the user hand unread items for the topic
[info] NewComment
[info]   must not do anything
[info]   - when all subscribers, except the updating user had unread items for the topic
[info]   must increase unread count, except the updatingUser
[info]   - when there are subscribers who all read the topic until the NewComment
```

なんかSpecの書き方(whenの位置)がおかしくなってしまいました…

## コード

今回のコードは単純なので、Actorのみに全てのロジックをもたせてもよいかと思いましたが、
それよりもServiceというものを作ったほうがコードの見通しが良くなるかと思い、
各Serviceが、それぞれが受け持つActorの親Actorに対する参照を持つという形にしました。

### UserService


```
UserService ----------------------------------------------
| : UserParentActor                                      |
|     +-------------UserUnreadCounterActor(user1)        |
|     +-------------UserUnreadCounterActor(user2)        |
|     +-------------UserUnreadCounterActor(user3)        |
|     +-------------UserUnreadCounterActor(user4)        |
|     ...                                                |
----------------------------------------------------------
```

以下の2つのメソッドでUserのActor(`UserUnreadCounterActor`)を追加または削除できます。
前提として、あるuserに対するあらゆる通知が来る前に、`addUser(user: User)`がよばれているという想定をしています。

```scala
def addUser(user: User): Unit
def removeUser(user: User): Unit
```

### TopicService

```
TopicService --------------------------------------------------------
| : TopicParentActor                                                |
|     +-------------TopicActor(topicA)                              |
|     |               +---------TopicUserStatusActor(topicA, user1) |
|     |               +---------TopicUserStatusActor(topicA, user2) |
|     |               +---------TopicUserStatusActor(topicA, user3) |
|     |                                                             |
|     +-------------UserUnreadCounterActor(user2)                   |
|     |               +---------TopicUserStatusActor(topicA, user2) |
|     |               +---------TopicUserStatusActor(topicA, user3) |
|     |                                                             |
|     +-------------UserUnreadCounterActor(user3)                   |
|     |               +...                                          |
|     |                                                             |
|     +-------------UserUnreadCounterActor(user4)                   |
|     |               +...                                          |
|     ...                                                           |
---------------------------------------------------------------------
```

以下の2つのメソッドでTopicのActor(`TopicActor`)を追加または削除できます。

```scala
def addTopic(topic: Topic): Unit
def removeTopic(topic: Topic): Unit
```

以下の2つのメソッドでUserがTopicにたいしてsubscribe/unsubscribeできます。

```scala
def subscribeTo(topic: Topic, user: User): Unit
def unsubscribeFrom(topic: Topic, user: User): Unit
```

以下の2つのメソッドは

- UserがTopicに対して新しいコメントを投稿した時、同じTopicをSubscribeしている他のユーザの既読数を更新するため
- UserがTopicのメッセージを全て読んだとき、そのUserの既読数を更新するため

に呼びます。

```scala
def newComment(topic: Topic, updatingUser: User): Unit
def allRead(topic: Topic, user: User): Unit 
```
