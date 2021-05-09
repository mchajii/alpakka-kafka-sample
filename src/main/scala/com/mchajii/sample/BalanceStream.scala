package com.mchajii.sample

import java.util.UUID

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Transactional
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.RestartSettings
import akka.stream.scaladsl.{RestartSource, Sink}
import com.mchajii.sample.apm.ElasticApmAgent
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.opentracing.util.GlobalTracer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes._

final case class Balance(accountNumber: String, balance: Int)

object BalanceStream extends App {

  ElasticApmAgent.start()

  private val log = LoggerFactory.getLogger(getClass)

  implicit private val system: ActorSystem = ActorSystem("BalanceStream")

  implicit private val executionContext: ExecutionContext = system.dispatcher

  private val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("accounts-group")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  private val db = Database.forConfig("db")
  private val accounts = TableQuery[Accounts]

  private val restartSettings = RestartSettings(minBackoff = 3.seconds, maxBackoff = 30.seconds, randomFactor = 0.2)

  db.run(accounts.schema.create).onComplete { _ =>
    RestartSource
      .onFailuresWithBackoff(restartSettings) { () =>
        Transactional
          .source(
            consumerSettings,
            Subscriptions.topics("transactions")
          )
          .map { message =>
            val transactionId = message.record.key()
            val span = GlobalTracer.get().activeSpan()
            span.setTag("type", "bank-transactions")
            span.setTag("bank_transaction_id", transactionId)
            message
          }
          .asSourceWithContext(message => message.partitionOffset)
          .map { message =>
            val transactionJson = message.record.value()
            val transactionOrError = decode[BankTransaction](transactionJson)
            transactionOrError
          }
          .collect {
            case Right(transaction) => transaction
          }
          .mapAsync(1) {
            transaction =>
              findByAccountNumber(transaction.accountNumber)
                .flatMap {
                  case Some(bankAccount) =>
                    val newBalance = bankAccount.balance + transaction.amount
                    update(bankAccount.id, newBalance)
                  case None =>
                    insert(transaction.accountNumber, transaction.amount)
                }
                .map(_ => transaction.accountNumber)
          }
          .mapAsync(4) { accountNumber => findByAccountNumber(accountNumber) }
          .collect {
            case Some(bankAccount) => Balance(bankAccount.accountNumber, bankAccount.balance)
          }
          .asSource
          .map { case (balance, partitionOffset) =>
            log.info(s"Writing account's balance $balance to Kafka")
            ProducerMessage.single(
              new ProducerRecord("balances", 0, UUID.randomUUID().toString, balance.asJson.noSpaces),
              partitionOffset
            )
          }
          .instrumentedPartial(name = "BalanceStream", traceable = true)
          .via(Transactional.flow(producerSettings, "transactionalId"))
      }
      .to(Sink.ignore)
      .run()
    }

  def findByAccountNumber(accountNumber: String): Future[Option[BankAccount]] =
    db.run(accounts.filter(_.accountNumber === accountNumber).result.headOption)

  def insert(accountNumber: String, balance: Int): Future[Int] =
    db.run(accounts += BankAccount(accountNumber = accountNumber, balance = balance))

  def update(id: Option[Long], balance: Int): Future[Int] =
    db.run(accounts.filter(_.id === id).map(_.balance).update(balance))
}
