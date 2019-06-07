package domain

import java.util.UUID

case class Partition(uuid: UUID)
object Partition {
  def create = Partition(UUID.randomUUID())
}
