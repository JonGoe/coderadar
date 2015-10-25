package org.wickedsource.coderadar.analyzer.loc;

import org.wickedsource.coderadar.analyzer.api.*;

import java.io.IOException;
import java.util.Properties;

/**
 * A simple plugin counting java lines of code (loc) in a naive way.
 */
public class LocAnalyzer implements Analyzer {

    private Properties properties;

    private LocCounter locCounter = new LocCounter();

    public static final Metric JAVA_LOC_METRIC = new Metric("org.wickedsource.coderadar.analyzer.plugin.api.AnalyzerPlugin.javaLoc");

    @Override
    public void configure(Properties properties) {
        this.properties = properties;
    }

    @Override
    public AnalyzerFilter getFilter() {
        return new LocAnalyzerFilter();
    }

    @Override
    public FileMetrics analyzeFile(byte[] fileContent) throws AnalyzerException {
        try {
            FileMetrics results = new FileMetrics();
            results.setMetricCount(JAVA_LOC_METRIC, (long) locCounter.count(fileContent));
            return results;
        }catch(IOException e){
            throw new AnalyzerException(e);
        }
    }
}