package com.cwl.spark.ml.job

import org.apache.spark.mllib.linalg.Vectors
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime
import com.cwl.spark.ml.model.ARIMA.ARIMA
import org.apache.commons.math3.util.Incrementor.MaxCountExceededCallback
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StructType, StructField, StringType, IntegerType, DoubleType}

object SaleForecastNewJob extends SparkBaseJob {

  // 期号和销售额字段格式
  case class resultset_lp(drawnum: Int,
                          saleamount: Double
                         )

  // 统计结果字段格式
  case class resultset_statis(uuid: String,
                              period: String,
                              lotterynum: Int,
                              preds_time: String,
                              provincename: String,
                              provinceid: String,
                              cityname: String,
                              cityid: String,
                              gamename: String,
                              gameid: String,
                              forecast_amount: Double,
                              true_amount: Double
                             )

  override def runJob: Unit = {
    println("start.")
    val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
    val gameidlist = gameMap.keys

    // 加载前十期销售额数据
    val dpTable_DF = hiveContext.read.jdbc(gp_url, "dataprepare", props)

    val finalRes_RDD = dpTable_DF.map { row =>
      val uuid = row.getAs[String]("uuid")
      val drawnum = row.getAs[Int]("lotterynum")
      val provincename = row.getAs[String]("provincename")
      val provinceid = row.getAs[String]("provinceid")
      val cityname = row.getAs[String]("cityname")
      val cityid = row.getAs[String]("cityid")
      val gamename = row.getAs[String]("gamename")
      val gameid = row.getAs[String]("gameid")

      var saleamount1 = row.getAs[Double]("amount1")
      var saleamount2 = row.getAs[Double]("amount2") + 1
      var saleamount3 = row.getAs[Double]("amount3") + 2
      var saleamount4 = row.getAs[Double]("amount4") + 3
      var saleamount5 = row.getAs[Double]("amount5") + 4
      var saleamount6 = row.getAs[Double]("amount6")
      var saleamount7 = row.getAs[Double]("amount7") + 1
      var saleamount8 = row.getAs[Double]("amount8") + 2
      var saleamount9 = row.getAs[Double]("amount9") + 3
      var saleamount10 = row.getAs[Double]("amount10") + 4

      println("saleamount1: ", saleamount1)
      println("saleamount2: ", saleamount2)
      println("saleamount3: ", saleamount3)
      println("saleamount4: ", saleamount4)
      println("saleamount5: ", saleamount5)
      println("saleamount6: ", saleamount6)
      println("saleamount7: ", saleamount7)
      println("saleamount8: ", saleamount8)
      println("saleamount9: ", saleamount9)
      println("saleamount10: ", saleamount10)

      // 建立模型，进行预测
      var forecast_amount = 0.0
      try {
        val ts = Vectors.dense(Array(saleamount1, saleamount2, saleamount3, saleamount4, saleamount5, saleamount6, saleamount7, saleamount8, saleamount9, saleamount10))
        val arimaModel = ARIMA.autoFit(ts)
        val valueList = arimaModel.forecast(ts, 1)
        forecast_amount = valueList(valueList.size - 1)
      } catch {
        case ex: java.lang.NullPointerException => {
          println(ex.getMessage)
        }
        case ex: MaxCountExceededCallback => {
          println(ex.getMessage)
        }
      }
      finally {
        if (forecast_amount <= 0.0) {
          forecast_amount = (saleamount1 + saleamount2 + saleamount3 + saleamount4 + saleamount5 + saleamount6 + saleamount7 + saleamount8 + saleamount9 + saleamount10) / 10
        }
      }
      val pattern = new java.text.DecimalFormat("#.##")
      forecast_amount = pattern.format(forecast_amount).toDouble

      Row(uuid, "draw", drawnum, getCurrentTime(), provincename, provinceid, cityname, cityid, gamename, gameid, forecast_amount, 0.0)
    }
    val schema = StructType(Array(
      StructField("uuid", StringType, nullable = true),
      StructField("period", StringType, nullable = true),
      StructField("lotterynum", IntegerType, nullable = true),
      StructField("preds_time", StringType, nullable = true),
      StructField("provincename", StringType, nullable = true),
      StructField("provinceid", StringType, nullable = true),
      StructField("cityname", StringType, nullable = true),
      StructField("cityid", StringType, nullable = true),
      StructField("gamename", StringType, nullable = true),
      StructField("gameid", StringType, nullable = true),
      StructField("forecast_amount", DoubleType, nullable = true),
      StructField("true_amount", DoubleType, nullable = true)
    ))
    val finalRes_DF = hiveContext.createDataFrame(finalRes_RDD, schema)

    // 保存结果
    finalRes_DF.write.mode("append").jdbc(gp_url, "saleforecast", props)
  }

  def main(args: Array[String]): Unit = {
    runJob
  }

}
