package com.cwl.spark.ml.job

import com.cwl.spark.ml.utils.GetUUID.getUUID
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime
import com.cwl.spark.ml.model.ARIMA.ARIMA
import org.apache.commons.math3.util.Incrementor.MaxCountExceededCallback

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
    val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
    val gameidlist = gameMap.keys
    //    val drawnumlist = List(2017057, 2017058, 2017059, 2017060, 2017061, 2017062, 2017063, 2017064, 2017065, 2017066)

    // 加载前十期销售额数据
    val drawsale_DF = hiveContext.read.jdbc(gp_url, "dataprepare", props)
    drawsale_DF.registerTempTable("dpTable")
    val citylist = hiveContext.sql("select distinct(cityid) as cityid from dpTable where cityid is not null").collectAsList()
    log.info("citylist: " + citylist)


    val statis_res = List(resultset_statis("init", "", 1, getCurrentTime(), "", "", "", "", "", "", 2, 2))
    var allRes_DF = hiveContext.createDataFrame(statis_res)

    for (row <- drawsale_DF) {
      val drawnum = row.getAs[Int]("lotterynum")
      val cityid = row.getAs[String]("cityid")
      val gameid = row.getAs[String]("gameid")
      //    for(i <- 0 until citylist.size()){
      //      val cityid = citylist.get(i).getAs[String]("cityid")
      //      if(cityid != null){
      //        log.info("city: " + cityid)
      //        for(gameid <- gameidlist){
      //          log.info("gameid: " + gameid)
      //          for(drawnum <- drawnumlist){
      log.info("drawnum: " + drawnum)
      val sql_1 = String.format("select amount1, amount2, amount3, amount4, amount5, amount6, amount7, amount8, amount9, amount10 from dpTable where cityid = '%s' and gameid = '%s' and lotterynum = '%s' order by preds_time desc limit 1", cityid, gameid, drawnum.toString)
      val amountRow = hiveContext.sql(sql_1).first()

      // 通过前10期的销售额预测该期
      var saleamount1 = amountRow.getAs[Double]("amount1")
      var saleamount2 = amountRow.getAs[Double]("amount2")
      var saleamount3 = amountRow.getAs[Double]("amount3")
      var saleamount4 = amountRow.getAs[Double]("amount4")
      var saleamount5 = amountRow.getAs[Double]("amount5")
      var saleamount6 = amountRow.getAs[Double]("amount6")
      var saleamount7 = amountRow.getAs[Double]("amount7")
      var saleamount8 = amountRow.getAs[Double]("amount8")
      var saleamount9 = amountRow.getAs[Double]("amount9")
      var saleamount10 = amountRow.getAs[Double]("amount10")

      log.info("saleamount1: " + saleamount1)
      log.info("saleamount2: " + saleamount2)
      log.info("saleamount3: " + saleamount3)
      log.info("saleamount4: " + saleamount4)
      log.info("saleamount5: " + saleamount5)
      log.info("saleamount6: " + saleamount6)
      log.info("saleamount7: " + saleamount7)
      log.info("saleamount8: " + saleamount8)
      log.info("saleamount9: " + saleamount9)
      log.info("saleamount10: " + saleamount10)
      val train_data = List(resultset_lp(1, saleamount1),
        resultset_lp(2, saleamount2),
        resultset_lp(3, saleamount3),
        resultset_lp(4, saleamount4),
        resultset_lp(5, saleamount5),
        resultset_lp(6, saleamount6),
        resultset_lp(7, saleamount7),
        resultset_lp(8, saleamount8),
        resultset_lp(9, saleamount9),
        resultset_lp(10, saleamount10)
      )
      val train_DF = hiveContext.createDataFrame(train_data)
      val parsedData = train_DF.map { row =>
        LabeledPoint(row.getAs[Double]("saleamount"), Vectors.dense(row.getAs[Int]("drawnum")))
      }

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

      // get true amount
      hiveContext.read.jdbc(gp_url, "drawsalegrandtotal", props).registerTempTable("drawsaleTable")
      val sql_2 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum.toString)
      log.info("sql_2: " + sql_2)
      val true_amount_tmp = hiveContext.sql(sql_2).first().getAs[java.math.BigDecimal]("saleamount")
      var true_amount = 0.0
      if (true_amount_tmp == null) {
        true_amount = 0
      } else {
        true_amount = true_amount_tmp.toString.toDouble
      }

      // 保存结果
      val sql_3 = String.format("select provinceid, provincename, cityname from dpTable where cityid = '%s' limit 1", cityid)
      log.info("sql_3: " + sql_3)
      val dataRow = hiveContext.sql(sql_3).first()
      var provinceid = ""
      var provincename = ""
      var cityname = ""
      try {
        if (dataRow.get(0) != null) {
          provinceid = dataRow.getAs[String]("provinceid")
        }
        if (dataRow.get(1) != null) {
          provincename = dataRow.getAs[String]("provincename")
        }
        if (dataRow.get(2) != null) {
          cityname = dataRow.getAs[String]("cityname")
        }
      }
      val gamename = gameMap(gameid)
      val uuid = getUUID()
      //            val statis_res = List(resultset_statis(uuid, "draw", drawnum.toInt, getCurrentTime().split(" ")(0), provincename, provinceid, cityname, cityid, gamename, gameid, forecast_amount, true_amount))
      val statis_res = List(resultset_statis(uuid, "draw", drawnum, getCurrentTime(), provincename, provinceid, cityname, cityid, gamename, gameid, forecast_amount, true_amount))
      val res_DF = hiveContext.createDataFrame(statis_res)
      allRes_DF = allRes_DF.unionAll(res_DF)
      //          }
      //        }
      //      }
    }
    allRes_DF = allRes_DF.filter("uuid != 'init'")
    allRes_DF.write.mode("append").jdbc(gp_url, "saleforecast", props)
  }

  def main(args: Array[String]): Unit = {
    runJob
  }

}
