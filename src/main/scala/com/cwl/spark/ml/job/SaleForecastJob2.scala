package com.cwl.spark.ml.job

import java.text.SimpleDateFormat

import com.cwl.spark.ml.utils.GetUUID.getUUID
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime
import com.cwl.spark.ml.model.ARIMA.ARIMA
import org.apache.commons.math3.util.Incrementor.MaxCountExceededCallback
import java.util.{Calendar, Random}


/**
  * Created by wdc on 2017/8/9.
  * 按周预测
  */
object SaleForecastJob2 extends SparkBaseJob{
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

  def getDate(year: Int, weeknum: Int): Array[String] = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.WEEK_OF_YEAR, weeknum)
    cal.set(Calendar.DAY_OF_WEEK, 2) // 1表示周日，2表示周一，7表示周六
    val mondaydate = cal.getTime()

    val cal2 = Calendar.getInstance()
    cal2.setTime(mondaydate)
    cal2.add(Calendar.DAY_OF_MONTH, +6)
    val sundaydate = cal2.getTime()

    val format = new SimpleDateFormat("yyyy-MM-dd")
    val Mdate = format.format(mondaydate)
    val Sdate = format.format(sundaydate)
    return Array(Mdate, Sdate)
  }

  def getSaleAmount(cityid: String, gameid: String): Double ={
    var saleamount = 0.0
    val sql_avggameamount = String.format("select avg(amount) as avggameamount from (select sum(drawsaleamount) as amount from drawsaleTable where cityid = '%s' and gameid = '%s' GROUP BY saletime) a", cityid, gameid)
    log.info("sql_avggameamount: "+sql_avggameamount)
    val avggameamount = hiveContext.sql(sql_avggameamount).first().getAs[java.math.BigDecimal]("avggameamount")
    println("avggameamount: "+avggameamount)
//    if(avggameamount == null){
//      val sql_avgcityamount = String.format("select avg(amount) as avgcityamount from (select sum(drawsaleamount) as amount, gamename,saledrawnumber from drawsaleTable where cityid = '%s' group by saledrawnumber, gamename) a", cityid)
//      log.info("sql_avgcityamount: "+sql_avgcityamount)
//      val avgcityamount = hiveContext.sql(sql_avgcityamount).first().getAs[java.math.BigDecimal]("avgcityamount").toString.toDouble
//      saleamount = avgcityamount
//    }else{
//      saleamount = avggameamount.toString.toDouble
//    }
//    return saleamount
    return (avggameamount.toString.toDouble) * 7
  }

  def main(args: Array[String]): Unit = {
    runJob
  }

  override def runJob: Unit ={

    // 获取当前日期在这一年属于哪个周
    val c=Calendar.getInstance()
    val cur_weekNum = c.get(Calendar.WEEK_OF_YEAR)
    val year = c.get(Calendar.YEAR)
    println(cur_weekNum)

    val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
    val gameidlist = gameMap.keys
    val weeknumlist = List(cur_weekNum)

    val drawsale_DF = hiveContext.read.jdbc(gp_url, "drawsalegrandtotal", props)
    drawsale_DF.registerTempTable("drawsaleTable")
    val cityidlist = hiveContext.sql("select distinct(cityid) as cityid from drawsaleTable").collectAsList()
    for(i <- 0 until cityidlist.size()) {
      val cityid = cityidlist.get(i).getAs[String]("cityid")
      if (cityid != null) {
        log.info("cityid: " + cityid)
        for (gameid <- gameidlist) {
          log.info("gameid: " + gameid)
          for(weeknum <- weeknumlist) {
            log.info("weeknum: " + weeknum)
            val weeknum1 = weeknum - 5
            val weeknum2 = weeknum - 4
            val weeknum3 = weeknum - 3
            val weeknum4 = weeknum - 2
            val weeknum5 = weeknum - 1
            val dateArray1 = getDate(year, weeknum1)
            val dateArray2 = getDate(year, weeknum2)
            val dateArray3 = getDate(year, weeknum3)
            val dateArray4 = getDate(year, weeknum4)
            val dateArray5 = getDate(year, weeknum5)
            val sql1 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saletime >= '%s' and saletime <= '%s'", cityid, gameid, dateArray1(0), dateArray1(1))
            val sql2 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saletime >= '%s' and saletime <= '%s'", cityid, gameid, dateArray2(0), dateArray2(1))
            val sql3 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saletime >= '%s' and saletime <= '%s'", cityid, gameid, dateArray3(0), dateArray3(1))
            val sql4 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saletime >= '%s' and saletime <= '%s'", cityid, gameid, dateArray4(0), dateArray4(1))
            val sql5 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saletime >= '%s' and saletime <= '%s'", cityid, gameid, dateArray5(0), dateArray5(1))
            println("sql1: "+sql1)
            println("sql2: "+sql2)
            println("sql3: "+sql3)
            println("sql4: "+sql4)
            var saleamount1 = 0.00
            var saleamount2 = 0.00
            var saleamount3 = 0.00
            var saleamount4 = 0.00
            var saleamount5 = 0.00
            val saleamount1_tmp = hiveContext.sql(sql1).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount2_tmp = hiveContext.sql(sql2).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount3_tmp = hiveContext.sql(sql3).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount4_tmp = hiveContext.sql(sql4).first().getAs[java.math.BigDecimal]("saleamount")
            val saleamount5_tmp = hiveContext.sql(sql5).first().getAs[java.math.BigDecimal]("saleamount")
            println("saleamount1_tmp: "+saleamount1_tmp)
            println("saleamount2_tmp: "+saleamount2_tmp)
            println("saleamount3_tmp: "+saleamount3_tmp)
            println("saleamount4_tmp: "+saleamount4_tmp)
            println("saleamount5_tmp: "+saleamount5_tmp)
            if(saleamount1_tmp == null){
              saleamount1 = getSaleAmount(cityid, gameid) - new Random().nextDouble()
            }else{
              saleamount1 = saleamount1_tmp.toString.toDouble
            }

            if(saleamount2_tmp == null){
              saleamount2 = getSaleAmount(cityid, gameid) + new Random().nextDouble()
            }else{
              saleamount2 = saleamount2_tmp.toString.toDouble
            }

            if(saleamount3_tmp == null){
              saleamount3 = getSaleAmount(cityid, gameid) - new Random().nextDouble()
            }else{
              saleamount3 = saleamount3_tmp.toString.toDouble
            }

            if(saleamount4_tmp == null){
              saleamount4 = getSaleAmount(cityid, gameid) + new Random().nextDouble()
            }else{
              saleamount4 = saleamount4_tmp.toString.toDouble
            }

            if(saleamount5_tmp == null){
              saleamount5 = getSaleAmount(cityid, gameid) - new Random().nextDouble()
            }else{
              saleamount5 = saleamount5_tmp.toString.toDouble
            }
            println("saleamount1: "+saleamount1)
            println("saleamount2: "+saleamount2)
            println("saleamount3: "+saleamount3)
            println("saleamount4: "+saleamount4)
            println("saleamount5: "+saleamount5)

            // 建立模型，进行预测
            var forecast_amount = 0.0
            try{
              val ts = Vectors.dense(Array(saleamount1, saleamount2, saleamount3, saleamount4, saleamount5))
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
                forecast_amount = (saleamount1+saleamount2+saleamount3+saleamount4+saleamount5)/5
              }
            }
            val pattern = new java.text.DecimalFormat("#.##")
            forecast_amount = pattern.format(forecast_amount).toDouble
            println("forecast_amount: " + forecast_amount)

            // 获取真实销售额
            val curdateArray = getDate(year, cur_weekNum)
            val sql6 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saletime >= '%s' and saletime <= '%s'", cityid, gameid, curdateArray(0), curdateArray(1))
            log.info("sql6: "+sql6)
            val true_amount_tmp = hiveContext.sql(sql6).first().getAs[java.math.BigDecimal]("saleamount")
            var true_amount = 0.0
            if(true_amount_tmp == null){
              true_amount = 0
            }else{
              true_amount = true_amount_tmp.toString.toDouble
            }

            // 保存结果
            val sql7 = String.format("select provinceid, provincename, cityname from drawsaleTable where cityid = '%s'", cityid)
            log.info("sql7: "+sql7)
            val provinceid = hiveContext.sql(sql7).first().getAs[String]("provinceid")
            val provincename = hiveContext.sql(sql7).first().getAs[String]("provincename")
            val cityname = hiveContext.sql(sql7).first().getAs[String]("cityname")
            val gamename = gameMap(gameid)
            val uuid = getUUID()
            val statis_res = List(resultset_statis(uuid, "week", year*1000+cur_weekNum, getCurrentTime().split(" ")(0), provincename, provinceid, cityname, cityid, gamename, gameid, forecast_amount, true_amount))
            val res_DF = hiveContext.createDataFrame(statis_res)
            res_DF.show()
//            res_DF.write.mode("append").jdbc(gp_url, "saleforecast", props)
          }
        }
      }
    }

  }
}
