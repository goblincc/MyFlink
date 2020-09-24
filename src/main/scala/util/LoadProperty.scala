package util

import java.io.FileInputStream
import java.util.Properties

object LoadProperty {
  def loadProperties():Properties = {
    val properties = new Properties()
    val path = Thread.currentThread().getContextClassLoader.getResource("config.properties").getPath
    properties.load(new FileInputStream(path))
    properties
  }
}
