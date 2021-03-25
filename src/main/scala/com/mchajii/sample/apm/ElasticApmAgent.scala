package com.mchajii.sample.apm

import co.elastic.apm.attach.ElasticApmAttacher
import scala.jdk.CollectionConverters._

object ElasticApmAgent {

  private val configuration: Map[String, String] = Map(
    "service-name" -> "bank",
    "enable_log_correlation" -> "true",
    "application_packages" -> "com.mchajii.sample",
    "log_level" -> "INFO",
    "disable_instrumentations" -> "kafka"
  )

  def start(): Unit =
    ElasticApmAttacher.attach(configuration.asJava)
}
