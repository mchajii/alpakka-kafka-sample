# Alpakka Akka sample

Sample to demonstrate that we can reduce the time taken by the `alpakka.kafka.producer` span to complete by increasing the interval on which the transactions are committed.

# Lightbend Credentials

You can access the commercial Lightbend Platform modules by providing a personalized resolver.<br/>
How to set up your Lightbend Platform Credentials:<br/>
https://www.lightbend.com/account/lightbend-platform/credentials

# Run the sample


To record traces, you first need to start the Elasticsearch cluster and Kafka:

```
docker-compose up
```

Generate test data in Kafka:

```
sbt "runMain com.mchajii.sample.BankTransactionsProducer"
```
Start the transactional stream:

```
sbt "runMain com.mchajii.sample.BalanceStream"
```

Navigate to http://localhost:5601 to interact with the Elasticsearch cluster and inspect the generated traces.           