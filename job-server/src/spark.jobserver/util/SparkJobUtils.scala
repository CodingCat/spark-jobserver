package spark.jobserver.util

import com.typesafe.config.Config
import org.apache.spark.SparkConf
import scala.util.Try

/**
 * Holds a few functions common to Job Server SparkJob's and SparkContext's
 */
object SparkJobUtils {
  import collection.JavaConverters._

  /**
   * Creates a SparkConf for initializing a SparkContext based on various configs.
   * Note that anything in contextConfig with keys beginning with spark. get
   * put directly in the SparkConf.
   *
   * @param config the overall Job Server configuration (Typesafe Config)
   * @param contextConfig the Typesafe Config specific to initializing this context
   *                      (typically based on particular context/job)
   * @param the spark master URL, ie "local[4]", "spark://<host>:7077"
   * @param the context name
   * @return a SparkConf with everything properly configured
   */
  def configToSparkConf(config: Config, contextConfig: Config,
                        sparkMaster: String, contextName: String): SparkConf = {
    val conf = new SparkConf()
    conf.setMaster(sparkMaster)
        .setAppName(contextName)

    for (cores <- Try(contextConfig.getInt("num-cpu-cores"))) {
      conf.set("spark.cores.max", cores.toString)
    }
    // Should be a -Xmx style string eg "512m", "1G"
    for (nodeMemStr <- Try(contextConfig.getString("memory-per-node"))) {
      conf.set("spark.executor.memory", nodeMemStr)
    }

    Try(config.getString("spark.home")).foreach { home => conf.setSparkHome(home) }

    // Set the Jetty port to 0 to find a random port
    conf.set("spark.ui.port", "0")

    // Set any other settings in context config that start with "spark"
    for (e <- contextConfig.entrySet().asScala if e.getKey.startsWith("spark.")) {
      conf.set(e.getKey, e.getValue.unwrapped.toString)
    }

    conf
  }
}