package org.jenkinsci.plugins.testweaver;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.util.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS;

public class TestWeaverPlugin extends Builder implements SimpleBuildStep {
    private final String projectPath;
    private final String experimentName;
    private final String jUnitReportDirectory;
    private String htmlReportDirectory;
    private String exportAsCsvDirectory;
    private String exportAsCsvReports;
    private String exportAsCsvSeparator;
    private String exportAsCsvQuote;
    private boolean exportAsCsvNoQuote;
    private String exportAsCsvDecimalSeparator;
    private String parameterValues;
    private String silverParameters;
    private boolean instrumentView;
    private boolean acceptInconclusiveWatchers;
    private String namespacePattern;
    private int runScenarioLimit;
    private long runTimeLimit;
    private static final int DEFAULT_VALUE = 0;

    @DataBoundConstructor
    public TestWeaverPlugin(String projectPath, String experimentName, String jUnitReportDirectory) {
        this.projectPath = projectPath;
        this.experimentName = experimentName;
        this.jUnitReportDirectory = jUnitReportDirectory;
        this.htmlReportDirectory = "";
        this.exportAsCsvDirectory = "";
        this.exportAsCsvReports = "";
        this.exportAsCsvSeparator = "";
        this.exportAsCsvQuote = "";
        this.exportAsCsvNoQuote = false;
        this.exportAsCsvDecimalSeparator = "";
        this.parameterValues = "";
        this.silverParameters = "";
        this.instrumentView = false;
        this.acceptInconclusiveWatchers = false;
        this.namespacePattern = "";
        this.runScenarioLimit = DEFAULT_VALUE;
        this.runTimeLimit = DEFAULT_VALUE;
    }

    @DataBoundSetter
    public void setHtmlReportDirectory(String htmlReportDirectory) {
        this.htmlReportDirectory = htmlReportDirectory;
    }

    @DataBoundSetter
    public void setExportAsCsvDirectory(String exportAsCsvDirectory) {
        this.exportAsCsvDirectory = exportAsCsvDirectory;
    }

    @DataBoundSetter
    public void setExportAsCsvReports(String exportAsCsvReports) {
        this.exportAsCsvReports = exportAsCsvReports;
    }

    @DataBoundSetter
    public void setExportAsCsvSeparator(String exportAsCsvSeparator) {
        this.exportAsCsvSeparator = exportAsCsvSeparator;
    }

    @DataBoundSetter
    public void setExportAsCsvQuote(String exportAsCsvQuote) {
        this.exportAsCsvQuote = exportAsCsvQuote;
    }

    @DataBoundSetter
    public void setExportAsCsvNoQuote(boolean exportAsCsvNoQuote) {
        this.exportAsCsvNoQuote = exportAsCsvNoQuote;
    }

    @DataBoundSetter
    public void setExportAsCsvDecimalSeparator(String exportAsCsvDecimalSeparator) {
        this.exportAsCsvDecimalSeparator = exportAsCsvDecimalSeparator;
    }

    @DataBoundSetter
    public void setParameterValues(String parameterValues) {
        this.parameterValues = parameterValues;
    }

    @DataBoundSetter
    public void setSilverParameters(String silverParameters) {
        this.silverParameters = silverParameters;
    }

    @DataBoundSetter
    public void setInstrumentView(boolean instrumentView) {
        this.instrumentView = instrumentView;
    }

    @DataBoundSetter
    public void setAcceptInconclusiveWatchers(boolean acceptInconclusiveWatchers) {
        this.acceptInconclusiveWatchers = acceptInconclusiveWatchers;
    }

    @DataBoundSetter
    public void setNamespacePattern(String namespacePattern) {
        this.namespacePattern = namespacePattern;
    }

    @DataBoundSetter
    public void setRunScenarioLimit(int runScenarioLimit) {
        this.runScenarioLimit = runScenarioLimit;
    }

