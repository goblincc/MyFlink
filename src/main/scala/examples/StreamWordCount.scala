package examples

//import org.apache.flink.api.scala._
//import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._ // 数据类型异常，动态数据引入

object StreamWordCount {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(8)

//    val paramTool: ParameterTool = ParameterTool.fromArgs(args)
//    val host: String = paramTool.get("host")
//    val port: Int = paramTool.getInt("port")

    val intputDataStream: DataStream[String] = env.socketTextStream("localhost", 7777)

    val resultDataStream: DataStream[(String, Int)] = intputDataStream
      .flatMap(_.split(" "))
      .filter(_.nonEmpty)
      .map((_, 1))
      .keyBy(0)
      .sum(1)
    resultDataStream.print()
    env.execute("stream word count")
  }
}
