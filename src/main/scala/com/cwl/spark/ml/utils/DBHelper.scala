package com.cwl.spark.ml.utils

import java.sql.{DriverManager, ResultSet}

import com.cwl.spark.ml.job.PredictJob.{gp_url, hiveContext, props}
import org.apache.spark.sql._

/**
  * Created by wdc on 2017/4/17.
  */
object DBHelper{
  Class.forName("com.mysql.jdbc.Driver").newInstance()
  Class.forName("org.postgresql.Driver").newInstance()

  def getdataFromMySQL(table: String, conn_mysql: String, sqlContext: SQLContext): DataFrame ={
    val mysql_df = sqlContext.jdbc(conn_mysql, table)
    return mysql_df
  }
  def getdataFromPostgresql(table: String, sqlContext: SQLContext, conn_pg: String): DataFrame ={
    val pg_df = sqlContext.jdbc(conn_pg, table)
    return pg_df
  }

  def insertdataToPostgresql(param: Map[String, Any], gp_url: String): Unit ={
    val insertSql = s"""INSERT INTO dm_modelInfo (
                       | model_record_uuid,
                       | model_type,
                       | algorithm,
                       | num_trees,
                       | max_depth,
                       | modelnumber,
                       | runtype,
                       | runuser,
                       | start_time,
                       | end_time,
                       | take_time,
                       | model_file_path)
                       | VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""".stripMargin.replaceAll("\\n", "")
    val conn = DriverManager.getConnection(gp_url)
    val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)

    // do database insert
    try {
      val prep = conn.prepareStatement(insertSql)
      prep.setString(1, param.get("uuid").get.toString)
      prep.setString(2, param.get("model_type").get.toString)
      prep.setString(3, param.get("algorithm").get.toString)
      prep.setInt(4, param.get("numTrees").get.toString.toInt)
      prep.setInt(5, param.get("maxDepth").get.toString.toInt)
      prep.setString(6, param.get("modelnumber").get.toString)
      prep.setString(7, param.get("runtype").get.toString)
      prep.setString(8, param.get("runuser").get.toString)
      prep.setTimestamp(9, new java.sql.Timestamp(param.get("start_time").get.toString.toLong))
      prep.setTimestamp(10, new java.sql.Timestamp(param.get("end_time").get.toString.toLong))
      prep.setInt(11, param.get("take_time").get.toString.toInt)
      prep.setString(12, param.get("model_file_path").get.toString)
      prep.executeUpdate()
    }
    finally {
      conn.close()
    }
  }

  def getPGData(sql: String): ResultSet ={
    val conn = DriverManager.getConnection(gp_url)
    val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
    try{
     val rs = statement.executeQuery(sql)
     return rs
   }finally {
     conn.close()
   }
  }

  def main(args: Array[String]): Unit = {
//    println("gp_url: "+gp_url)
//    hiveContext.read.jdbc(gp_url,"antiaddiction_result",props).registerTempTable("t1")
//    hiveContext.sql("select max(preds_time) as preds_time, account from t1 group by account").registerTempTable("t2")
//    val accAndstatus = hiveContext.sql("select t1.account as account, t1.status as status from t1 join t2 on t1.account = t2.account").select("account", "status").map{row=> (row.getAs[String]("account"), row.getAs[Int]("status"))}.collectAsMap()
  }
}
