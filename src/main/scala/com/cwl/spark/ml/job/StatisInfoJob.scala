package com.cwl.spark.ml.job

import com.cwl.spark.ml.utils.GetUUID.getUUID
import com.cwl.spark.ml.utils.TimeHelper.getCurrentTime
/**
  * Created by wdc on 2017/6/7.
  * 统计出防沉迷前端展示的字段
  */
object StatisInfoJob extends SparkBaseJob{
  case class resultset(uuid:String,
                       run_date: String,
                       provinceid:String,
                       provincename:String,
                       normalnum:Long,
                       mildlynum:Long,
                       moderatenum:Long,
                       heavynum:Long,
                       alertnum:Long
                      )

  override def runJob: Unit = {
    val cur_date = getCurrentTime().split(" ")(0)
    val antiaddiction_DF = sqlContext.read.jdbc(gp_url,"antiaddiction_result",props)
    val province_DF = antiaddiction_DF.select("provincename").distinct()
    val provinceList = province_DF.collectAsList()
    antiaddiction_DF.registerTempTable("antiaddiction_table")
    val sql = String.format("select count(*) as count , provincename, prediction from antiaddiction_table where preds_time like '%s%s%s' GROUP BY provincename, prediction", "%", cur_date, "%")
    val statisinfo_DF = sqlContext.sql(sql)
    for(i <- 0 until provinceList.size()){
      var normalnum = 0.toLong
      var mildlynum = 0.toLong
      var moderatenum = 0.toLong
      var heavynum = 0.toLong
      var alertnum = 0.toLong
      val uuid = getUUID()
      val provincename = provinceList.get(i).getAs[String]("provincename")
      val provinceid = antiaddiction_DF.filter(String.format("provincename = '%s'", provincename)).first().getAs[String]("provinceid")
      try{
        normalnum = statisinfo_DF.filter(String.format("provincename = '%s' and prediction = '%s'", provincename, "正常")).first().getAs[Long]("count")
      }catch {
        case ex: java.util.NoSuchElementException =>{
          log.info(ex.getMessage)
        }
      }
      try{
        mildlynum = statisinfo_DF.filter(String.format("provincename = '%s' and prediction = '%s'", provincename, "轻度沉迷")).first().getAs[Long]("count")
      }catch {
        case ex: java.util.NoSuchElementException =>{
          log.info(ex.getMessage)
        }
      }
      try{
        moderatenum = statisinfo_DF.filter(String.format("provincename = '%s' and prediction = '%s'", provincename, "中度沉迷")).first().getAs[Long]("count")
      }catch {
        case ex: java.util.NoSuchElementException =>{
          log.info(ex.getMessage)
        }
      }
      try{
        heavynum = statisinfo_DF.filter(String.format("provincename = '%s' and prediction = '%s'", provincename, "重度沉迷")).first().getAs[Long]("count")
      }catch {
        case ex: java.util.NoSuchElementException =>{
          log.info(ex.getMessage)
        }
      }
      try{
        alertnum = antiaddiction_DF.filter(String.format("provincename = '%s' and preds_time like '%s%s%s' and status > 2", provincename, "%", cur_date, "%")).count()
      }catch {
        case ex: java.util.NoSuchElementException =>{
          log.info(ex.getMessage)
        }
      }
      val res = List(resultset(uuid, cur_date, provinceid, provincename, normalnum, mildlynum, moderatenum, heavynum, alertnum))
      val statis_df = sqlContext.createDataFrame(res)
      statis_df.write.mode("append").jdbc(gp_url, "antiaddiction_rststatis", props)
    }
  }

  def main(args: Array[String]): Unit = {
    runJob
  }
}
