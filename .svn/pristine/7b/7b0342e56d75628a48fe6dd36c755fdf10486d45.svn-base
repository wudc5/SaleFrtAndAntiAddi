package com.cwl.spark.ml.features

import java.io.{File, IOException}

import com.cwl.spark.ml.utils.DeleteDir.deleteDir
import org.apache.spark.ml.feature.MinMaxScaler
import org.apache.spark.sql.DataFrame

/**
  * Created by wdc on 2017/4/17.
  */
object MinMaxScalerProcess {
  def minmaxScaler(dataFrame: DataFrame, filepath: String): DataFrame ={            // 特征字段最大最小化
    val scaler = new MinMaxScaler()
      .setInputCol("features")
      .setOutputCol("MinMaxScalerFeatures")

    // Compute summary statistics and generate MinMaxScalerModel
    val scalerModel = scaler.fit(dataFrame)
    try{
      scalerModel.save(filepath + "MinMaxScalerModel")
    }catch {
      case ex: IOException =>{
        val dir = new File(filepath + "MinMaxScalerModel")
        deleteDir(dir)
        scalerModel.save(filepath + "MinMaxScalerModel")
      }
    }
    // rescale each feature to range [min, max].
    val scaledData = scalerModel.transform(dataFrame)
    return scaledData
  }
}
