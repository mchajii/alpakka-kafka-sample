package com.mchajii.sample

import slick.jdbc.H2Profile.api._

final case class BankAccount(id: Option[Long] = None, accountNumber: String, balance: Int)

class Accounts(tag: Tag) extends Table[BankAccount](tag, "ACCOUNTS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def accountNumber = column[String]("ACCOUNT_NUMBER", O.Unique)
  def balance = column[Int]("BALANCE")
  def * = (id.?, accountNumber, balance).mapTo[BankAccount]
}
