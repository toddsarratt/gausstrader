import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

import static ch.qos.logback.classic.Level.DEBUG

scan()

def PROJECT_HOME = "/home/tsarratt/codeDir/GaussTrader"

appender("FILE", RollingFileAppender) {
  file = "${PROJECT_HOME}/logs/GaussTrader.log"
  rollingPolicy(TimeBasedRollingPolicy) {
    fileNamePattern = "${PROJECT_HOME}/logs/GaussTrader.log.%d"
    maxHistory = 30
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS zzz} [%thread] %-5level %logger{0} - %msg%n"
  }
}
root(DEBUG, ["FILE"])