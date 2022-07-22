package mt.gamify.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig

import java.net.InetAddress
import java.util.Properties
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Demo extends App with Producer with Consumer {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val topic = "test"

  private implicit lazy val properties: Properties = {
    val props = new Properties()
    props.put("client.id", InetAddress.getLocalHost.getHostName)
    props.put("bootstrap.servers", "localhost:9092")
    props.put(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.UUIDSerializer"
    )
    props.put(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.LongSerializer"
    )
    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.UUIDDeserializer"
    )
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.LongDeserializer"
    )
    props.put(ConsumerConfig.GROUP_ID_CONFIG, s"$topic-consumer-group")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props.put("polling.time", "10")
    props
  }

  val app = (for {
    _ <- createTopic(topic)
    _ <- produceAsync(topic, 1000)
    items <- consumeAsync(topic, 10.seconds)
  } yield {
    items.foreach(item => logger.info(s"consumed item: $item"))
    logger.info(s"finishing app with ${items.size} consumed items...")
  }).recover {
    case error => logger.error("app failed", error)
  }

  Await.ready(app, 1.minutes)
}
