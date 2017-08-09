package com.cwl.spark.ml.job

import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.SQLContext
//import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.spark.SparkConf
//import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.spark.sql.Row
import org.apache.commons.logging.LogFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.DriverManager

//import com.cwl.spark.dao.DaoMeta
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
//import org.apache.hadoop.hbase.mapreduce.TableInputFormat
//import org.apache.hadoop.hbase.client.{Result, Scan}
//import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
//import org.apache.hadoop.hbase.protobuf.ProtobufUtil
//import org.apache.hadoop.hbase.util.Base64
//import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
//import org.apache.hadoop.hbase.util.Bytes
//import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * Spark数据处理Job基类
 * 
 * 1.读取配置参数
 * 2.初始化SparkContext和SparkSQLContext等
 * 3.定义通用模板方法
 * @author Mingth
 */
abstract class SparkBaseJob{
  val log = LogFactory.getLog(this.getClass)
  
  val now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
  /**
   * 初始化
   * 1.读取配置参数
   * 2.初始化SparkContext和SparkSQLContext等
   */
//  private val propFile = "cwl_spark_prod.properties"
//  private val propFile = "cwl_spark_test_cdh.properties"
  private val propFile = "cwl_spark_test_hdp.properties"
  val props = new java.util.Properties
  props.load(Thread.currentThread().getContextClassLoader.getResourceAsStream(propFile))
    
  private val spark_home = props.getProperty("spark_home")
  private val spark_master = props.getProperty("spark_master")
  private val spark_driver_memory = props.getProperty("spark_driver_memory")
  private val spark_executor_memory = props.getProperty("spark_executor_memory")
  private val jar_file = props.getProperty("jar_file")
  private val app_name = s"${this.getClass.getName}-${now}"
  // sc.parallize方法第二个参数
  val spark_cores = props.getProperty("spark_cores").toInt
  
  // HBase configuration
//  private val hbase_master = props.getProperty("hbase_master")
//  private val hbase_zookeeper_quorum = props.getProperty("hbase_zookeeper_quorum")
//  private val hbase_table_name = props.getProperty("hbase_table_name")
//  val hbaseConf = HBaseConfiguration.create()
//      hbaseConf.set("hbase.master", hbase_master)
//      hbaseConf.set("hbase.zookeeper.quorum", hbase_zookeeper_quorum)
//      hbaseConf.set(TableInputFormat.INPUT_TABLE, hbase_table_name)
      
  // Hive configuration
  val hive_dbname = props.getProperty("hive_dbname")
  val hive_lotterysales_tbname = props.getProperty("hive_lotterysales_tbname")
  val hive_salerevoke_tbname = props.getProperty("hive_salerevoke_tbname")
  val hive_winvalidate_tbname = props.getProperty("hive_winvalidate_tbname")
  val hive_metastore_uris = props.getProperty("hive_metastore_uris")
  val javax_jdo_option_ConnectionDriverName = props.getProperty("javax_jdo_option_ConnectionDriverName")
  val javax_jdo_option_ConnectionPassword = props.getProperty("javax_jdo_option_ConnectionPassword")
  val javax_jdo_option_ConnectionURL = props.getProperty("javax_jdo_option_ConnectionURL")
  val javax_jdo_option_ConnectionUserName = props.getProperty("javax_jdo_option_ConnectionUserName")
  // MySQL configuration
  val mysql_url = props.getProperty("mysql_url")
  val gp_url = props.getProperty("gp_url")
  
  // 加载MySQL Driver包
  Class.forName("com.mysql.jdbc.Driver").newInstance()

  // 加载Greenplum Driver包
  Class.forName("org.postgresql.Driver").newInstance()
  val mysql_connection:java.sql.Connection = java.sql.DriverManager.getConnection(mysql_url)
  val gp_insert_connection:java.sql.Connection = java.sql.DriverManager.getConnection(gp_url)
  val gp_query_connection:java.sql.Connection = java.sql.DriverManager.getConnection(gp_url)
  
