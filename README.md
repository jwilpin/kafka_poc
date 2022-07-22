# Kafka POC

## Requirements

- Java 11
- Sbt
- Docker

## Running Kafka service (only for demo app)

```sh
docker compose up -d
```

- [Confluent Kafka Cluster center](http://localhost:9021/clusters)

## Testing

Tests were done using [kafka module of test containers](https://www.testcontainers.org/modules/kafka/). Hence, no local broker is required

```sh
sbt test
```

### expected results:

```sh
[info] ConnectorTest:
[info] Connector
[info] - should connect to the broker
[info] - should connect to the topic
[info] - should collect/consume data from current/latest entry inside the consumer
[info] - should maintain an open connection for a set of 1 minute and gracefully shutdown without issues
[info] Run completed in 1 minute, 30 seconds.
[info] Total number of tests run: 4
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 4, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
```

## References:
- [Confluent developer](https://docs.confluent.io/)
