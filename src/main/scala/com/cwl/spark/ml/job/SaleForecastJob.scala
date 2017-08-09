package com.cwl.spark.ml.job

import com.cwl.spark.ml.utils.GetUUID.getUUID
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime
import com.cwl.spark.ml.model.ARIMA.ARIMA
import org.apache.commons.math3.util.Incrementor.MaxCountExceededCallback
/**
  * Created by wdc on 2017/6/10.
  */
object SaleForecastJob extends SparkBaseJob{
  // 期号和销售额字段格式
  case class resultset_lp(drawnum:Int,
                          saleamount: Double
                         )
  // 统计结果字段格式
  case class resultset_statis(uuid:String,
                              period: String,
                              lotterynum: Int,
                              preds_time:String,
                              provincename:String,
                              provinceid:String,
                              cityname:String,
                              cityid:String,
                              gamename:String,
                              gameid:String,
                              forecast_amount: Double,
                              true_amount: Double
                             )

  def getSaleAmount(city: String, gameid: String): Double ={
    var saleamount = 0.0
    val sql_avggameamount = String.format("select avg(amount) as avggameamount from (select sum(drawsaleamount) as amount, saledrawnumber from drawsaleTable where cityname = '%s' and gameid = '%s' GROUP BY saledrawnumber) a", city, gameid)
    log.info("sql_avggameamount: "+sql_avggameamount)
    val avggameamount = hiveContext.sql(sql_avggameamount).first().getAs[java.math.BigDecimal]("avggameamount")
    if(avggameamount == null){
      val sql_avgcityamount = String.format("select avg(amount) as avgcityamount from (select sum(drawsaleamount) as amount, gamename,saledrawnumber from drawsaleTable where cityname = '%s' group by saledrawnumber, gamename) a", city)
      log.info("sql_avgcityamount: "+sql_avgcityamount)
      val avgcityamount = hiveContext.sql(sql_avgcityamount).first().getAs[java.math.BigDecimal]("avgcityamount").toString.toDouble
      saleamount = avgcityamount
    }else{
      saleamount = avggameamount.toString.toDouble
    }
    return saleamount
  }

