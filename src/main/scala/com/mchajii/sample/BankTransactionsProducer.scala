package com.mchajii.sample

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

object BankTransactionsProducer extends App {

  private val log = LoggerFactory.getLogger(getClass)

  implicit private val system: ActorSystem = ActorSystem("BankTransactionsProducer")

  private val rnd = new scala.util.Random

  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  Source(1 to 500)
    .map(_ =>
      Seq(
        BankTransaction("BE79989744181449", rnd.between(0, 100)),
        BankTransaction("BE13523588032070", rnd.between(0, 100)),
        BankTransaction("BE37570542673034", rnd.between(0, 100))
      )
    )
    .mapConcat(identity)
    .map { transaction =>
      log.info(s"Writing bank transaction $transaction to Kafka")
      transaction
    }
    .map(transaction =>
      new ProducerRecord[String, String](
        "transactions", UUID.randomUUID().toString, transaction.asJson.noSpaces
      )
    )
    .runWith(Producer.plainSink(producerSettings))
}
