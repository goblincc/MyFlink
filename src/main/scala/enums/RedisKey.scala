package enums

object RedisKey extends Enumeration {
  type RedisKey = Value
  val OLAPARTY_ROOM_ACTIVE = Value(" HagoParty:room_active")
  def main(args: Array[String]): Unit = {
    println(RedisKey.OLAPARTY_ROOM_ACTIVE.toString)
  }
}
