import com.cwl.spark.ml.job.MakeLabelJob_backup.{gp_url, props, sqlContext}
import com.cwl.spark.ml.job.SaleForecastLRJob.{hiveContext, resultset_lp}
import org.apache.spark.mllib.tree.RandomForest
import org.apache.spark.mllib.tree.model.RandomForestModel
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, SQLContext}
import com.cwl.spark.ml.model.ARIMA.ARIMA
import org.apache.commons.math3.util.Incrementor.MaxCountExceededCallback
//import com.cloudera.sparkts.models.ARIMA
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.{LinearRegressionWithSGD, LabeledPoint}
import org.apache.spark.SparkContext
import org.apache.spark.sql.Row
import java.util.Random

object Test {

  def test(): Unit = {
    val user_index_df = sqlContext.read.jdbc(gp_url, "user_index", props)
    user_index_df.drop("citytype").drop("age").show()
    //    val df2 = user_index_df.map{row =>
    //      val account = row.getAs[String]("account")
    //      sqlContext.read.jdbc(gp_url, "userinfo", props).registerTempTable("userinfoTable")
    //      val tel = sqlContext.sql(String.format("select tel from userinfoTable where account = '%s'", account))
    //      val userid = sqlContext.sql(String.format("select userid from userinfoTable where account = '%s'", account))
    //      println("tel: ", tel)
    //      println("userid: ", userid)
    //      Row(account, tel, userid)
    //    }
    //    df2.foreach(println)


  }

  def test1(): Unit = {
    var forecast_amount = 0.0
    try{
      val ts = Vectors.dense(Array(6.0, 7.0, 8.0,6.0,8.0))
      //              val arimaModel = ARIMA.fitModel(1,0,1,ts)
      val arimaModel = ARIMA.autoFit(ts)
      val valueList = arimaModel.forecast(ts, 1)
      forecast_amount = valueList(valueList.size-1)
    } catch {
      case ex: java.lang.NullPointerException=>{
        println(ex.getMessage)
      }
      case ex: MaxCountExceededCallback=>{
        println(ex.getMessage)
      }
    }
    finally {
      if(forecast_amount==0.0){
//        forecast_amount = (2.0+ 2.0+2.0+2.0 + 2.0+ 2.0+ 2.0+ 2.0+ 2.0+ 3.0)/10
      }
    }
    println("forecast_amount: "+ forecast_amount)
    //    val ts = Vectors.dense(Array(saleamount1, saleamount2, saleamount3, saleamount4, saleamount5, saleamount6, saleamount7, saleamount8, saleamount9, saleamount10))
    //    val arimaModel = ARIMA.fitModel(1,0,1,ts)
    //    val forecast_amount = arimaModel.forecast(ts, 1)

    //    val lines = scala.io.Source.fromFile("src/test/scala/lpsa2.data").getLines()
    //    val ts = Vectors.dense(lines.map(_.toDouble).toArray)
    //    val ts = Vectors.dense(Array(1680.0, 3872, 3090, 2846, 4988, 2574, 3906, 5976, 2888, 2666))
//    val ts = Vectors.dense(Array(194052.0, 216666.0, 191358.0, 201418.0, 211778.0, 188788.0))
//    val ts = Vectors.dense(Array(2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 3.0))
//    val arimaModel = ARIMA.autoFit(ts)
//      .fitModel(1, 0, 1, ts)
    //参数输出：
//    println("coefficients: " + arimaModel.coefficients.mkString(","))
//    val forecast = arimaModel.forecast(ts, 1)
//    预测出后五个的值。
//    println("forecast of next 1 observations: " + forecast.toArray.mkString(","))
  }

  def linearModel(): Unit = {
    val train_data = List(resultset_lp(1, 2630.0), resultset_lp(2, 2630.0), resultset_lp(3, 2630.0))
    val train_DF = hiveContext.createDataFrame(train_data)
    val parsedData = train_DF.map { row =>
      LabeledPoint(row.getAs[Double]("saleamount"), Vectors.dense(row.getAs[Int]("drawnum")))
    }.cache()
    println("parseData: ")
    parsedData.foreach(println)

    // 建立模型，进行预测
    val model = LinearRegressionWithSGD.train(parsedData, 100, 0.1)
    val pattern = new java.text.DecimalFormat("#.##")
    val forecast_amount = pattern.format(model.predict(Vectors.dense(4))).toDouble
    println("forecast_amount: " + forecast_amount)
  }

  def main(args: Array[String]): Unit = {
    val randNum = new Random().nextDouble()
    println(randNum)
//    val gameMap = Map("10001" -> "双色球", "10003" -> "七乐彩")
//    val colors = gameMap + ("red" -> "#FF0000")
//    val gamenamelist = colors.keys
//    for(gamename <- gamenamelist){
//      println(gamename)
//    }
//    test1()
  }
}