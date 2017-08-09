package com.cwl.spark.ml.utils

import java.util.Date
import java.text.SimpleDateFormat
import java.sql.Timestamp
import java.util.Calendar

import com.cwl.spark.ml.job.SaleForecastLRJob.{gp_url, hiveContext, log, props}

/**
  * Created by wdc on 2017/3/23.
  */
object TimeHelper {
  def getCurrentTime(): String ={
    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
    val time = df.format(new Date())
//    return time.replace("-","").replace(" ", "").replace(":","")
    return time
  }

  def caldiffTime(stime: String, etime: String): Long ={
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val startTime = sdf.parse(stime).getTime
    val endTime = sdf.parse(etime).getTime
    val diffTime = endTime - startTime
    return diffTime
  }

  def getDateBefore(stime: String, day: Int): String ={
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val d = sdf.parse(stime)
    val now =Calendar.getInstance()
    now.setTime(d)
    now.set(Calendar.DATE,now.get(Calendar.DATE)-day)
    return sdf.format(now.getTime())
  }

  def transStringToTimeStamp(time: String): Long ={
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    var ts = new Timestamp(System.currentTimeMillis())
    try {
      if (time == "")
        return 0
      else {
        val d = format.parse(time)
        val t = new Timestamp(d.getTime)
        return t.getTime
      }
    } catch {
      case e: Exception => log.info("cdr parse timestamp wrong")
    }
    return 0
  }

  def getSaleAmount(city: String, game: String): Double ={
    var saleamount = 0.0
      val sql4 = String.format("select avg(drawsaleamount) as avggameamount from drawsaleTable where cityname = '%s' and gamename = '%s'", city, game)
      log.info("sql4: "+sql4)
      val avggameamount = hiveContext.sql(sql4).first().getAs[java.math.BigDecimal]("avggameamount")
      if(avggameamount == null){
        val sql5 = String.format("select avg(drawsaleamount) as avgcityamount from drawsaleTable where cityname = '%s'", city)
        log.info("sql5: "+sql5)
        val avgcityamount = hiveContext.sql(sql5).first().getAs[java.math.BigDecimal]("avgcityamount").toString.toDouble
        saleamount = avgcityamount
      }else{
        saleamount = avggameamount.toString.toDouble
      }
    return saleamount
  }

  def main(args: Array[String]): Unit = {
    val drawsale_DF = hiveContext.read.jdbc(gp_url, "drawsalegrandtotal", props)
    drawsale_DF.registerTempTable("drawsaleTable")
    val saleamount = getSaleAmount("兰州市", "七乐彩")
//    val time = "2017-03-23"
//    println(getDateBefore(time, 0))
//    println(transStringToTimeStamp(time).toString.toLong)
//    println(getCurrentTime().split(" ")(0).replace("-",""))
  }

}
