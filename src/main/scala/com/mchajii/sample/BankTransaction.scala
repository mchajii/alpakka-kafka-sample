package com.mchajii.sample

final case class BankTransaction(
  accountNumber: String,
  amount: Int,
  timestamp: Long = System.currentTimeMillis()
)
