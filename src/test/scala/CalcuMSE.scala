import com.cwl.spark.ml.utils.DBHelper.getPGData
import java.lang.Math.sqrt
/**
  * Created by wdc on 2017/6/15.
  */
object CalcuMSE {
  def main(args: Array[String]): Unit = {
    val sql_linear = "select forecast_amount, true_amount from saleforecast where preds_time = '2017-07-27'"
    val sql_arima = "select forecast_amount, true_amount from saleforecast_arima where preds_time = '2017-07-27'"
    val datas_linear = getPGData(sql_linear)
    val datas_arima = getPGData(sql_arima)
    var MSE_linear = 0.0
    var MSE_arima = 0.0
    var n = 0
    while (datas_linear.next()){
      n += 1
      val forecast_amount = datas_linear.getString("forecast_amount").toDouble
      val true_amount = datas_linear.getString("true_amount").toDouble
      val diff = (forecast_amount - true_amount)*(forecast_amount - true_amount)
      MSE_linear += diff
    }
    var m = 0
    while(datas_arima.next()){
      m += 1
      val forecast_amount = datas_arima.getString("forecast_amount").toDouble
      val true_amount = datas_arima.getString("true_amount").toDouble
      val diff = (forecast_amount - true_amount)*(forecast_amount - true_amount)
      MSE_arima += diff
    }

    println("m: "+m)
    println("n: "+n)
    val res_linear = sqrt(MSE_linear / n)
    println("res_linear: "+res_linear)

    val res_arima = sqrt(MSE_arima/m)
    println("res_arima: "+res_arima)
  }

}
