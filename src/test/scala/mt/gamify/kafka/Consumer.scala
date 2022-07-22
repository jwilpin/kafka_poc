package mt.gamify.kafka

import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Properties, UUID}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try

trait Consumer extends StrictLogging {

  def consumeAsync(
      topic: String,
      timeout: FiniteDuration,
      poolTime: FiniteDuration = 100.millisecond
  )(implicit
      ec: ExecutionContext,
      properties: Properties
  ): Future[Seq[Model]] = {

    val consumer = new KafkaConsumer[UUID, Long](properties)
    val stopConsumer = new AtomicBoolean()

    val wait = Future {
      Thread.sleep(timeout.toMillis)
    }.recover {
      case e: InterruptedException =>
        logger.warn(s"couldn't wait until $timeout to wake consumer up")
    }.map { _ =>
      logger.info("waking consumer up...")
      stopConsumer.set(true)
      consumer.wakeup()
    }

    val collect = Future {
      consumer.subscribe(Seq(topic).asJavaCollection)
      var records: ListBuffer[Model] = ListBuffer.empty
      while (!stopConsumer.get()) {
        logger.info("polling...")

        val polledRecords: Seq[Model] = Try {
          consumer
            .poll(Duration.ofMillis(poolTime.toMillis))
            .asScala
            .toSeq
            .map(record => Model(record.key(), record.value()))
        }.recover {
          case _: WakeupException =>
            logger.info("polling stopped due to WakeupException, resuming...")
            Seq.empty[Model]
        }.get

        records ++= polledRecords
        logger.info(s"records: ${records.size}...")
      }
      logger.info(s"finishing consumer. Total records: ${records.size}...")
      consumer.commitSync();
      consumer.close()
      records.toSeq
    }

    wait.flatMap(_ => collect)
  }
}
