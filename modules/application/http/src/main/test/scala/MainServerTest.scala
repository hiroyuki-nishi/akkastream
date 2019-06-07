import org.scalatest.FunSuite

class MainServerTest extends FunSuite {
  case class Device ()
  sealed trait TriggerTarget
  case class AssetmanagementDeviceTriggerTarget(device: Device) extends TriggerTarget {

//    val osType: OsType = device.osType
  }
  sealed trait AlertTrigger
  sealed trait AssetAlertTrigger       extends AlertTrigger
  sealed trait AssetAlertTriggerPerDay extends AssetAlertTrigger
  sealed trait OperationAlertTrigger   extends AlertTrigger

  sealed abstract class TriggerSpecification(val code: String) {
    val isAlert: Boolean            = this.isInstanceOf[AlertTrigger]
    val isAssetAlert: Boolean       = this.isInstanceOf[AssetAlertTrigger]
    val isAssetAlertPerDay: Boolean = this.isInstanceOf[AssetAlertTriggerPerDay]
    val isOperationAlert: Boolean   = this.isInstanceOf[OperationAlertTrigger]
  }
  case object PasswordPolicyViolated extends TriggerSpecification("")
    with AssetAlertTrigger {}

  test("test") {
    def maybePartitionKey: Option[String] = Some("hoge")
    def maybePartitionKey2: Option[String] = None

    def getDeviceId(target: TriggerTarget): Option[String] =
    target match {
      case d: AssetmanagementDeviceTriggerTarget => Some("AssetTriggerTarget")
      case _                                     => None
    }

    def put(value: String) = {
      println(s"put-$value")
      println(value)
    }

    println(PasswordPolicyViolated.isAlert)
    maybePartitionKey
      .map(partitionKey => put(partitionKey))
      .getOrElse("None1")
    maybePartitionKey2
      .map(partitionKey => put(partitionKey))
      .getOrElse("None2")
  }

  test("HookedEvent") {
    sealed trait HookedEvent {}
    case class HookedTrigger() extends HookedEvent {}

    val h = new HookedTrigger{}
    h match {
      case s: HookedEvent => println("Hooked")
      case _ => println("Other")
    }
  }
}
