import org.apache.spark.sql._
import org.apache.spark.sql.functions.{lit,udf}
import org.apache.spark.ml.feature.VectorAssembler

import ml.dmlc.xgboost4j.scala.spark.XGBoost

object TrainModel {
   def main(args: Array[String]) {

      val sparkSession = SparkSession.builder()
        .appName("SparkSessionTrainer")
        .getOrCreate()
      val sqlContext = new org.apache.spark.sql.SQLContext(sparkSession.sparkContext)

      val data = sparkSession.read
        .option("inferSchema", "true")
        .option("header", true)
        .csv("fin_data.csv")
      System.out.println("before data")
      System.out.println(data)
      data.printSchema
      System.out.println(data.take(5).foreach(println))
      System.out.println("OK");
      
      val Y = data.select("isFraud")
      System.out.println("Labels:")
      Y.take(10).foreach(println)
      var fraudData = data.filter("type in ('TRANSFER','CASH_OUT')")
        .drop("nameOrig")
        .drop("nameDest")
        .drop("isFlaggedFraud")

      fraudData .take(50).foreach(println)
      //fraudData.withColumn("type_num", "type == 'TRANSFER'", 0).otherwise(1)
      //fraudData.take(5).foreach(println)
      System.out.println("test test")
      System.out.println(fraudData.getClass.getName)
      
      var transfers = fraudData.filter("type in ('TRANSFER')")
      var cashouts = fraudData.filter("type in ('CASH_OUT')")

      transfers = transfers.withColumn("type", lit(0))
      cashouts = cashouts.withColumn("type", lit(1))
      fraudData = transfers.union(cashouts)

      fraudData.take(50).foreach(println)

      def calcDelta(
        startBal: Column, 
        endBal: Column, 
        txnAmt: Column
      ): Column = startBal-txnAmt-endBal

      fraudData = fraudData.withColumn("origDelta", 
        calcDelta(
          fraudData.col("oldBalanceOrig"), 
          fraudData.col("newBalanceOrig"),
          fraudData.col("amount")
        )
      )

      fraudData = fraudData.withColumn("destDelta", 
        calcDelta(
          fraudData.col("oldBalanceDest"), 
          fraudData.col("newBalanceDest"),
          fraudData.col("amount")
        )
      )
      fraudData.show

      val Array(train, test) = fraudData.randomSplit(Array[Double](0.7,0.3), 18)

      val assembler = new VectorAssembler()
        .setInputCols(fraudData.columns)
        .setOutputCol("features")

      val out = assembler.transform(train)
      val train_full = out.withColumn("label", out.col("isFraud"))
      System.out.println(train_full.select("label", "features"))
      train_full.show

   }
}
