
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

### performance/scenario test

やろうと思ってたんですが時間がなくてできませんでした…

## コード

今回のコードは単純なので、Actorのみに全てのロジックをもたせてもよいかと思いましたが、
それよりもServiceというものを作ったほうがコードの見通しが良くなるかと思い、
各Serviceが、それぞれが受け持つActorの親Actor(`UserParentActor`, `TopicService`)に対する参照を持つという形にしました。

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
前提として、あるuserに対するあらゆる通知が来る前に、`addUser(user: User)`がよばれているという想定をしています。([unread-count per user](https://github.com/richardimaoka/cache-sample/blob/master/README.md#unread-count-per-user)を参照)

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
かく`TopicActor`は`children`として自身の該当する`Topic`にsubscribeしている`User`を記憶しています。

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

### Data flow

#### AllRead/NewComment

TopicServiceの`newComment/allRead`メソッドが呼ばれた時、以下の画像の通り:

- `TopicParentActor` -> `TopicActor` -> `TopicUserStatusActor`

という流れでActor間でメッセージが渡されます。

<img width=600 src="https://user-images.githubusercontent.com/7414320/38472296-d34b1fc0-3bb8-11e8-8e61-3009b2c2d08c.png">
<img width=600 src="https://user-images.githubusercontent.com/7414320/38472299-db7ece76-3bb8-11e8-8709-bbdb7355ff49.png">

`AllRead/NewComment`メッセージが送られる以前に`AddTopic`と`Subscribe`メッセージが送られ、当該TopicとそのTopicに対するUserのSubscriptionが完了している必要があります。それ以前に`AllRead/NewComment`が送られた場合エラーメッセージが表示されます。

#### unread-count per user

そして`TopicUserStatus`が`AllRead/NewComment`メッセージを受け取ると、
当該のUserの既読数を更新するため`UserUnreadCountActor`に`Increment`もしくは`Decrement`メッセージを送ります。

<img width=600 src="https://user-images.githubusercontent.com/7414320/38472303-e1ae5686-3bb8-11e8-9d26-9de953e28c93.png">

そのためには`UserUnreadCountActor`が事前に存在している必要があるので、
あるuserに対するあらゆる通知が来る前に`UserService`の`addUser(user: User)`がよばれ`UserUnreadCountActor`の生成が完了しているという想定をしています。

(`UserService/UserParentActor`と`TopicService/TopicParentActor`に直接つながりはないので、問題が発生したときのデバッグなどを考えてRace Conditionを避けるために全てのメッセージが`GateWayActor`のような単一のアクターを通過するようにしたほうが良かったかもしれません。そうすると`GateWayActor`が単一障害点かつボトルネックになる可能性がありますが…)

#### Periodical notification to firebase

100ミリ秒ごとに`BatchUpdaterActor`は`updateFirebase`を呼びます。現在は特にこのメソッドは何もしていません。

<img width=600 src="https://user-images.githubusercontent.com/7414320/38472306-03a58aa2-3bb9-11e8-834a-b6722748f445.png">

