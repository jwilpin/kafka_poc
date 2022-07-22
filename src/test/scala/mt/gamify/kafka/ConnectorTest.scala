package mt.gamify.kafka

import com.dimafeng.testcontainers.{ForAllTestContainer, KafkaContainer}
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time._

import java.net.InetAddress
import java.util.Properties
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

class ConnectorTest
  extends AnyFlatSpec
  with ForAllTestContainer
  with Matchers
  with StrictLogging
  with BeforeAndAfterAll
  with Producer
  with Consumer
  with ScalaFutures {

  override lazy val container: KafkaContainer = KafkaContainer()

  private val clientId = InetAddress.getLocalHost.getHostName
  private val topic = "test"

  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(5, Millis))

  private implicit lazy val properties: Properties = {
    val props = new Properties()
    props.put("client.id", clientId)
    props.put("bootstrap.servers", container.bootstrapServers)
    //props.put("bootstrap.servers", "localhost:9092")
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
    props
  }

  lazy val admin = Admin.create(properties)

  behavior of "Connector"

  it should "connect to the broker" in {
    whenReady(admin.describeCluster.nodes.toScala) { nodes =>
      logger.info(s"nodes: $nodes")
      nodes should not be empty
    }
  }

  it should "connect to the topic" in {
    whenReady(createTopic(topic: String).flatMap(_ => admin.listTopics().names().toScala)) {
      topicNames =>
        assert(topicNames.asScala.toSet.contains(topic), s"$topic should be in $topicNames")
    }
  }

  it should "collect/consume data from current/latest entry inside the consumer" in {
    whenReady {
      for {
        _ <- produceAsync(topic, 100)
        items <- consumeAsync(topic, Span(10, Seconds))
      } yield items
    } { items => items should not be empty }
  }

  it should "maintain an open connection for a set of 1 minute and gracefully shutdown without issues" in {
    whenReady(
      {
        for {
          _ <- produceAsync(topic, 100)
          items <- consumeAsync(topic, Span(1, Minutes))
        } yield items
      },
      timeout(Span(90, Seconds)),
      interval(Span(1, Seconds))
    ) { items =>
      items should not be empty
    }
  }
}
