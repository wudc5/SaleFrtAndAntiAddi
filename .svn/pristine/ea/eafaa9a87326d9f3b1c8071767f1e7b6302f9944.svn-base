package com.cwl.spark.ml.model

import java.io.{File, IOException}

import com.cwl.spark.ml.utils.DeleteDir.deleteDir
import org.apache.spark.SparkContext
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.rdd.RDD

/**
  * Created by wdc on 2017/4/17.
  */
object RandomForestModelBuild {
  def randomforestModelBuild(data: RDD[LabeledPoint], sc: SparkContext, filepath: String): Map[String,Any] ={
    val splits = data.randomSplit(Array(0.7, 0.3))
    val (trainingData, testData) = (splits(0), splits(1))

    // Train a RandomForest model.
    // Empty categoricalFeaturesInfo indicates all features are continuous.
    val numClasses = 10
    val categoricalFeaturesInfo = Map[Int, Int]()
    val numTrees = 3                  // Use more in practice.
    val featureSubsetStrategy = "auto" // Let the algorithm choose.
    val impurity = "gini"
    val maxDepth = 4
    val maxBins = 32

    val model = RandomForest.trainClassifier(trainingData, numClasses, categoricalFeaturesInfo,
      numTrees, featureSubsetStrategy, impurity, maxDepth, maxBins)

    // Evaluate model on test instances and compute test error
    val labelAndPreds = testData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    // calculate Accuracy
    val Accuracy = labelAndPreds.filter(r => r._1 == r._2).count.toDouble / testData.count()

    // calculate Recall score
    var sum = 0.0
    for(i <- 0 to 3){
      val TP = labelAndPreds.filter(r => r._1 == i && r._2 == i).count().toDouble
      val FN = labelAndPreds.filter(r => r._1 == i && r._2 != i).count().toDouble
      val FP = labelAndPreds.filter(r => r._1 != i && r._2 == i).count().toDouble
      val TN = labelAndPreds.filter(r => r._1 != i && r._2 != i).count().toDouble
      val R_Score_tmp = TP/(TP+FN)
      sum = sum + R_Score_tmp
    }
    val Recall_Score = sum/4

    // calculate F1 value
    val F1 = Accuracy * Recall_Score * 2 / (Accuracy+Recall_Score)

    // Save model
    try{
      model.save(sc, filepath + "RandomForestClassificationModel")
    } catch{
      case ex: IOException =>{
        val dir = new File(filepath + "RandomForestClassificationModel")
        deleteDir(dir)
        model.save(sc, filepath + "RandomForestClassificationModel")
      }
    }
    return Map("algorithm" -> "随机森林", "numTrees" -> 3, "maxDepth" -> 4, "Accuracy"->Accuracy, "Recall_Score" -> Recall_Score, "F1" -> F1)
  }
}
