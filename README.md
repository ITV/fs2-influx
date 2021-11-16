# fs2-influx

`fs2-influx` provides a functional, safe reporter that wraps the InfluxDB java client. 

The reporter and constructs for building points are available through the dependency 

```
"com.itv" %% "fs2-influx %% xxx
```

and support for reporting http metrics with `http4s` is provided with 

```
"com.itv" %% "fs2-influx-http4s %% xxx
```

## Core functionality

### Reporters

An asynchronous reporter can be created as follows: 

```scala
val config: InfluxDbConfig = ???
val influx: InfluxReporter[IO] = 
  InfluxReporter
    .async[IO](
      config,
      BatchingConfig(
        100, 
        500.millis //e.g. points are batched and sent in groups of the greater of 100 and the points written in 500 millis
      ),
      tags = Map("test" -> "tag")
    )
```

This client spins up a background `fs2.Stream` that handles the batching of points and writing to influx.

A blocking reporter can be created as follows:

```scala
val config: InfluxDbConfig = ???
val influx: InfluxReporter[IO] = 
  InfluxReporter
    .sync[IO](
      config,
      tags = Map("test" -> "tag")
    )
```

This client provides no batching capabilities and blocks until the point gets written to influx. 

### Writing metrics

```scala
import com.itv.fs2influx.syntax._

type A = ??? //A value you would like to report

implicit val i: InfluxReporter[IO] = ???
implicit val metric: Metric[A] = ??? //these will be in scope for primitive data types and Unit

val a: A = ???

a.report("measurement")
IO(a).reportWithDuration("measurement")

```