    @DataBoundSetter
    public void setRunTimeLimit(long runTimeLimit) {
        this.runTimeLimit = runTimeLimit;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public String getJUnitReportDirectory() {
        return jUnitReportDirectory;
    }

    public String getHtmlReportDirectory() {
        return htmlReportDirectory;
    }

    public String getExportAsCsvDirectory() {
        return this.exportAsCsvDirectory;
    }

    public String getExportAsCsvReports() {
        return this.exportAsCsvReports;
    }

    public String getExportAsCsvSeparator() {
        return this.exportAsCsvSeparator;
    }

    public String getExportAsCsvQuote() {
        return this.exportAsCsvQuote;
    }

    public boolean isExportAsCsvNoQuote() {
        return this.exportAsCsvNoQuote;
    }

    public String getExportAsCsvDecimalSeparator() {
        return this.exportAsCsvDecimalSeparator;
    }

    public String getParameterValues() {
        return parameterValues;
    }

    public String getSilverParameters() {
        return silverParameters;
    }

    public boolean isInstrumentView() {
        return instrumentView;
    }

    public boolean isAcceptInconclusiveWatchers() {
        return acceptInconclusiveWatchers;
    }

    public String getNamespacePattern() {
        return namespacePattern;
    }

    public int getRunScenarioLimit() {
        return runScenarioLimit;
    }

    public long getRunTimeLimit() {
        return runTimeLimit;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        String testWeaverPath;
        String baseCommand;
        if (IS_OS_WINDOWS) {
            testWeaverPath = "\"%WEAVER_HOME%/bin/testweaver.com\"";
        } else {
            testWeaverPath = "\"" + System.getenv("WEAVER_HOME") +"/bin/testweaver\"";
            testWeaverPath = StringUtils.replace(testWeaverPath, " ", "\\ ");
        }
        String workspace = filePath + "";
        String arguments =
                ((runScenarioLimit > 0) ? " --run-scenario-limit " + runScenarioLimit : "") +
                        ((htmlReportDirectory.length() != 0) ? " --html-report " + modifyPath(htmlReportDirectory, workspace) : "") +
                        ((exportAsCsvDirectory.length() != 0) ? (" --export-report-as-csv --export-dir " + modifyPath(exportAsCsvDirectory, workspace) +
                                (exportAsCsvReports.length() != 0 ? appendMultipleOptions(" --report", exportAsCsvReports, ",") : "") +
                                (exportAsCsvSeparator.length() != 0 ? (" --csv-separator \"" + exportAsCsvSeparator + "\"") : "") +
                                (exportAsCsvQuote.length() != 0 ? (" --csv-quote \"" + exportAsCsvQuote + "\"") : "") +
                                (exportAsCsvNoQuote ? (" --csv-no-quote"): "") +
                                (exportAsCsvDecimalSeparator.length() != 0 ? (" --csv-decimal-separator \"" + exportAsCsvDecimalSeparator + "\"") : ""))
                                : "") +
                        ((silverParameters.length() != 0) ? " --import-silver-parameters " + modifyPath(silverParameters, workspace) : "") +
                        ((parameterValues.length() != 0) ? " --import-parameter-values " + modifyPath(parameterValues, workspace) : "") +
                        ((runTimeLimit > 0) ? " --run-time-limit " + runTimeLimit : "") +
                        ((instrumentView == true) ? " -i" : "") +
                        ((acceptInconclusiveWatchers == true) ? " --accept-inconclusive-watchers" : "") +
                        ((namespacePattern.length() != 0) ? (" --namespace " + namespacePattern) : "") +
                        ((jUnitReportDirectory.length() != 0) ? (" --unit-test " + modifyPath(jUnitReportDirectory, workspace)) : "") +
                        ((projectPath.length() != 0) ? (" " + modifyPath(projectPath, workspace)) : "") +
                        ((experimentName.length() != 0) ? (" " + experimentName) : "");

        final ProcessBuilder processBuilder = new ProcessBuilder();
        final String command;
        if (SystemUtils.IS_OS_WINDOWS) {
            command = "\"" + testWeaverPath + arguments +"\"";
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            command = testWeaverPath + arguments;
            processBuilder.command("/bin/bash", "-c", command);
        }

        taskListener.getLogger().println("Generated command: " + command);
        final Process process = processBuilder.start();
        BufferedReader bufferedReaderOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        taskListener.getLogger().println("Output: ");
        while ((line = bufferedReaderOutput.readLine()) != null) {
            taskListener.getLogger().println(line);
        }
        bufferedReaderOutput.close();
    }

    private String modifyPath(String path, String workspace) {
        String result;
        if (!new File(path).isAbsolute()) {
            result =  "\"" + new File(workspace, path) + "\"";
        } else {
            result = "\"" + path + "\"";
        }
        if (!SystemUtils.IS_OS_WINDOWS) {
            result = StringUtils.replace(result, " ", "\\ ");
        }
        return result;
    }

    private String appendMultipleOptions(String option, String params, String separator) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String param : params.split(separator)) {
            stringBuilder.append(option).append(" ").append(param.trim());
        }
        return stringBuilder.toString();
    }

    @Symbol("testweaver")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private static FormValidation checkPath(String path) {
            try {
                new File(path).toPath();
                return FormValidation.ok();
            } catch (InvalidPathException e) {
                return FormValidation.error("Invalid path!");
            }
        }

        public static String wildcardPatternToRegex(String wildcardPattern) {
            final String quotedPattern = Pattern.quote(wildcardPattern);
            final String questionMarkReplaced = quotedPattern.replace("?", "\\E.\\Q");
            final String starReplaced = questionMarkReplaced.replace("*", "\\E.*\\Q");
            final String pattern;
            if (wildcardPattern.startsWith("*")) {
                pattern = '^' + starReplaced;
            } else {
                pattern = starReplaced;
            }
            return pattern;
        }

        public FormValidation doCheckProjectPath(@QueryParameter String projectPath)
                throws IOException, ServletException {
            if (projectPath.length() != 0) {
                return checkPath(projectPath);
            } else {
                return FormValidation.error("Please fill!");
            }
        }

        public FormValidation doCheckExperimentName(@QueryParameter String experimentName)
                throws IOException, ServletException {
            if (experimentName.length() == 0) {
                return FormValidation.error("Please fill!");
            } else {
                String[] experimentsNames = experimentName.split(" ");
                for (String experiment : experimentsNames) {
                    try {
                        Pattern.compile(wildcardPatternToRegex(experiment));
                    } catch (PatternSyntaxException e) {
                        return FormValidation.error("Invalid experiment's name!");
                    }
                }
                return FormValidation.ok();
            }

        }

        public FormValidation doCheckJUnitReportDirectory(@QueryParameter String jUnitReportDirectory)
                throws IOException, ServletException {
            if (jUnitReportDirectory.length() != 0) {
                return checkPath(jUnitReportDirectory);
            } else {
                return FormValidation.error("Please fill!");
            }
        }

        public FormValidation doCheckHtmlReportDirectory(@QueryParameter String htmlReportDirectory)
                throws IOException, ServletException {
            if (htmlReportDirectory.length() != 0) {
                return checkPath(htmlReportDirectory);
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckExportAsCsvDirectory(@QueryParameter String exportAsCsvDirectory,
                                                          @QueryParameter String exportAsCsvReports,
                                                          @QueryParameter String exportAsCsvSeparator,
                                                          @QueryParameter String exportAsCsvQuote,
                                                          @QueryParameter boolean exportAsCsvNoQuote,
                                                          @QueryParameter String exportAsCsvDecimalSeparator) {
            if ((exportAsCsvReports.length() !=  0 || exportAsCsvSeparator.length() != 0 || exportAsCsvQuote.length() != 0 ||
                    exportAsCsvNoQuote || exportAsCsvDecimalSeparator.length() != 0) && exportAsCsvDirectory.length() == 0) {
                return FormValidation.error("Please fill!");
            }
            if (exportAsCsvDirectory.length() != 0) {
                return checkPath(exportAsCsvDirectory);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckExportAsCsvReports(@QueryParameter String exportAsCsvReports) {
            return FormValidation.ok();
        }

        public FormValidation doCheckExportAsCsvSeparator(@QueryParameter String exportAsCsvSeparator) {
            if (exportAsCsvSeparator.length() > 1) {
                return FormValidation.error("Must be a single character");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckExportAsCsvQuote(@QueryParameter String exportAsCsvQuote) {
            if (exportAsCsvQuote.length() > 1) {
                return FormValidation.error("Must be a single character");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckExportAsCsvNoQuote(@QueryParameter boolean exportAsCsvNoQuote) {
            return FormValidation.ok();
        }

        public FormValidation doCheckExportAsCsvDecimalSeparator(@QueryParameter String exportAsCsvDecimalSeparator) {
            if (exportAsCsvDecimalSeparator.length() > 1) {
                return FormValidation.error("Must be a single character");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckParameterValues(@QueryParameter String parameterValues)
                throws IOException, ServletException {
            if (parameterValues.length() != 0) {
                return checkPath(parameterValues);
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckSilverParameters(@QueryParameter String silverParameters)
                throws IOException, ServletException {
            if (silverParameters.length() != 0) {
                return checkPath(silverParameters);
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckNamespacePattern(@QueryParameter String namespacePattern)
                throws IOException, ServletException {
            return FormValidation.ok();
        }

        private FormValidation checkNumber(String number) {
            try {
                long s = Long.parseLong(number);
                if (s <= 0) {
                    return FormValidation.error("Must be positive!");
                }
            } catch (NumberFormatException e) {
                return FormValidation.error("Please enter a number!");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRunScenarioLimit(@QueryParameter String runScenarioLimit)
                throws IOException, ServletException {
            if (runScenarioLimit.length() != 0) {
                return checkNumber(runScenarioLimit);
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRunTimeLimit(@QueryParameter String runTimeLimit)
                throws IOException, ServletException {
            if (runTimeLimit.length() != 0) {
                return checkNumber(runTimeLimit);
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "TestWeaver";
        }
    }
}
