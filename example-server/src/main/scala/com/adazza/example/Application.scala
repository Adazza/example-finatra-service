package com.adazza.example

import javax.inject.Inject

import com.adazza.common.dynamodb.ServiceConfig
import com.adazza.common.{ServiceConfigId, ServiceName, ServiceVersion, Tier}
import com.adazza.ingest.thrift.ExampleService
import com.adazza.ingest.thrift.ExampleService.Ping
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.google.inject.{Provides, Singleton}
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.Controller
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.TwitterModule
import com.twitter.util.Future

trait PingPong {
  def pong(p: String): Future[String]
}

class DefaultPingPong extends PingPong {
  override def pong(p: String): Future[String] = {
    Future.value(p)
  }
}

object EnvironmentModule extends TwitterModule {
  val adazzaRegion = flag[String]("adazza.region", "AWS Region")
  val adazzaTier = flag[String]("adazza.tier", "Tier")
  val adazzaServiceName = flag[String]("adazza.service.name", "Service Name")
  val adazzaServiceVersion = flag[String]("adazza.service.version", "Service Version")

  @Singleton
  @Provides
  def providesServiceConfigId(): ServiceConfigId = {
    ServiceConfigId(Tier(adazzaTier()), ServiceName(adazzaServiceName()), ServiceVersion(adazzaServiceVersion()))
  }

  @Singleton
  @Provides
  def providesRegion(): Regions = Regions.fromName(adazzaRegion())
}

object DefaultPingPongModule extends TwitterModule {
  @Singleton
  @Provides
  def provideDefaultPingPong(): DefaultPingPong = new DefaultPingPong()
}

case class Configuration()

object ConfigurationModule extends TwitterModule {
  @Provides
  @Singleton
  def providesConfigurationn(serviceConfigId: ServiceConfigId, region: Regions): Configuration = {
    val dynamo = AmazonDynamoDBClientBuilder.standard()
      .withRegion(region)
      .build()
    ServiceConfig.get[Configuration](dynamo)(serviceConfigId)
  }
}

object ServicesModule extends TwitterModule {
   override def modules = Seq(DefaultPingPongModule)

  override def configure(): Unit = {
    bind[PingPong].to[DefaultPingPong]
  }
}

/**
  * Created by ben on 7/14/17.
  */
object Application extends ApplicationBase

class ExampleController @Inject()(pingPong: PingPong) extends Controller with ExampleService.BaseServiceIface {
  override val ping: Service[Ping.Args, String] = handle(Ping) { args: Ping.Args =>
    pingPong.pong(args.pong)
  }
}

class ApplicationBase extends ThriftServer {
  override def modules = Seq(
    ServicesModule,
    EnvironmentModule)

  override protected def configureThrift(router: ThriftRouter): Unit = {
    router.add[ExampleController]
  }
}