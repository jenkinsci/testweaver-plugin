package io.jenkins.plugins.sample;

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
import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TestWeaverPlugin extends Builder implements SimpleBuildStep {
    private final String projectPath;
    private final String experimentName;
    private final String unitTestDirectory;
    private String htmlReportDirectory;
    private String parameterValues;
    private String silverParameters;
    private boolean instrumentView;
    private String namespacePattern;
    private int runScenarioLimit;
    private long runTimeLimit;
    private static final int DEFAULT_VALUE = 0;

    @DataBoundConstructor
    public TestWeaverPlugin(String projectPath, String experimentName, String unitTestDirectory) {
        this.projectPath = projectPath;
        this.experimentName = experimentName;
        this.unitTestDirectory = unitTestDirectory;
        this.htmlReportDirectory = "";
        this.parameterValues = "";
        this.silverParameters = "";
        this.instrumentView = false;
        this.namespacePattern = "";
        this.runScenarioLimit = DEFAULT_VALUE;
        this.runTimeLimit = DEFAULT_VALUE;
    }

    @DataBoundSetter
    public void setHtmlReportDirectory(String htmlReportDirectory) {
        this.htmlReportDirectory = htmlReportDirectory;
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

    public String getUnitTestDirectory() {
        return unitTestDirectory;
    }

    public String getHtmlReportDirectory() {
        return htmlReportDirectory;
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
        String testWeaverPath = "\"%WEAVER_HOME%/bin/testweaver.com\"";
        String baseCommand = "cmd /c";
        String workspace = filePath + "";
        String arguments =
                ((runScenarioLimit >0) ? " --run-scenario-limit " + runScenarioLimit : "") +
                        ((htmlReportDirectory.length() != 0) ? " --html-report " + modifyPath(htmlReportDirectory, workspace) : "") +
                        ((silverParameters.length() != 0) ? " --import-silver-parameters " + modifyPath(silverParameters, workspace) : "") +
                        ((parameterValues.length() != 0) ? " --import-parameter-values " + modifyPath(parameterValues, workspace) : "") +
                        ((runTimeLimit >0) ? " --run-time-limit " + runTimeLimit : "") +
                        ((instrumentView == true) ? " -i" : "") +
                        ((namespacePattern.length() != 0) ? (" --namespace " + namespacePattern) : "") +
                        ((unitTestDirectory.length() != 0) ? (" --unit-test " + modifyPath(unitTestDirectory, workspace)) : "") +
                        ((projectPath.length() != 0) ? (" " + modifyPath(projectPath, workspace)) : "") +
                        ((experimentName.length() != 0) ? (" " + experimentName) : "");

        String command = baseCommand + " \"" + testWeaverPath + arguments + "\"";
        taskListener.getLogger().println("Generated command: " + command);
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader bufferedReaderOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        taskListener.getLogger().println("Output: ");
        while ((line = bufferedReaderOutput.readLine()) != null) {
            taskListener.getLogger().println(line);
        }
        bufferedReaderOutput.close();
    }

    private String modifyPath(String path, String workspace) {
        if (new File(path).isAbsolute() == false)
            return "\"" + new File(workspace, path) + "\"";
        else
            return "\"" + path + "\"";
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
            if (projectPath.length() != 0)
                return checkPath(projectPath);
            else {
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
                        return FormValidation.error("Invalid experiment's names!");
                    }
                }
                return FormValidation.ok();
            }

        }

        public FormValidation doCheckUnitTestDirectory(@QueryParameter String unitTestDirectory)
                throws IOException, ServletException {
            if (unitTestDirectory.length() != 0)
                return checkPath(unitTestDirectory);
            else {
                return FormValidation.error("Please fill!");
            }
        }

        public FormValidation doCheckHtmlReportDirectory(@QueryParameter String htmlReportDirectory)
                throws IOException, ServletException {
            if (htmlReportDirectory.length() != 0)
                return checkPath(htmlReportDirectory);
            else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckParameterValues(@QueryParameter String parameterValues)
                throws IOException, ServletException {
            if (parameterValues.length() != 0)
                return checkPath(parameterValues);
            else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckSilverParameters(@QueryParameter String silverParameters)
                throws IOException, ServletException {
            if (silverParameters.length() != 0)
                return checkPath(silverParameters);
            else {
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
                if (s <= 0)
                    return FormValidation.error("Must be positive!");
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
