package com.yy.persona

import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}
import org.apache.flink.api.scala._

object wc {
  def main(args: Array[String]): Unit = {
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val inputpath = "E:\\workspaceIde\\flinkplatform\\src\\main\\resources\\hello.txt"
    val inputDataset: DataSet[String] = env.readTextFile(inputpath)
    val value: AggregateDataSet[(String, Int)] = inputDataset
      .flatMap(_.split(" "))
      .map((_, 1))
      .groupBy(0)
      .sum(1)
    value.print()

  }
}
