package com.cwl.spark.ml.features
import java.io.{File, IOException}
import com.cwl.spark.ml.utils.DeleteDir.deleteDir
import org.apache.spark.ml.feature.{OneHotEncoder, StringIndexer}
import org.apache.spark.sql.DataFrame

/**
  * Created by wdc on 2017/4/17.
  */
object OneHotEncoderProcess {
  def onehotEncoder(df: DataFrame, col: String): DataFrame ={
    val indexer = new StringIndexer()
      .setInputCol(col)
      .setOutputCol(col+"Index")
      .fit(df)
    try{
      indexer.save("target/tmp/" + col +"_StringIndexer")
    } catch {
      case ex: IOException =>{
        val dir = new File("target/tmp/" + col+"_StringIndexer")
        deleteDir(dir)
        indexer.save("target/tmp/" + col+"_StringIndexer")
      }
    }
    val indexed = indexer.transform(df)
    val encoder = new OneHotEncoder()
      .setInputCol(col+"Index")
      .setOutputCol(col+"Vec")
    try{
      encoder.save("target/tmp/" + col+"_encoder")
    } catch {
      case ex: IOException =>{
        val dir1 = new File("target/tmp/" + col+"_encoder")
        deleteDir(dir1)
        encoder.save("target/tmp/" + col+"_encoder")
      }
    }
    val encoded = encoder.transform(indexed)
    return encoded
  }
}
