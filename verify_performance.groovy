import java.nio.file.Files

// CSV field names
// sampler_label,aggregate_report_count,average,aggregate_report_median,aggregate_report_90%_line,
// aggregate_report_min,aggregate_report_max,aggregate_report_error%,aggregate_report_rate,
// aggregate_report_bandwidth,aggregate_report_stddev

def failed = false;
new File("Aggregate_Report.csv").splitEachLine(",") { fields ->
    try {
        def label = fields[0];
        //def sampleCount = fields[1];
        //def average = fields[2];

        // Ignore header, footer row if present
        if (label != "sampler_label" && label != "TOTAL") {
            if (!label.contains('[') || !label.contains(']')) {
                throw new NumberFormatException("No acceptable [num] parameter given in "+label);
            }
            def ninety = fields[4].toInteger();
            def acceptable = label.substring(label.indexOf('[')+1, label.indexOf(']')).toInteger();
            if (ninety > acceptable) {
                println(label + ": 90% line " + ninety + " is greater than acceptable ('" + acceptable + "')");
                failed = true;
            } else {
                println(label + ": OK ("+ninety+"ms)");
            }
        }
    } catch (NumberFormatException nfe) {
        println("Error: "+nfe.message);
        System.exit(1);
    }
}

if (failed) {
    println("Verification completed, with errors");
    System.exit(1);
}