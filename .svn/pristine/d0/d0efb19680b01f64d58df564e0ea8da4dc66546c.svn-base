package com.cwl.spark.ml.features

import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.sql.DataFrame
/**
  * Created by wdc on 2017/5/24.
  */
object StringIndex {
    def stringIndexer(col: String, dataFrame: DataFrame, filepath: String): DataFrame ={
      val stringIndexModel = new StringIndexer()
        .setInputCol(col)
        .setOutputCol("Indexed_"+col).fit(dataFrame)

      stringIndexModel.write.overwrite().save(filepath+col+"IndexModel")
      val indexed = stringIndexModel.transform(dataFrame)
      return indexed
    }
}
