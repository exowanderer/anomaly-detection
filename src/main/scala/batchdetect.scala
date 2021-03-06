package test

import org.apache.spark.sql._
import org.apache.spark.sql.functions.{lit,udf}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.SparkContext

import ml.dmlc.xgboost4j.scala.spark.XGBoost

import dataprep.DataPreparer

object BatchTestModel {
   def main(args: Array[String]) {
      if (args.length < 2) {
        println("Need arguments for model source dir and test data filename, in that order")
	System.exit(1)
      }
      implicit val sc = new SparkContext()
      val prep = new DataPreparer(args(1))
      var fraudData = prep.prepData()
      fraudData.show
      fraudData = prep.vectorize(fraudData)

      val fraudModel = XGBoost.loadModelFromHadoopFile(args(0))
      System.out.println(fraudModel)
      //val predictions = fraudModel.predict(fraudData.select("features","label"))
      val predictions = fraudModel.setExternalMemory(true).transform(fraudData).select("label", "probabilities")
      predictions.show(100)
   }
}
