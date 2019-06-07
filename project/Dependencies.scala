import sbt._

object Dependencies {
  val AkkaStreamVersion = "2.5.21"

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.1.7"
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaStreamVersion
  val akkaAlpakkaSqs = Seq("com.lightbend.akka" %% "akka-stream-alpakka-sqs" % "1.0.0")
  val akkaStreamTestKit = Seq("com.typesafe.akka" %% "akka-stream-testkit" % AkkaStreamVersion % Test)

  val awsSDKVersion = "1.11.+"
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
//  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.25"
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaStreamVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

  val infraAkkaStremConfigDeps = Seq(
    akkaStream,
    akkaSlf4j,
//    slf4j,
    logback
  )

  lazy val ElasticSearchDepends = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-aws" % "6.2.8",
    "com.sksamuel.elastic4s" %% "elastic4s-spray-json" % "6.2.8"
  )

  lazy val ScalaTest = Seq(
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
}
