import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.FileAppender

import static ch.qos.logback.classic.Level.DEBUG

scan()

def PROJECT_HOME = "/home/tsarratt/codeDir/GaussTrader"

appender("FILE", FileAppender) {
  file = "${PROJECT_HOME}/logs/GaussTrader.log"
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd/HH:mm:ss.SSS/zzz} [%thread] %-5level %logger{36} - %msg%n"
  }
}
root(DEBUG, ["FILE"])