package examples

import org.apache.flink.streaming.api.scala._

case class SensorReading(id: String, timestamp: Long, temperature: Double)
object SourceTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val datalist = List(
      SensorReading("sensor_1", 1547718199, 35.8 ),
      SensorReading("sensor_2", 1547718120, 37.8 ),
      SensorReading("sensor_3", 1547718188, 32.8 )
    )
    val stream1 = env.fromCollection(datalist)
    stream1.print()
    env.execute("source test")
  }
}
