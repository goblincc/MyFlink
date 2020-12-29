package persona

import java.io.FileInputStream
import java.time.Duration
import java.util.{Collections, Properties}

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.{DataStream, DataStreamSource}
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.{FlinkJedisPoolConfig, FlinkJedisSentinelConfig}
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
import org.apache.kafka.clients.consumer.KafkaConsumer
import redis.clients.jedis.{Jedis, JedisSentinelPool}
import util.LoadProperty
import java.util

import com.alibaba.fastjson.{JSON, JSONObject}
import com.typesafe.scalalogging.Logger
import enums.RedisKey
import jdk.nashorn.internal.parser.JSONParser


object KafkaConnect {

  val logger = Logger(KafkaConnect.getClass)

  case class SensorReading(uid: String, time: String, event: String)

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    processKafkaEvent(env)
    env.execute("kafka data insert to redis")
  }

  def processKafkaEvent(env: StreamExecutionEnvironment): Unit ={
    val config = LoadProperty.loadProperties()
    val properties = new Properties()
    config.setProperty("bootstrap.servers", config.getProperty("kafka.bootstrap.servers.1"))
    config.setProperty("group.id", config.getProperty("kafka.group-id.1"))
    val topic = config.getProperty("kafka.topic.1")
    env.addSource(new RichSourceFunction[SensorReading] {
      var consumer: KafkaConsumer[String, String] = null

      override def open(parameters: Configuration): Unit =  {
        consumer = new KafkaConsumer[String, String](properties)
        val list = util.Arrays.asList(topic)
        consumer.subscribe(list)
      }

      override def run(sc: SourceFunction.SourceContext[SensorReading]): Unit= {
        while (true) {
          val records = consumer.poll(Duration.ofSeconds(500))
          val iter = records.iterator()
          while (iter.hasNext) {
            val record = iter.next()
            val key = record.key()
            val event = record.value()
            if("RoomJoin".equals(key)){
              try{
                val roomEvent: RoomEvent = JSON.parseObject(event, classOf[RoomEvent])
                if(roomEvent.uid.equals(roomEvent.owner)){
                  sc.collect(SensorReading(roomEvent.uid, roomEvent.timestamp, "RoomJoin"))
                }
              }catch{
                case e:Exception  => logger.error("process room join event {} error: {} ", event, e)
              }

            }else if ("RoomLeave".equals(key)){
              try{
                val roomEvent: RoomEvent = JSON.parseObject(event, classOf[RoomEvent])
                if(roomEvent.uid.equals(roomEvent.owner)){
                  sc.collect(SensorReading(roomEvent.uid, roomEvent.timestamp, "RoomLeave"))
                }
              }catch{
                case e:Exception  => logger.error("process room leave event {} error: {} ", event, e)
              }

            }
          }
        }
      }

      override def cancel(): Unit = {
      }

    }).addSink(new RichSinkFunction[SensorReading] {
      var redisCon: Jedis = _
      var pool: JedisSentinelPool = _

      override def open(parameters: Configuration): Unit = {
        super.open(parameters)
      }

      override def invoke(value: SensorReading, context: SinkFunction.Context[_]): Unit = {
        val properties: Properties = LoadProperty.loadProperties()
        val size: String = properties.getProperty("redis.num")
        //          val index: Int = Math.abs(value.key.hashCode()) % size.toInt
        val index = 0
        val masters: Array[String] = properties.getProperty("redis.address.master").split(",")
        val servers = properties.getProperty("redis.address.servers").split(",")
        val sentinels: java.util.Set[String] = new java.util.HashSet()
        sentinels.add(servers(0))
        sentinels.add(servers(1))
        sentinels.add(servers(2))
        println(sentinels.size())
        try{
          pool = new JedisSentinelPool(masters(index), sentinels)
          this.redisCon  = pool.getResource
          if(value.event.equals("RoomJoin")){
            redisCon.hset(RedisKey.OLAPARTY_ROOM_ACTIVE.toString, value.uid, value.time)
          }else if(value.event.equals("RoomLeave")){
            redisCon.hdel(RedisKey.OLAPARTY_ROOM_ACTIVE.toString, value.uid)
          }
        }catch{
          case e: Exception => logger.error("jedis handle fail e: {}", e)
        }finally {
          close()
        }
      }
      override def close(): Unit = {
        super.close()
        try{
          if (this.redisCon != null) {
            this.redisCon.close()
          }
        }catch{
          case e:Exception => logger.error(" jedis close failed e: {}", e)
        }
      }
    })
  }

  class RoomEvent {
    val roomid: String = ""
    val owner: String = ""
    val uid: String = ""
    val country: String = ""
    val timestamp: String = ""
  }

}