  private val sparkConf = new SparkConf()
                      //.setSparkHome(spark_home)
                      //.setMaster(spark_master)
                      .setAppName(app_name)
                      //.setJars(Array[String]{jar_file})
                      .set("spark.driver.memory", spark_driver_memory)
                      .set("spark.executor.memory", spark_executor_memory)
                      .set("spark.driver.allowMultipleContexts", "true")
                      //.set("spark.kryoserializer.buffer","2048")
                      //.set("spark.kryoserializer.buffer.max", "2048")
  
  val sparkContext:SparkContext = new SparkContext(sparkConf)
  val hiveContext:HiveContext = new HiveContext(sparkContext)

  hiveContext.setConf("hive.metastore.uris",hive_metastore_uris)
  hiveContext.setConf("javax.jdo.option.ConnectionDriverName",javax_jdo_option_ConnectionDriverName)
  hiveContext.setConf("javax.jdo.option.ConnectionPassword",javax_jdo_option_ConnectionPassword)
  hiveContext.setConf("javax.jdo.option.ConnectionURL",javax_jdo_option_ConnectionURL)
  hiveContext.setConf("javax.jdo.option.ConnectionUserName",javax_jdo_option_ConnectionUserName)

  import hiveContext.implicits._
  import hiveContext.sql
  val sqlContext:SQLContext = new SQLContext(sparkContext)
  
  
  
  /**
   * 获取该Job本次运行的最大时间戳
   * @param tsType 时间戳类型：1-数据清洗Job使用最大时间戳，2-数据抽取Job使用最大时间戳
   */
  def getMaxTimestamp(tsType:String):Long = {
    val df = sqlContext.read.format("jdbc").options(
                          Map("url" -> mysql_url,
                          "dbtable" -> "cwl.cwl_max_timestamp")).load
    val mts = df.filter(s"ts_type = ${tsType}").first.getLong(1)
    val maxTimestampStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(mts))
    log.info(s">>>>>>>>>>>>>>>>>>>>${app_name}: MaxTimestamp=${maxTimestampStr}")
    mts
  }
  
  /**
   * 写入本次运行的最大时间戳数据
   * @param tsType 时间戳类型：1-质量检查Job使用最大时间戳，2-数据抽取Job使用最大时间戳
   * @param maxTimestamp 最大时间戳：本次Job运行完毕后的最大时间戳
   */
  def writeMaxTimestamp(tsType:String, maxTimestamp:Long):Unit = {
    var conn:Connection = null
    var ps:PreparedStatement = null
    val update = s"UPDATE cwl.cwl_max_timestamp SET max_timestamp = '${maxTimestamp.toString}' WHERE ts_type = '${tsType}'"
    try {
      conn = DriverManager.getConnection(mysql_url)
      ps = conn.prepareStatement(update)
      ps.executeUpdate()
    } catch {
      case e: Exception => log.error(s">>>>>>>>>>>>>>>>>>>>${app_name}: writeMaxTimestamp to MySQL Error - ${e.getMessage}")
    } finally {
      if (ps != null) {
        ps.close()
      }
      if (conn != null) {
        conn.close()
      }
    }
  }
  
  /**
   * 根据上次运行Job的最大时间戳，从HBase获取全部增量数据
   * 继承基类Job的子Job必须覆盖的方法
   *
   * @param oldMaxTimeStamp 上次Job运行的最大时间戳
   * @return HBase全部增量数据
   */
  //def getAllHBaseDataAfter(oldMaxTimeStamp:Long):RDD[(String,String)]

  /**
   * Job运行逻辑
   * 继承基类Job的子Job必须覆盖的方法
   */
  def runJob


//  def convertScanToString(scan: Scan) = {
//    val proto = ProtobufUtil.toScan(scan)
//    Base64.encodeBytes(proto.toByteArray)
//  }
}

