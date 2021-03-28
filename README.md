# Flag Alerts Solution Overview [![Java CI with Maven](https://github.com/akshaddhoke/flag-alerts-batch/actions/workflows/maven.yml/badge.svg)](https://github.com/akshaddhoke/flag-alerts-batch/actions/workflows/maven.yml)
The following were the key design concerns/ decisions for designing the solution:

* Batch Processing: As the input events were to be read from a log file instead of receiving events as a stream, the log file will be batch-processed for flagging alerts
* Spring Batch: The solution uses Spring Batch for batch processing, which provides out of the box support for:
  * Chunk oriented Batch Processing - Data is read, processed, and then written in configurable chunks which makes it robust and performant for large datasets, and avoids pitfalls like OOM issues from load huge volumes of data to memory and reading/ writing
  * Fault Tolerance - Since the primary source of input is from a log file, flag-alerts-batch is made to be fault-tolerant for Input Validation/ Read parsing failures. Skip limit is configurable via property `flag-alerts.parser.invalid-entry.skip-limit`
  * Performance - This solution can parse large log file in an efficient and performant manner. Partitioning logic can also be used for parallel processing - https://docs.spring.io/spring-batch/docs/current/reference/html/index-single.html#partitioning
* Since the events in log file can be unordered, the batch job is made up of two steps. The events are flagged for alerts in column `LOG_EVENT_ALERT.ALERT`:
  * Step 1 - Parsing the logfile for Log Events and persist to temporary tables [ParseLogEntryStepConfiguration](src/main/java/com/test/assignment/cs/flagalerts/processing/parser/ParseLogEntryStepConfiguration.java)
  * Step 2 - Join entries for log entries, and Flag Events and persist Event Alerts into `LOG_EVENT_ALERT` Table [FlagAlertStepConfiguration](src/main/java/com/test/assignment/cs/flagalerts/processing/alerts/FlagAlertStepConfiguration.java)
* Functional/ Integration tests are available in [FlagAlertsJobFunctionalTests](src/test/java/com/test/assignment/cs/flagalerts/processing/FlagAlertsJobFunctionalTests.java)

# Building from Source

Clone the git repository/ download the source using the URL on the Github page:

## Command Line

Maven is the build tool used for flag-alerts-batch. You can build the project and via the command:

    $ ./mvnw clean package

Running tests via maven:

    $ ./mvnw clean test

## Running the application locally using IDE

Since this is a spring boot application, any IDE can be used to run it locally with:
Main class `com.test.assignment.cs.flagalerts.FlagAlertsBatchApplication`, and configuration parameters/ properties

1. Program Argument `log-events.file="./src/test/resources/logfile-assignment-example.txt"` - Pass argument for logfile job parameter(defaults to logfile.txt in working directory)
2. Spring Property `spring.datasource.url=jdbc:hsqldb:file:flag-alerts` - By default, hsql file db in working directory is created with the required schema. Use spring.datasource properties to customize the datasource
 
## Running the application using JAR file

Download the built jar from latest build in [Maven Workflow](https://github.com/akshaddhoke/flag-alerts-batch/actions/workflows/maven.yml), Or once the application is built locally, use the jar from `target/flag-alerts-batch-0.0.1-SNAPSHOT.jar` to run the application. Example:

    $ java -jar flag-alerts-batch-0.0.1-SNAPSHOT.jar log-events.file=logfile-generated.txt

## Additional Configuration Properties
```
#event duration threshold in ms, beyond which the event is flagged for alert
flag-alerts.alerts.event-duration.threshold-ms=4
#Fault tolerance skip limit for invalid entries during log file parsing, before Job Failure
flag-alerts.parser.invalid-entry.skip-limit=10
```

### Reference Documentation
For further reference, please consider the following sections:

* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.4.4/maven-plugin/reference/html/)
* [Spring Batch](https://docs.spring.io/spring-boot/docs/2.4.4/reference/htmlsingle/#howto-batch-applications)
* [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config)

### Guides
The following guides illustrate how to use some features concretely:

* [Creating a Batch Service](https://spring.io/guides/gs/batch-processing/)

