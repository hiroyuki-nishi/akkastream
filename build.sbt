import Dependencies._
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest
import jp.pigumer.sbt.cloud.aws.cloudformation.{Alias, AwscfSettings, CloudformationStack, Stacks}
import jp.pigumer.sbt.cloud.aws.ecr.{AwsecrCommands, AwsecrCredential}

lazy val commonSettings = Seq(
  organization := "nishi",
  scalaVersion := "2.12.8",
  scalacOptions := Seq(
    "-deprecation",
    "-feature"
  ),
  scalafmtOnCompile in ThisBuild := true,
)

lazy val contextName = "akkastream"
val EnvName: String = sys.env.getOrElse("ENV_NAME", "it")
val Region = sys.env.getOrElse("AWS_REGION", "ap-northeast-1")
lazy val sts = {
  AWSSecurityTokenServiceClientBuilder.standard.withRegion(Region).build
}
lazy val AccountId: String = {
  sts.getCallerIdentity(new GetCallerIdentityRequest()).getAccount
}
val ECRAccount = ""
val BucketName: String = "xxx"
val CredentialsProvider =
  new AWSCredentialsProviderChain(
    new STSAssumeRoleSessionCredentialsProvider.Builder(
      "xxx",
      "xxx"
    ).build())
val RoleARN: String = s"xxx"
val ClusterSuffix: String = EnvName match {
  case e => e
}

val ecrLogin = taskKey[Unit]("login")
val ecrPush = taskKey[Unit]("push")

lazy val root = (project in file("."))
  .enablePlugins(CloudformationPlugin)
  .settings(commonSettings: _*)
  .aggregate(appHttp, akka)
  .settings(name := "root").
  settings(
    awscfSettings in Global := AwscfSettings(region = Region,
      bucketName = Some(BucketName),
      projectName = "akkastream",
      templates = Some(file("cloudformation/ecs")),
      credentialsProvider = CredentialsProvider,
      roleARN = Some(RoleARN)),
    awscfStacks := {
      val log = streams.value.log
      Stacks(
        Alias("api-service") -> {
          val domain = s"$ECRAccount.dkr.ecr.$Region.amazonaws.com"
          val repositoryName = "akkastream-service"
          val imageVersion = (version in Docker in appHttp).value
          CloudformationStack(
            stackName = "akkastream-service",
            template = "api.yaml",
            parameters = Map("EnvName" -> EnvName,
              "Image" -> s"$domain/$repositoryName:$imageVersion",
              "ContextName" -> "v1",
              "ClusterSuffix" -> ClusterSuffix,
              "ClusterName" -> s"spa-api-cluster-$ClusterSuffix"
            )
          )
        }
      )
    }
  ).
  settings(
    ecrLogin := {
      val request = new GetAuthorizationTokenRequest().withRegistryIds(ECRAccount)
      val result = awsecr.value.getAuthorizationToken(request)
      val domain = s"$ECRAccount.dkr.ecr.$Region.amazonaws.com"
      val docker = (awsecrDockerPath in awsecr).value
      val credential = AwsecrCredential(result)
      val loginCommand = docker :: "login" :: "-u" :: credential.user :: "-p" :: credential.password :: s"https://$domain" :: Nil
      val cmd = loginCommand.mkString(" ")
      sys.process.Process(cmd) ! match {
        case 0 ⇒ cmd
        case _ ⇒ sys.error(s"Login failed. Command: $cmd")
      }
    },
    ecrPush := {
      def pushAppHttp: Unit = {
        val docker = (awsecrDockerPath in awsecr).value
        val domain = s"$ECRAccount.dkr.ecr.$Region.amazonaws.com"
        val imageName = (name in appHttp).value
        val imageVersion = (version in Docker).value
        val repositoryName = "spa/akkastream-akka-service"
        val source = s"$imageName:$imageVersion"
        val target = s"$domain/$repositoryName:$imageVersion"

        AwsecrCommands.tag(docker, source, target)
        AwsecrCommands.push(docker, target)
        ()
      }

      pushAppHttp
    }
  )

lazy val domain = (project in file("modules/domain")).settings(commonSettings: _*)

lazy val appHttp = (project in file("./modules/application/http")).
  enablePlugins(JavaAppPackaging, AshScriptPlugin, DockerPlugin).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= ScalaTest ++ awsSqs
  ).
  settings(
    name := "lspan-application-http",
    libraryDependencies ++= appHttpDeps,
    mainClass in Compile := Some("MainServer"),
    version in Docker := "1.0.0",
    dockerBaseImage := "java:openjdk-8-jdk",
    dockerExposedPorts := Seq(8080)
  )

lazy val akka = (project in file("./modules/application/akka")).
  enablePlugins(JavaAppPackaging, AshScriptPlugin, DockerPlugin).
  dependsOn(domain, infraDynamoDB).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= ScalaTest ++ awsSqs ++ akkaAlpakkaSqs ++ appHttpDeps ++ awsSts
      ++ infraAkkaStremConfigDeps ++ akkaStreamTestKit ++ kantanCsvGeneric ++ betterFiles ++ openCsv
  ).
  settings(
    name := "lspan-akka",
    mainClass in Compile := Some("company.Main"),
    version in Docker := "1.0.0",
    dockerBaseImage := "java:openjdk-8-jdk",
    dockerExposedPorts := Seq(8080)
  )

lazy val infraSQS = (project in file("./modules/adapter/infrastructure/sqs")).
  dependsOn().
  settings(commonSettings: _*).
  settings(
    name := s"$contextName-infrastructure-sqs",
    libraryDependencies ++= ScalaTest ++ awsSqs ++ sprayJson
  )

lazy val infraDynamoDB = (project in file("./modules/adapter/infrastructure/dynamodb")).
  dependsOn(domain).
  settings(commonSettings: _*).
  settings(
    name := s"$contextName-infrastructure-dynamodb",
    libraryDependencies ++= ScalaTest ++ awsDynamoDB ++ sprayJson
  )

lazy val infraKinesis = (project in file("./modules/adapter/infrastructure/kinesis")).
  dependsOn(domain).
  settings(commonSettings: _*).
  settings(
    name := s"$contextName-infrastructure-kinesis",
    libraryDependencies ++= ScalaTest ++ awsKinesis ++ sprayJson
  )

