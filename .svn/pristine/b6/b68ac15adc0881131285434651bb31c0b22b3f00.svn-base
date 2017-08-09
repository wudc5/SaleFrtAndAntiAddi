package com.cwl.spark.ml.job

import com.cwl.spark.ml.features.StringIndex.stringIndexer
import com.cwl.spark.ml.utils.TimeHelper.{caldiffTime, getCurrentTime, transStringToTimeStamp}
import com.cwl.spark.ml.utils.GetUUID.getUUID
import com.cwl.spark.ml.utils.DBHelper.{getdataFromPostgresql, insertdataToPostgresql}
import com.cwl.spark.ml.features.MergeFeatures.mergeFeatures
import com.cwl.spark.ml.features.MinMaxScalerProcess.minmaxScaler
import com.cwl.spark.ml.model.RandomForestModelBuild.randomforestModelBuild
import org.apache.spark.mllib.regression.LabeledPoint

/**
  * Created by wdc on 2017/3/20.
  */

object ModelBuildJob extends SparkBaseJob{

  override def runJob: Unit = {
    val starttime = getCurrentTime()
    val filepath = "/user/ml/Classification/"
    val savefilepath = filepath + starttime.replace("-","").replace(" ", "").replace(":","") + "/"
    val df = sqlContext.read.jdbc(gp_url,"antiaddiction_train",props)

    //  性别处理为数值
    val genderScaled_DF = stringIndexer("gender", df, savefilepath)

    // 合并特征
    val inputCols = Array(
      "age",
      "Indexed_gender",
      "avgdailyvisit",
      "avgdailyvisittime",
      "ratioofvisitwith3page",
      "avgdailyvisitsatworktime",
      "avgdailyvisitsatofftime",
      "avgdailymoney",
      "avgweekvisitsatofftime",
      "maxdailymoney",
      "avgbetmultiple",
      "maxbetmultiple",
      "avgweekbuycount"
    )
    val mergedFeat_DF = mergeFeatures(genderScaled_DF, inputCols)

    //  最大最小化特征字段
    val minmax_DF = minmaxScaler(mergedFeat_DF, savefilepath)

    //  将dataframe转为RDD
    val df_rdd = minmax_DF.rdd.map(row =>LabeledPoint(
      row.getAs[Int]("label"),
      row.getAs[org.apache.spark.mllib.linalg.Vector]("MinMaxScalerFeatures")
    )).cache()

    //  训练随机森林模型
    val evaluateValueMap = randomforestModelBuild(df_rdd, sparkContext, savefilepath)     // 返回模型性能度量值
    val algorithm = evaluateValueMap("algorithm").toString
    val Accuracy = evaluateValueMap("Accuracy").toString.toDouble
    val Recall_Score = evaluateValueMap("Recall_Score").toString.toDouble
    val F1 = evaluateValueMap("F1").toString.toDouble
    val numTrees = evaluateValueMap("numTrees").toString.toInt
    val maxDepth = evaluateValueMap("maxDepth").toString.toInt
    log.info("Accuracy: " + Accuracy)
    log.info("Recall_score: " + Recall_Score)
    log.info("F1: " + F1)

    //  保存模型信息

    val endtime =  getCurrentTime()
    val modelinfo_DF = getdataFromPostgresql("dm_modelinfo", sqlContext, gp_url)
    modelinfo_DF.registerTempTable("modelinfoTable")
    val num = sqlContext.sql(String.format("select count(*) from modelinfoTable where model_type = '分类模型' and start_time like '%s%s%s'", "%", starttime.split(" ")(0),"%")).first().getAs[Long]("_c0")
    var serialnum = ""
    if(num+1<10){
      serialnum = "00"+(num+1).toString
    }else if((num+1)>10 && (num+1)<100){
      serialnum = "0"+(num+1).toString
    }else{
      serialnum = (num+1).toString
    }
    val uuid = getUUID()
    val paramMap = Map("uuid" -> uuid,
                        "model_type" ->"分类模型",
                        "algorithm"->algorithm,
                        "numTrees"->numTrees,
                        "maxDepth"->maxDepth,
                        "start_time"->transStringToTimeStamp(starttime),
                        "end_time" -> transStringToTimeStamp(endtime),
                        "take_time"->caldiffTime(starttime, endtime),
                        "modelnumber" -> String.format("RandomForest||%s",
                        starttime.split(" ")(0).replace("-","")+serialnum),
                        "runtype" -> "自动运行",
                        "runuser" -> "System",
                        "model_file_path"->savefilepath)
    insertdataToPostgresql(paramMap, gp_url)
  }

  def main(args: Array[String]): Unit = {
    runJob
  }

}
