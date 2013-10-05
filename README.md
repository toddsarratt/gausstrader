---++ GaussTrader by toddsarratt.net

GaussTrader is an automated algorithmic trading program written in Java.
A list of stock tickers is provided to the program which uses a Yahoo! API
to get historical and near-realtime stock information. Data is committed to
a PostgreSQL database.

Key features :
* Java based algorithmic trading system
* [[http://www.postgresql.org/][PostgreSQL]] implementation with flexibility for other database integration
* [[http://www.gradle.org][Gradle]] build tool
* [[http://git-scm.com/][Git]] version control
* [[http://joda-time.sourceforge.net/][Joda-Time]] date and time API
* [[http://logback.qos.ch/][Logback]] / [[http://www.slf4j.org/][slf4j]] with [[http://groovy.codehaus.org/][Groovy]] configuration

All configuration files in Groovy. We are dedicated to a 100% XML-free environment.