  override def runJob: Unit ={
    val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
    val gameidlist = gameMap.keys
    val drawnumlist = List(2017057, 2017058, 2017059, 2017060, 2017061, 2017062, 2017063, 2017064, 2017065, 2017066)
    val drawsale_DF = hiveContext.read.jdbc(gp_url, "drawsalegrandtotal", props)
    drawsale_DF.registerTempTable("drawsaleTable")
    val citylist = hiveContext.sql("select distinct(cityname) as cityname from drawsaleTable").collectAsList()
    log.info("citylist: "+citylist)
    for(i <- 0 until citylist.size()){
      val city = citylist.get(i).getAs[String]("cityname")
      if(city != null){
        log.info("city: " + city)
        for(gameid <- gameidlist){
          log.info("gameid: " + gameid)
          for(drawnum <- drawnumlist){
            log.info("drawnum: "+ drawnum)
            // 通过前10期的销售额预测该期
            val drawnum1 = (drawnum - 1).toString
            val drawnum2 = (drawnum - 2).toString
            val drawnum3 = (drawnum - 3).toString
            val drawnum4 = (drawnum - 4).toString
            val drawnum5 = (drawnum - 5).toString
            val drawnum6 = (drawnum - 6).toString
            val drawnum7 = (drawnum - 7).toString
            val drawnum8 = (drawnum - 8).toString
            val drawnum9 = (drawnum - 9).toString
            val drawnum10 = (drawnum - 10).toString
            val sql1 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum1)
            val sql2 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum2)
            val sql3 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum3)
            val sql4 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum4)
            val sql5 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum5)
            val sql6 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum6)
            val sql7 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum7)
            val sql8 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum8)
            val sql9 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum9)
            val sql10 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum10)
            log.info("sql1: "+sql1)
            log.info("sql2: "+sql2)
            log.info("sql3: "+sql3)
            log.info("sql4: "+sql4)
            log.info("sql5: "+sql5)
            log.info("sql6: "+sql6)
            log.info("sql7: "+sql7)
            log.info("sql8: "+sql8)
            log.info("sql9: "+sql9)
            log.info("sql10: "+sql10)
            var saleamount1 = 0.00
            var saleamount2 = 0.00
            var saleamount3 = 0.00
            var saleamount4 = 0.00
            var saleamount5 = 0.00
            var saleamount6 = 0.00
            var saleamount7 = 0.00
            var saleamount8 = 0.00
            var saleamount9 = 0.00
            var saleamount10 = 0.00
            val saleamount1_tmp = hiveContext.sql(sql1).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount2_tmp = hiveContext.sql(sql2).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount3_tmp = hiveContext.sql(sql3).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount4_tmp = hiveContext.sql(sql4).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount5_tmp = hiveContext.sql(sql5).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount6_tmp = hiveContext.sql(sql6).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount7_tmp = hiveContext.sql(sql7).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount8_tmp = hiveContext.sql(sql8).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount9_tmp = hiveContext.sql(sql9).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount10_tmp = hiveContext.sql(sql10).first().getAs[java.math.BigDecimal]("saleamount")
            if(saleamount1_tmp == null){
              saleamount1 = getSaleAmount(city, gameid)-1
            }else{
              saleamount1 = saleamount1_tmp.toString.toDouble
            }

            if(saleamount2_tmp == null){
              saleamount2 = getSaleAmount(city, gameid)+1
            }else{
              saleamount2 = saleamount2_tmp.toString.toDouble
            }

            if(saleamount3_tmp == null){
              saleamount3 = getSaleAmount(city, gameid)-1
            }else{
              saleamount3 = saleamount3_tmp.toString.toDouble
            }

            if(saleamount4_tmp == null){
              saleamount4 = getSaleAmount(city, gameid)+1
            }else{
              saleamount4 = saleamount4_tmp.toString.toDouble
            }

            if(saleamount5_tmp == null){
              saleamount5 = getSaleAmount(city, gameid)-1
            }else{
              saleamount5 = saleamount5_tmp.toString.toDouble
            }

            if(saleamount6_tmp == null){
              saleamount6 = getSaleAmount(city, gameid)+1
            }else{
              saleamount6 = saleamount6_tmp.toString.toDouble
            }

            if(saleamount7_tmp == null){
              saleamount7 = getSaleAmount(city, gameid)-1
            }else{
              saleamount7 = saleamount7_tmp.toString.toDouble
            }

            if(saleamount8_tmp == null){
              saleamount8 = getSaleAmount(city, gameid)+1
            }else{
              saleamount8 = saleamount8_tmp.toString.toDouble
            }

            if(saleamount9_tmp == null){
              saleamount9 = getSaleAmount(city, gameid)-1
            }else{
              saleamount9 = saleamount9_tmp.toString.toDouble
            }

            if(saleamount10_tmp == null){
              saleamount10 = getSaleAmount(city, gameid)+1
            }else{
              saleamount10 = saleamount10_tmp.toString.toDouble
            }
            println("saleamount1: " + saleamount1)
            println("saleamount2: " + saleamount2)
            println("saleamount3: " + saleamount3)
            println("saleamount4: " + saleamount4)
            println("saleamount5: " + saleamount5)
            println("saleamount6: " + saleamount6)
            println("saleamount7: " + saleamount7)
            println("saleamount8: " + saleamount8)
            println("saleamount9: " + saleamount9)
            println("saleamount10: " + saleamount10)
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
            val parsedData = train_DF.map{ row =>
              LabeledPoint(row.getAs[Double]("saleamount"), Vectors.dense(row.getAs[Int]("drawnum")))
            }

            // 建立模型，进行预测
            var forecast_amount = 0.0
            try{
              val ts = Vectors.dense(Array(saleamount1, saleamount2, saleamount3, saleamount4, saleamount5, saleamount6, saleamount7, saleamount8, saleamount9, saleamount10))
//            val arimaModel = ARIMA.fitModel(1,0,1,ts)
              val arimaModel = ARIMA.autoFit(ts)
              val valueList = arimaModel.forecast(ts, 1)
              forecast_amount = valueList(valueList.size-1)
            } catch {
              case ex: java.lang.NullPointerException=>{
                println(ex.getMessage)
              }
              case ex: MaxCountExceededCallback=>{
                println(ex.getMessage)
              }
            }
            finally {
              if(forecast_amount<=0.0){
               forecast_amount = (saleamount1+saleamount2+saleamount3+saleamount4+saleamount5+saleamount6+saleamount7+saleamount8+saleamount9+saleamount10)/10
              }
            }
            val pattern = new java.text.DecimalFormat("#.##")
            forecast_amount = pattern.format(forecast_amount).toDouble


            // get true amount
            val sql11 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityname = '%s' and gameid = '%s' and saledrawnumber = '%s'", city, gameid, drawnum.toString)
            log.info("sql11: "+sql11)
            val true_amount_tmp = hiveContext.sql(sql11).first().getAs[java.math.BigDecimal]("saleamount")
            var true_amount = 0.0
            if(true_amount_tmp == null){
              true_amount = 0
            }else{
              true_amount = true_amount_tmp.toString.toDouble
            }
            log.info(drawnum1+ "的销售额为: " + saleamount1)
            log.info(drawnum2+ "的销售额为: " + saleamount2)
            log.info(drawnum3+ "的销售额为: " + saleamount3)
            log.info(drawnum4+ "的销售额为: " + saleamount4)
            log.info(drawnum5+ "的销售额为: " + saleamount5)
            log.info(drawnum6+ "的销售额为: " + saleamount6)
            log.info(drawnum7+ "的销售额为: " + saleamount7)
            log.info(drawnum8+ "的销售额为: " + saleamount8)
            log.info(drawnum9+ "的销售额为: " + saleamount9)
            log.info(drawnum10+ "的销售额为: " + saleamount10)
            log.info(drawnum + "的预测销售额为: "+ forecast_amount)
            log.info(drawnum + "的真实销售额为: "+ true_amount)

            // 保存结果
            val sql12 = String.format("select provinceid, provincename, cityid from drawsaleTable where cityname = '%s'", city)
            log.info("sql12: "+sql12)
            val provinceid = hiveContext.sql(sql12).first().getAs[String]("provinceid")
            val provincename = hiveContext.sql(sql12).first().getAs[String]("provincename")
            val cityid = hiveContext.sql(sql12).first().getAs[String]("cityid")
            val gamename = gameMap(gameid)
            val uuid = getUUID()
            val statis_res = List(resultset_statis(uuid, "draw", drawnum, getCurrentTime().split(" ")(0), provincename, provinceid, city, cityid, gamename, gameid, forecast_amount, true_amount))
            val res_DF = hiveContext.createDataFrame(statis_res)
            res_DF.write.mode("append").jdbc(gp_url, "saleforecast", props)
          }
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    runJob
  }
}
