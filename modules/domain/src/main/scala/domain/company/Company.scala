package domain.company

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class CompanyId(value: String)

case class CompanyName(value: String) {
  assert(value.length <= 255, "over_max_length")
}

case class Subdomain(value: String) {
  assert(value.length <= 20, "over_max_length")
}

case class Company(id: CompanyId, name: CompanyName) {}

case class CompanyWithPlan(name: CompanyName,
                           endDate: Option[DomainDate],
                           basicPlan: String) {}

case class CurrentContractGetOutput(companyName: String,
                                    endDate: Option[String])

case class DomainDate(value: LocalDate) {
  def >(other: DomainDate): Boolean = value.compareTo(other.value) > 0

  def <=(other: DomainDate): Boolean = value.compareTo(other.value) <= 0

  def minusOneDays: DomainDate = DomainDate(value.minusDays(1l))

  val formattedDate = value.format(DateTimeFormatter.ISO_DATE)
  val formattedDateInt =
    value.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt
}

object DomainDate {
  def parse(value: String) =
    DomainDate(LocalDate.parse(value, DateTimeFormatter.ISO_DATE))

  def parseInt(value: Int) =
    DomainDate(LocalDate.of(value / 10000, (value % 10000) / 100, value % 100))
}
