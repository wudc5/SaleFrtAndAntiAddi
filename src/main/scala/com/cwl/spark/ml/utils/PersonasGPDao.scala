//package com.cwl.spark.ml.utils
//
//import java.sql.{Connection, PreparedStatement, Timestamp}
//import java.util.{Properties, UUID}
//
////import com.cwl.spark.dao.DaoMeta
////import com.cwl.spark.dao.gp.GameGPDao.log
//import org.apache.spark.SparkContext
//import org.apache.spark.sql.hive.HiveContext
//import org.apache.spark.sql.types.{StringType, StructField, StructType}
//import org.apache.spark.sql.{DataFrame, Row, SQLContext}
//
///**
//  * Created by dushao on 17-5-15.
//  */
//object PersonasGPDao {
//  //将DataFrame 写入到GP中
//  def updateGP(connection:Connection,dataFrame:DataFrame,tableName:String): Unit ={
//
//    val dfFields = dataFrame.columns
//    var setFields:String = null
//
//    for(field<-dfFields ; if !field.equals("account")) yield setFields = setFields.concat(field+",")
//
//    var values:String = null
//
//
//    for(valueNum <- 0 to dfFields.length-1) {
//      values = values.concat("?,")
//    }
//    val sql =
//      s"""UPDATE $tableName SET (${setFields.substring(0,setFields.length-1)})
//         | VALUES(${values.substring(0,values.length-1)})
//         | WHERE ACCOUNT = ?;
//          """.stripMargin.replaceAll("\\r\\n", "")
//
//    var ps: PreparedStatement = null
//    try {
//      ps = connection.prepareStatement(sql)
//      connection.setAutoCommit(false)
//      dataFrame.rdd.map { row =>
//        val fieldsArray = setFields.split(",")
//        val fieldsNum = fieldsArray.length
//        for(fieldIndex <- 0 until fieldsNum){
//          ps.setDouble(fieldIndex+1,row.getAs[Double](fieldsArray(fieldIndex)))
//        }
//        ps.setString(fieldsNum+1,row.getAs[String]("account"))
//        ps.addBatch()
//      }
//      ps.executeBatch()
//      connection.commit()
//      connection.setAutoCommit(true)
//    } catch {
//      case ex: Exception => {
//        ex.printStackTrace()
//      }
//    } finally {
//      if (ps != null) ps.close()
//    }
//  }
//
//  def insertGP(connection:Connection,dataFrame:DataFrame,tableName:String): Unit ={
//    val dfFields = dataFrame.columns
//    var setFields:String = null
//
//    for(field<-dfFields) yield setFields = setFields.concat(field+",")
//
//    var values:String = null
//
//    for(valueNum <- 0 until dfFields.length) yield values = values.concat("?,")
//
//    val sql =
//      s"""INSERT INTO $tableName
//         | VALUES(${values.substring(0,values.length-1)})
//         | WHERE ACCOUNT = ?;
//          """.stripMargin.replaceAll("\\r\\n", "")
//
//    var ps: PreparedStatement = null
//    try {
//      ps = connection.prepareStatement(sql)
//      connection.setAutoCommit(false)
//      dataFrame.rdd.map { row =>
//        val fieldsArray = setFields.split(",")
//        val fieldsNum = fieldsArray.length
//        for(fieldIndex <- 0 until fieldsNum){
//          ps.setString(fieldIndex+1,row.getAs[String](fieldsArray(fieldIndex)))
//        }
//        ps.addBatch()
//      }
//      ps.executeBatch()
//      connection.commit()
//      connection.setAutoCommit(true)
//    } catch {
//      case ex: Exception => {
//        ex.printStackTrace()
//      }
//    } finally {
//      if (ps != null) ps.close()
//    }
//  }
//
//  def insertUserInfoToGp(connection: Connection,dataFrame: DataFrame):Unit={
//    val sql =
//      """
//        |INSERT INTO userInfo VALUES(
//        |UUID,
//        |account,
//        |name,
//        |userId,
//        |provinceId,
//        |provinceName,
//        |cityId,
//        |cityName,
//        |provinceName2,
//        |cityName2,
//        |registerTime,
//        |gender,
//        |cityType,
//        |birthYear
//        |)
//      """.stripMargin
//
//    var ps: PreparedStatement = null
//    try {
//      ps = connection.prepareStatement(sql)
//      connection.setAutoCommit(false)
//      dataFrame.rdd.map { row =>
//        ps.setString(1,row.getString(0))
//        ps.setString(2,row.getString(1))
//        ps.setString(3,row.getString(2))
//        ps.setString(4,row.getString(3))
//        ps.setString(5,row.getString(4))
//        ps.setString(6,row.getString(5))
//        ps.setString(7,row.getString(6))
//        ps.setString(8,row.getString(7))
//        ps.setString(9,row.getString(8))
//        ps.setString(10,row.getString(9))
//        ps.setString(11,row.getString(10))
//        ps.setString(12,row.getString(11))
//        ps.setString(13,row.getString(12))
//        ps.setString(14,row.getString(13))
//
//        ps.addBatch()
//      }
//      ps.executeBatch()
//      connection.commit()
//      connection.setAutoCommit(true)
//    } catch {
//      case ex: Exception => {
//        ex.printStackTrace()
//      }
//    } finally {
//      if (ps != null) ps.close()
//    }
//  }
//
//  /**
//    * 将用户画像的指标数据写入到GP中
//    * @param userDF 用户信息表
//    * @param actionDF 用户行为特征表
//    * @param tradeDF  用户交易行特征表
//    * @param hiveContext
//    * @param gpURL    GP数据库url
//    * @param properties  GP数据库配置
//    * @param features  需要进行训练的特征字段数组
//    * */
//  def personasIndexToGP(userDF:DataFrame, actionDF:DataFrame, tradeDF:DataFrame, hiveContext: HiveContext,
//                        gpURL:String, properties: Properties,features:Array[String] = null): Unit ={
//    userDF.registerTempTable("user_info")
//    actionDF.registerTempTable("action_index")
//    tradeDF.registerTempTable("trade_index")
//
//    hiveContext.sql(
//        """
//          |SELECT
//          |
//          |       user_info.account,
//          |       cityType,
//          |       age,
//          |       gender,
//          |       registerTime,
//          |       avgDailyVisit,
//          |       avgDailyVisitsAtWorkTime,
//          |       avgDailyVisitsAtOffTime,
//          |       avgWeekVisit,
//          |       avgWeekVisitsAtWorkTime,
//          |       avgWeekVisitsAtOffTime,
//          |       avgDailyVisitTime,
//          |       avgDailyPageVisit,
//          |       ratioOfVisitWith3Page,
//          |       avgDailyMoney,
//          |       avgWeekMoney,
//          |       avgMonthMoney,
//          |       maxDailyMoney,
//          |       minDailyMoney,
//          |       maxWeekMoney,
//          |       minWeekMoney,
//          |       maxMonthMoney,
//          |       minMonthMoney,
//          |       stdDailyMoney,
//          |       stdWeekMoney,
//          |       stdMonthMoney,
//          |       mostBuyGame,
//          |       ratioOfMostBuyGame,
//          |       MostBetType,
//          |       maxBetMultiple,
//          |       maxWinAmount,
//          |       avgBetMultiple,
//          |       stdBetMultiple,
//          |       recentBuyTime,
//          |       avgWeekBuyCount,
//          |       totalBuyAmount
//          |FROM user_info
//          |LEFT JOIN trade_index ON user_info.account = trade_index.account
//          |LEFT JOIN action_index ON user_info.account = action_index.account
//        """.stripMargin).write.mode("overwrite").jdbc(url = gpURL,"user_index",properties)
//
////    userDF.join(actionDF,"account").join(tradeDF,"account").select("account",
////                                            "avgDailyVisit",
////                                            "avgWeekVisit",
////                                            "avgMonthVisit",
////                                            "avgDailyVisitsAtWorkTime",
////                                            "avgWeekVisitsAtWorkTime",
////                                            "avgDailyVisitsAtOffTime",
////                                            "avgWeekVisitsAtOffTime",
////                                            "avgDailyVisitTime",
////                                            "avgDailyPageVisit",
////                                            "registerDayAmount",
////                                            "avgDailyMoney",
////                                            "avgWeekMoney",
////                                            "avgMonthMoney",
////                                            "maxDailyMoney",
////                                            "minDailyMoney",
////                                            "maxWeekMoney",
////                                            "minWeekMoney",
////                                            "maxMonthMoney",
////                                            "minMonthMoney",
////                                            "stdDailyMoney",
////                                            "stdWeekMoney",
////                                            "stdMonthMoney",
////                                            "mostBuyGame",
////                                            "ratioOfMostBuyGame",
////                                            "MostBetType",
////                                            "maxBetMultiple",
////                                            "maxWinAmount",
////                                            "avgBetMultiple",
////                                            "stdBetMultiple",
////                                            "recentBuyTime",
////                                            "avgDailyBuyCount",
////                                            "avgWeekBuyCount",
////                                            "avgMonthBuyCount",
////                                            "totalBuyAmount").limit(10).write.jdbc(gpURL,"user_index",properties)
//  }
//
//  /**
//    * 将用户的基本信息写入到GP中
//    * @param sparkContext 读取用户信息文件的启动项
//    * @param sQLContext  读到的用户信息转化为用户信息表
//    * @param gpURL       GP url地址
//    * @param prop        GP的配置项
//    * @param path        用户信息文件的位置
//    * */
//  def userInfoToGP(sparkContext: SparkContext, sQLContext: SQLContext, gpURL:String, prop:Properties, path:String): Unit ={
//
//    val userRow= sparkContext.textFile(path).map(_.split(",")).map{fields =>
//      val uuid = UUID.randomUUID().toString.replaceAll("-", "")
//      //账户，    手机号    姓名        身份证号    省码      省名称    市县名称
//      Row(uuid,fields(0),fields(1),fields(2),fields(3),fields(4),fields(5),fields(6),
//        //省名称     市县名称  注册日期    性别         城市类别     出生年       年龄
//        fields(7),fields(8),fields(9),fields(10),fields(11),fields(12))
//    }
//    val userSchema = StructType(List(
//      StructField("uuid",StringType,nullable = false),
//      StructField("account",StringType,nullable = false),
//      StructField("tel",StringType,nullable = true),
//      StructField("name",StringType,nullable = true),
//      StructField("userId",StringType,nullable = true),
//      StructField("provinceId",StringType,nullable = true),
//      StructField("provinceName",StringType,nullable = true),
//      StructField("cityName",StringType,nullable = true),
//      StructField("provinceName2",StringType,nullable = true),
//      StructField("cityName2",StringType,nullable = true),
//      StructField("registerTime",StringType,nullable = true),
//      StructField("gender",StringType,nullable = true),
//      StructField("cityType",StringType,nullable = true),
//      StructField("birthYear",StringType,nullable = true)
//    ))
//    val userDF = sQLContext.createDataFrame(userRow,userSchema)
//    //将用户信息写入到GP中
//    userDF.write.mode("overwrite").jdbc(gpURL,DaoMeta.personas_user_gp_Table,prop)
//  }
//  /**
//    * 将模型的运行参数写入到GP中
//    * */
//  def kMeansModelInfoToGP(paramMap:Map[String,Any],connection: Connection): Unit ={
//    val insertSql = s"""INSERT INTO dm_modelInfo (
//                       | model_record_uuid,
//                       | model_type,
//                       | algorithm,
//                       | k_value,
//                       | run_iteration,
//                       | epsilon,
//                       | average_distance,
//                       | start_time,
//                       | end_time,
//                       | take_time,
//                       | model_file_path)
//                       | VALUES (?,?,?,?,?,?,?,?,?,?,?)""".stripMargin.replaceAll("\\n", "")
//    //log.info(s">>>>>>>>游戏数据插入game=${game.toString}")
//
//    var ps:PreparedStatement = null
//    try {
//      ps = connection.prepareStatement(insertSql)
//      ps.setString(1, paramMap.get("uuid").get.toString)
//      ps.setString(2, "聚类模型")
//      ps.setString(3, "KMeans||")
//      ps.setInt(4, paramMap.get("k").get.toString.toInt)
//      ps.setInt(5, paramMap.get("iteration").get.toString.toInt)
//      ps.setDouble(6, paramMap.get("epsilon").get.toString.toDouble)
//      ps.setDouble(7, paramMap.get("average_distance").get.toString.toDouble)
//      ps.setTimestamp(8, new java.sql.Timestamp(paramMap.get("startTime").get.toString.toLong))
//      ps.setTimestamp(9, new java.sql.Timestamp(paramMap.get("endTime").get.toString.toLong))
//      ps.setInt(10, paramMap.get("take_time").get.toString.toInt)
//      ps.setString(11, paramMap.get("model_file_path").get.toString)
//      ps.executeUpdate()
//    } catch {
//      case ex:Exception => {
//        log.error(s">>>>>>>>>>>>KMeans Model info 插入GP操作失败：${insertSql}")
//        ex.printStackTrace()
//      }
//    } finally {
//      if (ps != null) ps.close()
//    }
//
//  }
//
//
//  /**
//    * 将模型的运行结果写入到GP中
//    * */
//  def kMeansModelResultToGP(clusterKMap:Map[Int,String],modelParamMap:Map[String,Any],connection: Connection):Unit = {
//    val insertSql = s"""INSERT INTO dm_clusterResult (
//                       |cluster_result_uuid,
//                       |model_record_uuid,
//                       |predict_value,
//                       |algorithm,
//                       |k_value,
//                       |run_iteration,
//                       |epsilon,
//                       |create_time
//                       |) VALUES (?,?,?,?,?,?,?,?)""".stripMargin.replaceAll("\\n", "")
//    //log.info(s">>>>>>>>游戏数据插入game=${game.toString}")
//    var ps:PreparedStatement = null
//    val predictK = clusterKMap.size
//
//    ps = connection.prepareStatement(insertSql)
//
//    try {
//      for(clusterK <- clusterKMap.keys){
//        ps.setString(1, clusterKMap.get(clusterK).get)
//        ps.setString(2, modelParamMap.get("uuid").get.toString)
//        ps.setInt(3, clusterK)
//        ps.setString(4, "KMeans||")
//        ps.setInt(5, modelParamMap.get("k").get.toString.toInt)
//        ps.setInt(6, modelParamMap.get("iteration").get.toString.toInt)
//        ps.setDouble(7, modelParamMap.get("epsilon").get.toString.toDouble)
//        ps.setTimestamp(8, new java.sql.Timestamp(modelParamMap.get("startTime").get.toString.toLong))
//        ps.addBatch()
//      }
//      ps.executeUpdate()
//    } catch {
//      case ex:Exception => {
//        log.error(s">>>>>>>>>>>>KMeans model result数据插入GP操作失败：${insertSql}")
//        ex.printStackTrace()
//      }
//    } finally {
//      if (ps != null) ps.close()
//    }
//  }
//
//  /**
//    * 用户模型表
//    * */
//  def userClusterRelToGP(clusterRelDF:DataFrame,gpURL:String,properties: Properties): Unit ={
//
//   clusterRelDF.write.mode("append").jdbc(gpURL,DaoMeta.KMeans_user_cluster_rel,properties)
//
//  }
//
//}
