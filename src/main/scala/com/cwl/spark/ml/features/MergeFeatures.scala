package com.cwl.spark.ml.features

import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.DataFrame

/**
  * Created by wdc on 2017/4/17.
  */
object MergeFeatures {
  def mergeFeatures(dataFrame: DataFrame, inputCols: Array[String]): DataFrame ={       //合并各字段值得到features
    val assembler = new VectorAssembler()
    .setInputCols(inputCols)
    .setOutputCol("features")
    val output = assembler.transform(dataFrame)
    return output
  }
}
