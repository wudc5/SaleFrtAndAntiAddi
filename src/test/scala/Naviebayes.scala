import com.cwl.spark.ml.job.SaleForecastLRJob.{hiveContext, resultset_lp}
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
/**
  * Created by wdc on 2017/7/6.
  */
object Naviebayes {
  def main(args: Array[String]): Unit = {

    val conf = new SparkConf() //创建SparkConf对象
    conf.setAppName("Wow,My First Sparkin IDEA!") //设置应用程序的名称，在程序运行的监控界面可以看到名称
    conf.setMaster("local")

    val sc = new SparkContext(conf)
//    val data = sc.textFile("C:/Spark/data/mllib/sample_naive_bayes_data.txt")
//    val parsedData = data.map { line =>
//      val parts = line.split(',')
//      LabeledPoint(parts(0).toDouble, Vectors.dense(parts(1).split(' ').map(_.toDouble)))
//    }
//
//    // Split data into training (60%) and test (40%).
//    val splits = parsedData.randomSplit(Array(0.6, 0.4), seed = 11L)
//    val training = splits(0)
//    val test = splits(1)
//
//    val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")
//
//    val predictionAndLabel = test.map(p => (model.predict(p.features), p.label))
//    val accuracy = 1.0 * predictionAndLabel.filter(x => x._1 == x._2).count() / test.count()
//    println("accuracy: "+accuracy)

    val train_data = List(resultset_lp(1, 2560), resultset_lp(2, 2561), resultset_lp(3, 2562))
    val train_DF = hiveContext.createDataFrame(train_data)
    val parsedData = train_DF.map{ row =>
      LabeledPoint(row.getAs[Double]("saleamount"), Vectors.dense(row.getAs[Int]("drawnum")))
    }.cache()

    // Save and load model
//    model.save(sc, "target/tmp/myNaiveBayesModel")
//    val sameModel = NaiveBayesModel.load(sc, "target/tmp/myNaiveBayesModel")
  }

}
