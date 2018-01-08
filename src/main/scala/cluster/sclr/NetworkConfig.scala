package cluster.sclr

import java.net.NetworkInterface

import scala.collection.JavaConverters._

object NetworkConfig {

  def hostLocalAddress(): String = {
    NetworkInterface.getNetworkInterfaces.asScala.
      find(_.getName equals "eth0").
      flatMap(interface =>
        interface.getInetAddresses.asScala.find(_.isSiteLocalAddress).map(_.getHostAddress)).
      getOrElse("127.0.0.1")
  }
}
