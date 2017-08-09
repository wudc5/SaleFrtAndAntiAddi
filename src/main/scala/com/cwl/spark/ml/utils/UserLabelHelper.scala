package com.cwl.spark.ml.utils
/**
  * Created by wdc on 2017/4/17.
  */
object UserLabelHelper {
  def getLabel(valueList: List[AnyVal], value: Double): Int = {
    val num1 = valueList(0).toString.toDouble
    val num2 = valueList(1).toString.toDouble
    val num3 = valueList(2).toString.toDouble
    var label = 0
    if (value <= num1){
      label = 0
    }else if (value > num1 && value <= num2){
      label = 1
    }else if (value >num2 && value <= num3){
      label = 2
    }else{
      label = 3
    }
    return label
  }
}
