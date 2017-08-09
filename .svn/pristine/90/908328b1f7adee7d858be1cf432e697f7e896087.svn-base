package com.cwl.spark.ml.job

import com.cwl.spark.ml.features.StringIndex.stringIndexer
import org.apache.spark.ml.clustering.KMeans
import com.cwl.spark.ml.features.MergeFeatures.mergeFeatures
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StructField, StructType, StringType, IntegerType, DoubleType}
/**
  * Created by wdc on 2017/7/7.
  */
object MakeLabelJob  extends SparkBaseJob{

  override def runJob: Unit = {
    // 获取用户静态和行为信息
    val user_info_df = sqlContext.read.jdbc(gp_url, "userinfo", props)
    val user_index_df = sqlContext.read.jdbc(gp_url,"user_index",props)
    val allInfoDF = user_info_df.join(user_index_df, Seq("account","gender","citytype"))
    allInfoDF.registerTempTable("infoTable")
    val df1 = sqlContext.sql("select account, " +
                                    "age, " +
                                    "gender, " +
                                    "provinceid, " +
                                    "provincename, " +
                                    "cityname, " +
                                    "citytype, " +
                                    "avgdailyvisit, " +
                                    "avgdailyvisittime, " +
                                    "ratioofvisitwith3page,"+
                                    "avgdailyvisitsatworktime, " +
                                    "avgdailyvisitsatofftime, " +
                                    "avgdailymoney, " +
                                    "maxdailymoney, " +
                                    "avgweekvisitsatofftime, " +
                                    "avgbetmultiple," +
                                    "maxbetmultiple, " +
                                    "avgweekbuycount " +
                                    "from infoTable")
    // 合并特征
    val genderScaled_DF = stringIndexer("gender", df1, "/user/ml/Classification/")
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

    // kmeans 聚类
    val kmeans = new KMeans()
      .setK(4)
      .setFeaturesCol("features")
      .setPredictionCol("label")
    val model = kmeans.fit(mergedFeat_DF)
    val res_DF = model.transform(mergedFeat_DF).drop("Indexed_gender").drop("features")

    // 当前label为聚类结果label，这里要处理成用户沉迷度 label
    res_DF.registerTempTable("resTable")
    val labelList = sqlContext.sql("select label, count(*) as num from resTable group by label order by num desc").select("label").map{row=> row.getAs[Integer]("label")}.collect()
    val new_res_rdd = res_DF.map{ row =>
      val old_label = row.getAs[Integer]("label")
      var label = 0
      for(i<- 0 until labelList.length){
        if(old_label == labelList(i)){
          label = i
        }
      }
      Row(row.getAs[String]("account"),
        row.getAs[Int]("age"),
        row.getAs[String]("gender"),
        row.getAs[String]("provinceid"),
        row.getAs[String]("provincename"),
        row.getAs[String]("cityname"),
        row.getAs[String]("citytype"),
        row.getAs[Double]("avgdailyvisit"),
        row.getAs[Double]("avgdailyvisittime"),
        row.getAs[Double]("ratioofvisitwith3page"),
        row.getAs[Double]("avgdailyvisitsatworktime"),
        row.getAs[Double]("avgdailyvisitsatofftime"),
        row.getAs[Double]("avgdailymoney"),
        row.getAs[Double]("maxdailymoney"),
        row.getAs[Double]("avgweekvisitsatofftime"),
        row.getAs[Double]("avgbetmultiple"),
        row.getAs[Double]("maxbetmultiple"),
        row.getAs[Double]("avgweekbuycount"),
        label)
    }
    val schema = StructType(Array(
      StructField("account", StringType, nullable = true),
      StructField("age", IntegerType, nullable = true),
      StructField("gender", StringType, nullable = true),
      StructField("provinceid", StringType, nullable = true),
      StructField("provincename", StringType, nullable = true),
      StructField("cityname", StringType, nullable = true),
      StructField("citytype", StringType, nullable = true),
      StructField("avgdailyvisit", DoubleType, nullable = true),
      StructField("avgdailyvisittime", DoubleType, nullable = true),
      StructField("ratioofvisitwith3page", DoubleType, nullable = true),
      StructField("avgdailyvisitsatworktime", DoubleType, nullable = true),
      StructField("avgdailyvisitsatofftime", DoubleType, nullable = true),
      StructField("avgdailymoney", DoubleType, nullable = true),
      StructField("maxdailymoney", DoubleType, nullable = true),
      StructField("avgweekvisitsatofftime", DoubleType, nullable = true),
      StructField("avgbetmultiple", DoubleType, nullable = true),
      StructField("maxbetmultiple", DoubleType, nullable = true),
      StructField("avgweekbuycount", DoubleType, nullable = true),
      StructField("label", IntegerType, nullable = true)
    ))
    val new_res_DF = sqlContext.createDataFrame(new_res_rdd, schema)

    // 结果存储
    new_res_DF.write.mode("overwrite").jdbc(gp_url, "antiaddiction_train", props)
  }

  def main(args: Array[String]): Unit = {
    runJob
  }
}
