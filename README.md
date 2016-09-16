# Zanata-performance
Performance testing objects and scripts for Zanata

## Requires
* JMeter
* JMeterPluginsCMD
* Groovy

## Objects
* ZanataPerfTest\_T510\_NoSSd.jmx : Performance tests based on the Lenovo ThinkPad T510

## Scripts
* verify_performance.groovy : Groovy script to collect and verify the 90% line falls below the acceptable value
* MemoryTest.groovy : Basic Virtual Memory usage monitor test tool

## Usage
### Jmeter
Assuming Current Working Directory is this repo

```
/pathToJmeter/bin/jmeter.sh -n -t ZanataPerfTest\_T510\_NoSSd.jmx -Juser={zanata user} -Jkey={zanata key} -Jhostname={test host ip} -Jport={test host port} -l report.csv
java -jar /pathToJMeter/lib/ext/CMDRunner.jar --tool Reporter --generate-csv "Aggregate\_Report.csv" --input-jtl report.csv --plugin-type AggregateReport
groovy verify_performance.groovy

```

### MemoryTest
```
groovy MemoryTest.groovy [-h -v -i <interval> -p <processId> -c <count> -t <threshold> -s <startMem]
```
h:Help
v:Verbose
i:Interval between samples (seconds)
p:Process id if already known
c:Count of samples to take
t:Threshold of memory increase before failure (double, as a multiplier)
_s:Start memory value, for test purposes_
