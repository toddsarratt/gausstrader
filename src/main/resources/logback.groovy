import ch.qos.logback.classic.encoder.PatternLayoutEncoder

import static ch.qos.logback.classic.Level.DEBUG

scan()

def PROJECT_HOME = "/home/tsarratt/codeDir/gaussTrader"

appender("FILE", RollingFileAppender) {
    file = "${PROJECT_HOME}/logs/gaussTrader.log"
  rollingPolicy(TimeBasedRollingPolicy) {
      fileNamePattern = "${PROJECT_HOME}/logs/gaussTrader.log.%d"
    maxHistory = 120
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS zzz} [%thread] %-5level %logger{0} - %msg%n"
  }
}
root(DEBUG, ["FILE"])