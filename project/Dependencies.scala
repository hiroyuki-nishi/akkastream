import sbt._

object Dependencies {
  val AkkaStreamVersion = "2.6.10"

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.2.1"
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaStreamVersion
  val akkaAlpakkaSqs = Seq("com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "2.0.2")
   "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "2.0.2"
  val akkaStreamTestKit = Seq("com.typesafe.akka" %% "akka-stream-testkit" % AkkaStreamVersion % Test)

  val awsSDKVersion = "1.11.906"
  val awsSDKCore = Seq("com.amazonaws" % "aws-java-sdk-core" % awsSDKVersion)
  val awsSqs = Seq("com.amazonaws" % "aws-java-sdk-sqs" % awsSDKVersion)
  val awsDynamoDB = Seq("com.amazonaws" % "aws-java-sdk-dynamodb" % awsSDKVersion)
  val awsKinesis = Seq("com.amazonaws" % "aws-java-sdk-kinesis" % awsSDKVersion)

  val sprayJson = Seq("io.spray" %%  "spray-json" % "1.3.5")

  val appHttpDeps = Seq(
    akkaHttp,
    akkaStream
  )

  //log
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaStreamVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

  val infraAkkaStremConfigDeps = Seq(
    akkaStream,
    akkaSlf4j,
    logback
  )

  lazy val ScalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.2.3" % Test
  )
}
