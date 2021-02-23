package hotItem

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, Table}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.scala._



object HotItemsWithSql {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val inputStream = env.readTextFile("F:\\workspace\\MyFlink\\src\\main\\resources\\UserBehavior.csv")

    val dataStream: DataStream[UserBehavior] = inputStream
      .map(data => {
        var arr = data.split(",")

        UserBehavior(arr(0).toLong, arr(1).toLong, arr(2).toInt, arr(3), arr(4).toLong)
      })
        .assignAscendingTimestamps(_.timestamp)

    val settings = EnvironmentSettings
      .newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()

    val tableEnv: StreamTableEnvironment = StreamTableEnvironment.create(env, settings)


    val dataTable: Table = tableEnv.fromDataStream(dataStream, 'itemId, 'behavior, 'timestamp.rowtime as 'ts)


  }
}
