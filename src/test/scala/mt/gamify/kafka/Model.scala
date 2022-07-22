package mt.gamify.kafka

import java.util.UUID

final case class Model(key: UUID, value: Long)

object Model {

  private lazy val fibs: LazyList[Long] =
    0L #:: 1L #:: fibs.zip(fibs.tail).map { n => n._1 + n._2 }

  lazy val models: LazyList[Model] =
    fibs.map(fib => Model(UUID.randomUUID(), fib))
}
