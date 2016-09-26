#!/bin/env groovy
import groovy.transform.Field

@Field int interval = 600
@Field String processid = ""
@Field boolean debug = false
@Field int count = 5
@Field float thresholdMultiplier = 1.1
@Field int startValue = -1
@Field Date startTime = new Date()

/*
 * Messages for debug mode
 */
void debugOut(message) {
    if (debug) {
        message = message.trim()
        println "${ new Date() }[DEBUG] $message"
    }
}


void infoOut(message) {
    message = message.trim()
    println "${ new Date() }[INFO] $message"
}

void parseOpts(args) {
    infoOut "Parsing options"
    def cli = new CliBuilder(usage:'groovy MemoryTest.groovy [-h -v -i <interval> -p <processId> -c <count> -t <threshold>]')
    cli.with {
        h longOpt: 'help', 'Show usage'
        v longOpt: 'verbose', 'Verbose mode'
        i longOpt: 'interval', args:1, argName:'interval', "Delay between samples (seconds) [600]"
        c longOpt: 'count', args:1, argName:'count', "Number of samples to record [5]"
        p longOpt: 'processId', args:1, argName:'process', "ID of Zanata server process, eg. 123456"
        t longOpt: 'threshold', args:1, argName:'thresholdMultiplier', "Max memory increase (multiplied) before failure [1.1]"
        s longOpt: 'startMem', args:1, argName:'startmem', "Starting memory (for script testing)"
    }
    def options = cli.parse args
    if (!options) {
        return
    }
    if (options.h) {
        cli.usage()
        System.exit 0
    }
    if (options.v) {
        debug = true
    }
    if (options.i) {
        interval = options.i.toInteger()
    }
    if (options.p) {
        processid = options.p
    }
    if (options.c) {
        count = options.c.toInteger()
    }
    if (options.t) {
        thresholdMultiplier = options.t.toFloat()
    }
    if (options.s) {
        startValue = options.s.toInteger()
    }
}

/*
 * Exit with failure
 */
void exitWithError(message) {
    debugOut "Exiting for"
    infoOut message
    System.exit 1
}

/**
 * Verify process is functional
 * This uses the system util 'pkill -0', which has the no-op signal of 0
 * @param pid Process ID
 */
void verifyProcess(pid) {
    try {
        command = "kill -0 $pid"
        debugOut command
        excode = command.execute().waitFor()
        debugOut "Kill process check code $excode"
        if (excode != 0) {
            exitWithError "Unable to verify running process $pid"
        }
    } catch (IllegalThreadStateException e) {
        exitWithError "Error attempting to verify running process $pid : $e"
    }
}

/**
 * Retrieve the process ID via the process name
 * This uses the system util 'pgrep'
 * @param processName A string that exists in the name of the Zanata process
 */
Integer getPid(processName) {
    pid = processid.toString().trim()
    if (pid.isEmpty()) {
        command = "pgrep -f $processName"
        debugOut command
        pid = command.execute().text
        debugOut "pgrep: $pid"
        if (pid.isEmpty()) {
            exitWithError "Error: Could not find running process with name $processName"
        }
    }
    verifyProcess pid
    return pid as Integer
}

/**
 * Get memory usage from process, with delay
 * @param pid Process ID of Server
 * @param delay Seconds to wait before sampling
 */
Integer getMemoryUsage(pid, delay) {
    String command = "pidstat -p $pid -r $delay 2"
    debugOut command
    result = command
            .execute()
            .text
            .split('\n')?.
            find { it.contains "java" }
    debugOut result
    return result.split()[5] as Integer
}

/**
 * End the test sequence
 * @param message Pass/fail message to report 
 * @param passed Test pass/fail status
 * @param endtime Time the test ended
 * @param endmemory Total memory used at test end 
 */
void finishTest(String message, boolean passed, Date endtime, int endmemory) {
    infoOut "\n[END TEST]"
    infoOut "Start: $startTime"
    infoOut "End: $endtime"
    infoOut "Start Memory: $startingMemory"
    infoOut "End Memory: $endmemory"
    if (!passed) {
        exitWithError message
    } else {
        infoOut "[PASS] $message"
    }
}

/**
 * The test procedure
 * This test locates the running Zanata Server process, takes
 * samples - ie. the amount of Virtual Memory allocated - and
 * warns if the memory usage increases.
 * If the value exceeds the acceptable threshold, the test will
 * fail.
 */
void test() {
    parseOpts args
    debugOut "Verbose mode enabled"
    debugOut "Interval is $interval seconds"
    debugOut "Count is $count iterations"
    debugOut "Threshold Multiplier is $thresholdMultiplier"

    // Validate process
    String pid = getPid 'org.jboss.as.standalone'
    if (pid == null || pid.isEmpty()) {
        exitWithError  "Process not found, run Zanata server first!"
    }

    // Get starting memory and prepare test
    startingMemory = startValue > 0 ? startValue : getMemoryUsage(pid, 1)
    infoOut "Starting memory is $startingMemory"
    int oldMemory = startingMemory
    int memoryThreshold = (startingMemory * thresholdMultiplier).toInteger()
    debugOut "Memory threshold is $memoryThreshold"
    String testMessage = ""

    // Begin sampling
    1.upto(count, {
        verifyProcess(pid)
        newMemory = getMemoryUsage(pid, interval)
        infoOut "Sample $it:$newMemory"
        debugOut "[INFO] Start $startingMemory  Old $oldMemory  New $newMemory  Upper $memoryThreshold"
        if (newMemory > memoryThreshold) {
            finishTest("[FAIL] Memory exceeds threshold ($newMemory > $memoryThreshold)", false, new Date(), newMemory)
        } else if (newMemory > oldMemory) {
            infoOut "[WARNING] Memory usage increased ($newMemory > $oldMemory)"
            oldMemory = newMemory
            debugOut "Old memory value updated to $oldMemory"
            testMessage = "memory increased to $newMemory (${ ((newMemory - startingMemory)/startingMemory) * 100 }%)"
        }
    })

    finishTest(testMessage, true, new Date(), newMemory)
}

// Execute
test()
