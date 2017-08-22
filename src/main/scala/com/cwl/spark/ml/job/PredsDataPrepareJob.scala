package com.cwl.spark.ml.job

import com.cwl.spark.ml.utils.GetUUID.getUUID
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime

object PredsDataPrepareJob extends SparkBaseJob{
  // 期号和销售额字段格式
  case class resultset_lp(drawnum:Int,
                          saleamount: Double
                         )
  // 统计结果字段格式
  case class dataInfo(uuid:String,
                      period: String,
                      lotterynum: Int,
                      preds_time:String,
                      provincename:String,
                      provinceid:String,
                      cityname:String,
                      cityid:String,
                      gamename:String,
                      gameid:String,
                      amount1: Double,
                      amount2: Double,
                      amount3: Double,
                      amount4: Double,
                      amount5: Double,
                      amount6: Double,
                      amount7: Double,
                      amount8: Double,
                      amount9: Double,
                      amount10: Double
                     )

  def getSaleAmount(cityid: String, gameid: String): Double ={
    var saleamount = 0.0
    val sql_avggameamount = String.format("select avg(amount) as avggameamount from (select sum(drawsaleamount) as amount, saledrawnumber from drawsaleTable where cityid = '%s' and gameid = '%s' GROUP BY saledrawnumber) a", cityid, gameid)
    log.info("sql_avggameamount: "+sql_avggameamount)
    val avggameamount = hiveContext.sql(sql_avggameamount).first().getAs[java.math.BigDecimal]("avggameamount")
    if(avggameamount == null){
      val sql_avgcityamount = String.format("select avg(amount) as avgcityamount from (select sum(drawsaleamount) as amount, gamename,saledrawnumber from drawsaleTable where cityid = '%s' group by saledrawnumber, gamename) a", cityid)
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
    val citylist = hiveContext.sql("select distinct(cityid) as cityid from drawsaleTable where cityid is not null").collectAsList()
    log.info("citylist: "+citylist)

    val data_res = List(dataInfo("init", "draw", 1, getCurrentTime(), "", "", "", "", "", "", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
    var allRes_DF = hiveContext.createDataFrame(data_res)

    for(i <- 0 until citylist.size()){
      val cityid = citylist.get(i).getAs[String]("cityid")
      if(cityid != null){
        log.info("cityid: " + cityid)
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
            val sql1 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum1)
            val sql2 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum2)
            val sql3 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum3)
            val sql4 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum4)
            val sql5 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum5)
            val sql6 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum6)
            val sql7 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum7)
            val sql8 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum8)
            val sql9 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum9)
            val sql10 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, drawnum10)
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
              saleamount1 = getSaleAmount(cityid, gameid)-1
            }else{
              saleamount1 = saleamount1_tmp.toString.toDouble
            }

            if(saleamount2_tmp == null){
              saleamount2 = getSaleAmount(cityid, gameid)+1
            }else{
              saleamount2 = saleamount2_tmp.toString.toDouble
            }

            if(saleamount3_tmp == null){
              saleamount3 = getSaleAmount(cityid, gameid)-1
            }else{
              saleamount3 = saleamount3_tmp.toString.toDouble
            }

            if(saleamount4_tmp == null){
              saleamount4 = getSaleAmount(cityid, gameid)+1
            }else{
              saleamount4 = saleamount4_tmp.toString.toDouble
            }

            if(saleamount5_tmp == null){
              saleamount5 = getSaleAmount(cityid, gameid)-1
            }else{
              saleamount5 = saleamount5_tmp.toString.toDouble
            }

            if(saleamount6_tmp == null){
              saleamount6 = getSaleAmount(cityid, gameid)+1
            }else{
              saleamount6 = saleamount6_tmp.toString.toDouble
            }

            if(saleamount7_tmp == null){
              saleamount7 = getSaleAmount(cityid, gameid)-1
            }else{
              saleamount7 = saleamount7_tmp.toString.toDouble
            }

            if(saleamount8_tmp == null){
              saleamount8 = getSaleAmount(cityid, gameid)+1
            }else{
              saleamount8 = saleamount8_tmp.toString.toDouble
            }

            if(saleamount9_tmp == null){
              saleamount9 = getSaleAmount(cityid, gameid)-1
            }else{
              saleamount9 = saleamount9_tmp.toString.toDouble
            }

            if(saleamount10_tmp == null){
              saleamount10 = getSaleAmount(cityid, gameid)+1
            }else{
              saleamount10 = saleamount10_tmp.toString.toDouble
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

            // 保存结果
            val sql12 = String.format("select provinceid, provincename, cityname from drawsaleTable where cityid = '%s' limit 1", cityid)
            log.info("sql12: "+sql12)
            val dataRow = hiveContext.sql(sql12).first()
            var provinceid = ""
            var provincename = ""
            var cityname = ""
            try{
              if(dataRow.get(0) != null){
                provinceid = dataRow.getAs[String]("provinceid")
              }
              if(dataRow.get(1) != null){
                provincename = dataRow.getAs[String]("provincename")
              }
              if(dataRow.get(2) != null){
                cityname = dataRow.getAs[String]("cityname")
              }
            }
            val gamename = gameMap(gameid)
            val uuid = getUUID()
            val data_res = List(dataInfo(uuid, "draw", drawnum, getCurrentTime(), provincename, provinceid, cityname, cityid, gamename, gameid, saleamount1, saleamount2, saleamount3, saleamount4, saleamount5, saleamount6, saleamount7, saleamount8, saleamount9, saleamount10))
            val res_DF = hiveContext.createDataFrame(data_res)
            allRes_DF = allRes_DF.unionAll(res_DF)
          }
        }
      }
    }
    allRes_DF = allRes_DF.filter("uuid != 'init'")
    allRes_DF.write.mode("overwrite").jdbc(gp_url, "dataPrepare", props)
  }
  def main(args: Array[String]): Unit = {
    runJob
  }

}
