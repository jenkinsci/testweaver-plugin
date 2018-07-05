package io.jenkins.plugins.sample;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class TestWeaverTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestWeaverPlugin("C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\test1", "test", "C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\", "C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\"));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new TestWeaverPlugin("C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\test1", "test", "C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\", "C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\"), project.getBuildersList().get(0));
    }


   /* @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        TestWeaverPlugin builder = new TestWeaverPlugin("C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\test1","test","C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\","C:\\Users\\Qtronic\\Desktop\\Work\\TestWeaver\\test test\\","","",false,"","","");
        project.getBuildersList().add(builder);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Hello, " + name, build);
    }*/


}