package mt.gamify.kafka

import com.typesafe.scalalogging.StrictLogging
import mt.gamify.kafka.Model.models
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.errors.TopicExistsException

import java.util.{Collections, Optional, Properties, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait Producer extends StrictLogging {

  def produceAsync(topic: String, items: Int)(implicit
      ec: ExecutionContext,
      properties: Properties
  ): Future[Unit] = {
    val producer = new KafkaProducer[UUID, Long](properties)
    val callback = new Callback {
      override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
        Option(exception) match {
          case Some(err) => logger.error(s"record was not produced. Error: $err")
          case None      => logger.debug(s"produced record: $metadata")
        }
      }
    }
    createTopic(topic)
      .map { _ =>
        models.take(items).foreach { model =>
          val record = new ProducerRecord(topic, model.key, model.value)
          producer.send(record, callback)
        }
        producer.flush()
      }
      .andThen(_ => producer.close())
      .andThen {
        case Success(_)     => logger.info(s"$items items where produced into $topic")
        case Failure(error) => logger.error(s"$items items couldn't be produced into $topic", error)
      }
  }

  def createTopic(topic: String)(implicit
      ec: ExecutionContext,
      properties: Properties
  ): Future[Unit] = {
    val newTopic =
      new NewTopic(topic, Optional.empty[Integer](), Optional.empty[java.lang.Short]());
    val adminClient = AdminClient.create(properties)
    Future {
      adminClient
        .createTopics(Collections.singletonList(newTopic))
        .all
        .get
    }
      .map(_ => ())
      .andThen(_ => adminClient.close())
      .recover {
        case e if e.getCause.isInstanceOf[TopicExistsException] =>
          logger.warn(s"topic $topic already exists")
      }
  }
}
