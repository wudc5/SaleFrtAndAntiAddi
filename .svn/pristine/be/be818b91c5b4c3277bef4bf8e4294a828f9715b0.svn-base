package com.cwl.spark.ml.utils

import java.io.File

/**
  * Created by wdc on 2017/4/17.
  */
object DeleteDir {
  def deleteDir(dir: File): Unit = {
    val files = dir.listFiles()
    files.foreach(f => {
      if (f.isDirectory) {
        deleteDir(f)
      } else {
        f.delete()
      }
    })
    dir.delete()
  }
}
