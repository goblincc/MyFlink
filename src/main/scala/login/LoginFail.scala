package login

import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer


case class LoginEvent(userId: Long, ip: String, eventType: String, timestamp: Long)

case class LoginFailWarning(userId: Long, firstFailTime: Long, lastFailTime: Long, warningMsg: String)

object LoginFail {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val resource = getClass.getResource("/LoginLog.csv")
    val inputStream = env.readTextFile(resource.getPath)

    val loginEventStream = inputStream
      .map(data=>{
        val arr = data.split(",")
        LoginEvent(arr(0).toLong, arr(1), arr(2), arr(3).toLong)
      })
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[LoginEvent](Time.seconds(3)) {
        override def extractTimestamp(element: LoginEvent): Long = element.timestamp * 1000L
      })

    val loginFailWarningStream = loginEventStream
      .keyBy(_.userId)
      .process(new LoginFailWarningResult(2))

    loginFailWarningStream.print()
    env.execute("login fail job")
  }
}

class LoginFailWarningResult(failTimes: Int) extends KeyedProcessFunction[Long, LoginEvent, LoginFailWarning]{
  lazy val loginFailListState: ListState[LoginEvent] = getRuntimeContext.getListState(new ListStateDescriptor[LoginEvent]("login-fail", classOf[LoginEvent]))
  lazy val timerTsState: ValueState[Long] = getRuntimeContext.getState(new ValueStateDescriptor[Long]("time-ts", classOf[Long]))

  override def processElement(value: LoginEvent, ctx: KeyedProcessFunction[Long, LoginEvent, LoginFailWarning]#Context, out: Collector[LoginFailWarning]): Unit = {
    if(value.eventType == "fail"){
        loginFailListState.add(value)
      if(timerTsState.value() == 0){
        //注册一个2s的定时器
        val ts = value.timestamp * 1000L + 2000L
        ctx.timerService().registerEventTimeTimer(ts)
        timerTsState.update(ts)
      }
    }else{
      // 如果登录成功， 那么直接清空状态和定时器，重新开始
      ctx.timerService().deleteEventTimeTimer(timerTsState.value())
      loginFailListState.clear()
      timerTsState.clear()
    }
  }

  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, LoginEvent, LoginFailWarning]#OnTimerContext, out: Collector[LoginFailWarning]): Unit = {

    val allLoginFailList: ListBuffer[LoginEvent] = new ListBuffer[LoginEvent]

    val iter = loginFailListState.get.iterator()
    while(iter.hasNext){
      allLoginFailList += iter.next()
    }
    //判断登录失败的次数， 如果超过次数则报警
    if(allLoginFailList.length >= failTimes){
        out.collect(LoginFailWarning(allLoginFailList.head.userId, allLoginFailList.head.timestamp, allLoginFailList.last.timestamp, "login fail in 2s for " + allLoginFailList.length + " times."))
    }

    loginFailListState.clear()
    timerTsState.clear()

  }
}
