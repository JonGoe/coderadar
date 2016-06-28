package org.wickedsource.coderadar.analyzer.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wickedsource.coderadar.analyzer.api.*;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;

public class CheckstyleSourceCodeFileAnalyzerPlugin implements SourceCodeFileAnalyzerPlugin, ConfigurableAnalyzerPlugin {

    private Logger logger = LoggerFactory.getLogger(CheckstyleSourceCodeFileAnalyzerPlugin.class);

    private Checker checker;

    private CoderadarAuditListener auditListener;

    private byte[] checkstyleConfigurationXml;

    public CheckstyleSourceCodeFileAnalyzerPlugin() {
        checker = new Checker();
        try {
            auditListener = new CoderadarAuditListener();
            Configuration checkstyleConfig = createCheckstyleConfiguration();
            final ClassLoader moduleClassLoader = Checker.class.getClassLoader();
            checker.setModuleClassLoader(moduleClassLoader);
            checker.configure(checkstyleConfig);
            checker.addListener(auditListener);
        } catch (CheckstyleException e) {
            throw new AnalyzerConfigurationException(e);
        }
    }

    @Override
    public AnalyzerFileFilter getFilter() {
        return new AnalyzerFileFilter() {
            @Override
            public boolean acceptFilename(String filename) {
                return filename.endsWith(".java");
            }

            @Override
            public boolean acceptBinary() {
                return false;
            }
        };
    }


    private Configuration createCheckstyleConfiguration() throws CheckstyleException {
        if (this.checkstyleConfigurationXml != null) {
            return getConfigurationFromStream(new ByteArrayInputStream(this.checkstyleConfigurationXml));
        } else {
            // load default configuration file
            return getConfigurationFromStream(getClass().getResourceAsStream("/checkstyle.xml"));
        }
    }

    @Override
    public FileMetrics analyzeFile(byte[] fileContent) throws AnalyzerException {
        File fileToAnalyze = null;
        try {
            fileToAnalyze = createTempFile(fileContent);
            auditListener.reset();
            checker.process(Arrays.asList(fileToAnalyze));
            return auditListener.getMetrics();
        } catch (CheckstyleException | IOException e) {
            throw new AnalyzerException(e);
        } finally {
            if (fileToAnalyze != null && !fileToAnalyze.delete()) {
                logger.warn("Could not delete temporary file {}", fileToAnalyze);
            }
        }
    }

    private File createTempFile(byte[] fileContent) throws IOException {
        File file = File.createTempFile("coderadar-", ".java");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        out.write(fileContent);
        out.close();
        return file;
    }

    @Override
    public boolean isValidConfigurationFile(byte[] configurationFile) {
        try {
            Configuration config = getConfigurationFromStream(new ByteArrayInputStream(configurationFile));
            return true;
        } catch (CheckstyleException e) {
            return false;
        }
    }

    private Configuration getConfigurationFromStream(InputStream in) throws CheckstyleException {
        return ConfigurationLoader.loadConfiguration(
                new InputSource(in),
                new CheckstylePropertiesResolver(new Properties()), // TODO: pass real properties
                true);
    }

    @Override
    public void configure(byte[] configurationFile) {
        this.checkstyleConfigurationXml = configurationFile;
    }
}
