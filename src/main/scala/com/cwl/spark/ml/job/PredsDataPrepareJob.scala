package com.cwl.spark.ml.job

import com.cwl.spark.ml.utils.GetUUID.getUUID
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime
import org.postgresql.util.PSQLException
import com.cwl.spark.ml.utils.DBHelper.updatedataToPostgresql

object PredsDataPrepareJob extends SparkBaseJob {

  // 统计结果字段格式
  case class dataInfo(uuid: String,
                      period: String,
                      lotterynum: Int,
                      preds_time: String,
                      provincename: String,
                      provinceid: String,
                      cityname: String,
                      cityid: String,
                      gamename: String,
                      gameid: String,
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

  def getSaleAmount(cityid: String, gameid: String): Double = {
    var saleamount = 0.0
    val sql_avggameamount = String.format("select avg(amount) as avggameamount from (select sum(drawsaleamount) as amount, saledrawnumber from drawsaleTable where cityid = '%s' and gameid = '%s' GROUP BY saledrawnumber) a", cityid, gameid)
    log.info("sql_avggameamount: " + sql_avggameamount)
    val avggameamount = hiveContext.sql(sql_avggameamount).first().getAs[java.math.BigDecimal]("avggameamount")
    if (avggameamount == null) {
      val sql_avgcityamount = String.format("select avg(amount) as avgcityamount from (select sum(drawsaleamount) as amount, gamename,saledrawnumber from drawsaleTable where cityid = '%s' group by saledrawnumber, gamename) a", cityid)
      log.info("sql_avgcityamount: " + sql_avgcityamount)
      val avgcityamount = hiveContext.sql(sql_avgcityamount).first().getAs[java.math.BigDecimal]("avgcityamount").toString.toDouble
      saleamount = avgcityamount
    } else {
      saleamount = avggameamount.toString.toDouble
    }
    return saleamount
  }

  override def runJob: Unit = {
    // 判断dataprepare表是否存在
    var dpTableExists = 1
    try {
      hiveContext.read.jdbc(gp_url, "dataprepare", props)
    } catch {
      case ex: PSQLException => {
        dpTableExists = 0
      }
    }
    if (dpTableExists == 0) {
      val data_res = List(dataInfo("init", "draw", 1, getCurrentTime(), "", "", "", "", "", "", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
      var allRes_DF = hiveContext.createDataFrame(data_res)
      hiveContext.read.jdbc(gp_url, "drawgame", props).registerTempTable("drawgameTable")
      val cur_time = getCurrentTime()
      val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
      val gameidlist = gameMap.keys
      val drawsale_DF = hiveContext.read.jdbc(gp_url, "drawsalegrandtotal", props)
      drawsale_DF.registerTempTable("drawsaleTable")
      val game_maxdrawnum_Map = hiveContext.sql("select gameid, max(saledrawnumber) as drawnumber from drawsaleTable group by gameid").select("gameid", "drawnumber").map { row => (row.getAs[String]("gameid"), row.getAs[String]("drawnumber")) }.collectAsMap()
//      val game_maxdrawnum_Map = Map("10001"->"2017100", "10003" -> "2017100")
      val citylist = hiveContext.sql("select distinct(cityid) as cityid from drawsaleTable where cityid is not null").collectAsList()
      log.info("citylist: " + citylist)

      var totalfrontdrawinfo:Map[List[String], String] = Map()
      for (gameid <- gameidlist) {
        var drawnum = 1
        val maxdrawnum = game_maxdrawnum_Map(gameid).toString.toInt
        val sql_periodinfo = String.format("select count(*)  as num from drawgameTable where gameid = '%s'  and drawnumber = '%s' and (drawbegintime<'%s' and drawendtime >'%s')", gameid, maxdrawnum.toString, cur_time, cur_time)
        val periodInfo = hiveContext.sql(sql_periodinfo).first().getAs[Long]("num")
        println("periodInfo: ", periodInfo)
        if (periodInfo == 0) {
          drawnum = maxdrawnum + 1
        } else {
          drawnum = maxdrawnum
        }
        for (i <- 0 until citylist.size()) {
          val cityid = citylist.get(i).getAs[String]("cityid")

          // 通过前10期的销售额预测该期
          val drawnum1 = (drawnum - 10).toString
          val drawnum2 = (drawnum - 9).toString
          val drawnum3 = (drawnum - 8).toString
          val drawnum4 = (drawnum - 7).toString
          val drawnum5 = (drawnum - 6).toString
          val drawnum6 = (drawnum - 5).toString
          val drawnum7 = (drawnum - 4).toString
          val drawnum8 = (drawnum - 3).toString
          val drawnum9 = (drawnum - 2).toString
          val drawnum10 = (drawnum - 1).toString
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
          log.info("sql1: " + sql1)
          log.info("sql2: " + sql2)
          log.info("sql3: " + sql3)
          log.info("sql4: " + sql4)
          log.info("sql5: " + sql5)
          log.info("sql6: " + sql6)
          log.info("sql7: " + sql7)
          log.info("sql8: " + sql8)
          log.info("sql9: " + sql9)
          log.info("sql10: " + sql10)
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
          if (saleamount1_tmp == null) {
            saleamount1 = getSaleAmount(cityid, gameid) - 1
          } else {
            saleamount1 = saleamount1_tmp.toString.toDouble
          }

          if (saleamount2_tmp == null) {
            saleamount2 = getSaleAmount(cityid, gameid) + 1
          } else {
            saleamount2 = saleamount2_tmp.toString.toDouble
          }

          if (saleamount3_tmp == null) {
            saleamount3 = getSaleAmount(cityid, gameid) - 1
          } else {
            saleamount3 = saleamount3_tmp.toString.toDouble
          }

          if (saleamount4_tmp == null) {
            saleamount4 = getSaleAmount(cityid, gameid) + 1
          } else {
            saleamount4 = saleamount4_tmp.toString.toDouble
          }

          if (saleamount5_tmp == null) {
            saleamount5 = getSaleAmount(cityid, gameid) - 1
          } else {
            saleamount5 = saleamount5_tmp.toString.toDouble
          }

          if (saleamount6_tmp == null) {
            saleamount6 = getSaleAmount(cityid, gameid) + 1
          } else {
            saleamount6 = saleamount6_tmp.toString.toDouble
          }

          if (saleamount7_tmp == null) {
            saleamount7 = getSaleAmount(cityid, gameid) - 1
          } else {
            saleamount7 = saleamount7_tmp.toString.toDouble
          }

          if (saleamount8_tmp == null) {
            saleamount8 = getSaleAmount(cityid, gameid) + 1
          } else {
            saleamount8 = saleamount8_tmp.toString.toDouble
          }

          if (saleamount9_tmp == null) {
            saleamount9 = getSaleAmount(cityid, gameid) - 1
          } else {
            saleamount9 = saleamount9_tmp.toString.toDouble
          }

          if (saleamount10_tmp == null) {
            saleamount10 = getSaleAmount(cityid, gameid) + 1
          } else {
            saleamount10 = saleamount10_tmp.toString.toDouble
          }
          log.info(drawnum1 + "的销售额为: " + saleamount1)
          log.info(drawnum2 + "的销售额为: " + saleamount2)
          log.info(drawnum3 + "的销售额为: " + saleamount3)
          log.info(drawnum4 + "的销售额为: " + saleamount4)
          log.info(drawnum5 + "的销售额为: " + saleamount5)
          log.info(drawnum6 + "的销售额为: " + saleamount6)
          log.info(drawnum7 + "的销售额为: " + saleamount7)
          log.info(drawnum8 + "的销售额为: " + saleamount8)
          log.info(drawnum9 + "的销售额为: " + saleamount9)
          log.info(drawnum10 + "的销售额为: " + saleamount10)

          // 保存结果
          val sql12 = String.format("select provinceid, provincename, cityname from drawsaleTable where cityid = '%s' limit 1", cityid)
          log.info("sql12: " + sql12)
          val dataRow = hiveContext.sql(sql12).first()
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
          val data_res = List(dataInfo(uuid, "draw", drawnum, getCurrentTime(), provincename, provinceid, cityname, cityid, gamename, gameid, saleamount1, saleamount2, saleamount3, saleamount4, saleamount5, saleamount6, saleamount7, saleamount8, saleamount9, saleamount10))
          val res_DF = hiveContext.createDataFrame(data_res)
          allRes_DF = allRes_DF.unionAll(res_DF)

          // 记录该城市该游戏该期前一期的销售额
          val sql_2 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, (drawnum - 1).toString)
          //      log.info("sql_2: " + sql_2)
          val true_amount_tmp = hiveContext.sql(sql_2).first().getAs[java.math.BigDecimal]("saleamount")
          var true_amount = 0.0
          if (true_amount_tmp == null) {
            true_amount = 0
          } else {
            true_amount = true_amount_tmp.toString.toDouble
          }
          println("cityid: ", cityid, "gameid: ", gameid, "lotterynum: ", drawnum-1, "true_amount: ", true_amount)
          val frontdrawinfo = List(cityid, gameid, (drawnum-1).toString, true_amount.toString)
          totalfrontdrawinfo += (frontdrawinfo -> "a")
        }
      }
      allRes_DF = allRes_DF.filter("uuid != 'init'")
      allRes_DF.write.mode("overwrite").jdbc(gp_url, "dataPrepare", props)

      // 更新saleforecast表中前一期销售额
      println("totalfrontdrawinfo: ", totalfrontdrawinfo)
      updatedataToPostgresql(totalfrontdrawinfo, gp_url)
    }
    else {
      hiveContext.read.jdbc(gp_url, "drawgame", props).registerTempTable("drawgameTable")
      val cur_time = getCurrentTime()
      val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
      val gameidlist = gameMap.keys
      val drawsale_DF = hiveContext.read.jdbc(gp_url, "drawsalegrandtotal", props)
      drawsale_DF.registerTempTable("drawsaleTable")
      val game_maxdrawnum_Map = hiveContext.sql("select gameid, max(saledrawnumber) as drawnumber from drawsaleTable group by gameid").select("gameid", "drawnumber").map { row => (row.getAs[String]("gameid"), row.getAs[String]("drawnumber")) }.collectAsMap()
//      val game_maxdrawnum_Map = Map("10001"->"2017100", "10003" -> "2017100")
      val cityidlist = hiveContext.sql("select distinct(cityid) as cityid from drawsaleTable where cityid is not null").collectAsList()
      log.info("citylist: " + cityidlist)
      val recentdraw_res = List(dataInfo("init", "", 1, "", "", "", "", "", "", "", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
      var allrecentdrawinfo_DF = hiveContext.createDataFrame(recentdraw_res)
      var totalfrontdrawinfo:Map[List[String], String] = Map()
      for (gameid <- gameidlist) {
        val maxdrawnum = game_maxdrawnum_Map(gameid).toInt
        var drawnum = 1
        val sql_periodinfo = String.format("select count(*)  as num from drawgameTable where gameid = '%s'  and drawnumber = '%s' and (drawbegintime<'%s' and drawendtime >'%s')", gameid, maxdrawnum.toString, cur_time, cur_time)
        val periodInfo = hiveContext.sql(sql_periodinfo).first().getAs[Long]("num")

        // 如果期结则更新dataprepare表数据，同时更新saleforecast表中上一期彩票销售额
        if (periodInfo == 0) {
          val datapre_DF = hiveContext.read.jdbc(gp_url, "dataprepare", props)
          drawnum = maxdrawnum + 1
          for (i <- 0 until cityidlist.size()) {
            val cityid = cityidlist.get(i).getAs[String]("cityid")
            val sql_maxdrawnumamount = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, maxdrawnum.toString)
            log.info("sql_maxdrawnumamount: " + sql_maxdrawnumamount)
            var saleamountrecent = 0.00
            val saleamountrecent_tmp = hiveContext.sql(sql_maxdrawnumamount).first().getAs[java.math.BigDecimal]("saleamount")
            if (saleamountrecent_tmp == null) {
              saleamountrecent = getSaleAmount(cityid, gameid) - 1
            } else {
              saleamountrecent = saleamountrecent_tmp.toString.toDouble
            }
            if (datapre_DF.filter(String.format("cityid = '%s' and gameid = '%s'", cityid, gameid)).count() == 0) {
              val sql12 = String.format("select provinceid, provincename, cityname from drawsaleTable where cityid = '%s' limit 1", cityid)
              log.info("sql12: " + sql12)
              val dataRow = hiveContext.sql(sql12).first()
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
              val avgsaleamount = getSaleAmount(cityid, gameid)
              val recentdrawInfo = List(dataInfo(getUUID(), "draw", drawnum, cur_time, provincename, provinceid, cityname, cityid, gamename, gameid, avgsaleamount, avgsaleamount, avgsaleamount, avgsaleamount, avgsaleamount, avgsaleamount, avgsaleamount, avgsaleamount, avgsaleamount, saleamountrecent))
              val recentdrawinfo_DF = hiveContext.createDataFrame(recentdrawInfo)
              allrecentdrawinfo_DF = allrecentdrawinfo_DF.unionAll(recentdrawinfo_DF)
            } else {
              val cityinfo = datapre_DF.filter(String.format("cityid = '%s' and gameid = '%s'", cityid, gameid)).first()
              val recentdrawInfo = List(dataInfo(cityinfo.getAs[String]("uuid"), "draw", drawnum, cur_time, cityinfo.getAs[String]("provincename"),
                cityinfo.getAs[String]("provinceid"), cityinfo.getAs[String]("cityname"), cityinfo.getAs[String]("cityid"),
                cityinfo.getAs[String]("gamename"), cityinfo.getAs[String]("gameid"), cityinfo.getAs[Double]("amount2"),
                cityinfo.getAs[Double]("amount3"), cityinfo.getAs[Double]("amount4"), cityinfo.getAs[Double]("amount5"),
                cityinfo.getAs[Double]("amount6"), cityinfo.getAs[Double]("amount7"), cityinfo.getAs[Double]("amount8"),
                cityinfo.getAs[Double]("amount9"), cityinfo.getAs[Double]("amount10"), saleamountrecent))
              val recentdrawinfo_DF = hiveContext.createDataFrame(recentdrawInfo)
              allrecentdrawinfo_DF = allrecentdrawinfo_DF.unionAll(recentdrawinfo_DF)
            }
            // 记录该城市该游戏上一期的销售总额
            val sql_2 = String.format("select sum(drawsaleamount) as saleamount from drawsaleTable where cityid = '%s' and gameid = '%s' and saledrawnumber = '%s'", cityid, gameid, (drawnum - 1).toString)
            log.info("sql_2: " + sql_2)
            val true_amount_tmp = hiveContext.sql(sql_2).first().getAs[java.math.BigDecimal]("saleamount")
            var true_amount = 0.0
            if (true_amount_tmp == null) {
              true_amount = 0
            } else {
              true_amount = true_amount_tmp.toString.toDouble
            }
            println("cityid: ", cityid, "gameid: ", gameid, "lotterynum: ", drawnum-1, "true_amount: ", true_amount)
            val frontdrawinfo = List(cityid, gameid, (drawnum-1).toString, true_amount.toString)
            totalfrontdrawinfo += (frontdrawinfo -> "a")
          }
        }
      }
      // 获取dataprepare表中无需更新的数据，然后拼接保存
      val dpdata_DF = hiveContext.read.jdbc(gp_url, "dataprepare", props)
      dpdata_DF.registerTempTable("dpTable")
      allrecentdrawinfo_DF.registerTempTable("updateTable")
      val tmp_DF = hiveContext.sql(String.format("select a.*, b.uuid as uuid2 from dpTable a left join updateTable b on a.gameid = b.gameid and a.cityid = b.cityid"))
      val othergame_DF = tmp_DF.filter("uuid2 is null").drop("uuid2")
      val finalRes_DF = allrecentdrawinfo_DF.unionAll(othergame_DF).filter("uuid != 'init'")
      finalRes_DF.write.mode("overwrite").jdbc(gp_url, "dataprepare", props)

      //更新前一期销售额
      println("totalfrontdrawinfo: ", totalfrontdrawinfo)
      updatedataToPostgresql(totalfrontdrawinfo, gp_url)
    }
  }

  def main(args: Array[String]): Unit = {
    runJob
  }

}
