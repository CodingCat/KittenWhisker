# HappySparking
an auto performance investigation tool


```
spark-submit --master yarn-client --class com.microsoft.spark.perf.RunBenchmark --driver-class-path $JAVA_HOME/lib/tools.jar --jars /home/zhunan/code/happysparking/target/happysparking-0.1-SNAPSHOT-jar-with-dependencies.jar,$JAVA_HOME/lib/tools.jar --conf spark.driver.extraJavaOptions=-javaagent:/home/zhunan/code/happysparking/target/happysparking-0.1-SNAPSHOT-jar-with-dependencies.jar=waitingLength=200000,targetDirectory=/flameperf/ --conf "spark.executor.extraJavaOptions=-javaagent:./happysparking-0.1-SNAPSHOT-jar-with-dependencies.jar=waitingLength=200000,targetDirectory=/flameperf/" --conf "spark.executor.extraClassPath=./tools.jar" --driver-memory 16g --executor-memory 20g --executor-cores 8 --num-executors 4  /home/zhunan/code/spark-benchmark/target/scala-2.11/spark-benchmark-assembly-0.4.11-SNAPSHOT.jar sql --benchmark com.microsoft.spark.perf.sql.tpcds.ImpalaKitQueries --database db1 --path /tpcds/ --executionMode parquet -i 2 --outputDir /outputresults/ --reportFormat parquet
```
