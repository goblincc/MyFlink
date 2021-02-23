package hotItem

import java.sql.Timestamp

import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.java.tuple.{Tuple, Tuple1}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer



object HotItems {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val inputStream: DataStream[String] = env.readTextFile("F:\\workspace\\MyFlink\\src\\main\\resources\\UserBehavior.csv")
    val dataStream = inputStream
      .map(data =>{
        val arr = data.split(",")
        UserBehavior(arr(0).toLong, arr(1).toLong, arr(2).toInt, arr(3), arr(4).toLong)
      })
      .assignAscendingTimestamps(_.timestamp * 1000L)

    val aggStream: DataStream[ItemViewCount] = dataStream
      .filter(_.behavior == "pv")
      .keyBy("itemId")
      .timeWindow(Time.hours(1), Time.minutes(5))
      .aggregate(new CountAgg(), new ItemViewWindowResult())

    val resultStream = aggStream
      .keyBy("windowEnd")
      .process(new TopNHotItems(5))


    resultStream.print()

    env.execute("top")
  }
}

class CountAgg() extends AggregateFunction[UserBehavior, Long, Long]{
  override def createAccumulator(): Long = 0L

  override def add(value: UserBehavior, accumulator: Long): Long = accumulator + 1

  override def getResult(accumulator: Long): Long = accumulator

  override def merge(a: Long, b: Long): Long = a + b
}

class ItemViewWindowResult() extends WindowFunction[Long, ItemViewCount, Tuple, TimeWindow]{
  override def apply(key: Tuple, window: TimeWindow, input: Iterable[Long], out: Collector[ItemViewCount]): Unit = {
    val itemId = key.asInstanceOf[Tuple1[Long]].f0
    val windowEnd = window.getEnd
    val count = input.iterator.next()
    out.collect(ItemViewCount(itemId, windowEnd, count))
  }
}

class TopNHotItems(topSize: Int) extends KeyedProcessFunction[Tuple, ItemViewCount, String]{
  //定义状态ListState
  private var itemViewCountListState: ListState[ItemViewCount] = _

  override def open(parameters: Configuration): Unit = {
    itemViewCountListState = getRuntimeContext.getListState(new ListStateDescriptor[ItemViewCount]("itemViewCount-list", classOf[ItemViewCount]))
  }

  override def processElement(value: ItemViewCount, ctx: KeyedProcessFunction[Tuple, ItemViewCount, String]#Context, out: Collector[String]): Unit = {
    itemViewCountListState.add(value)
    //注册定时器
    ctx.timerService().registerEventTimeTimer(value.windowEnd + 1)
  }


  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Tuple, ItemViewCount, String]#OnTimerContext, out: Collector[String]): Unit = {
    val allItemViewCounts: ListBuffer[ItemViewCount] = ListBuffer()
    val iter = itemViewCountListState.get.iterator()
    while(iter.hasNext){
      allItemViewCounts += iter.next()
    }

    itemViewCountListState.clear()

    val sortedItemViewCounts = allItemViewCounts.sortBy(_.count)(Ordering.Long.reverse).take(topSize)

    val result: StringBuilder = new StringBuilder
    result.append("窗口结束时间：").append(new Timestamp(timestamp - 1)).append("\n")
    for(i <- sortedItemViewCounts.indices){
      val currentItemViewCount = sortedItemViewCounts(i)
      result.append("NO").append(i + 1).append(": ")
        .append("商品ID=").append(currentItemViewCount.itemId).append("\t")
        .append("热门度=").append(currentItemViewCount.count).append("\n")
    }

    result.append("============================\n\n")
    Thread.sleep(1000)
    out.collect(result.toString())

  }


}

case class UserBehavior(userId: Long, itemId: Long, categoryId: Int, behavior: String, timestamp: Long)

case class ItemViewCount(itemId: Long, windowEnd: Long, count: Long)
