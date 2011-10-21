package com.deluan.jenkins.plugins;

import com.deluan.jenkins.plugins.changelog.JazzChangeLogFormatter;
import com.deluan.jenkins.plugins.changelog.JazzChangeLogParser;
import com.deluan.jenkins.plugins.changelog.JazzChangeSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.scm.*;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: deluan
 * Date: 19/10/11
 */
public class JazzSCM extends SCM {

    protected static final Logger logger = Logger.getLogger(JazzSCM.class.getName());

    private String repositoryLocation;
    private String workspaceName;
    private String streamName;
    private String username;
    private String password;

    @DataBoundConstructor
    public JazzSCM(String repositoryLocation, String workspaceName, String streamName,
                   String username, String password) {

        logger.log(Level.FINER, "In JazzSCM constructor");

        this.repositoryLocation = repositoryLocation;
        this.workspaceName = workspaceName;
        this.streamName = streamName;
        this.username = username;
        this.password = password;
    }

    public String getRepositoryLocation() {
        return repositoryLocation;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    private JazzCLI getCliInstance(Launcher launcher, TaskListener listener, FilePath jobWorkspace) {
        return new JazzCLI(launcher, listener, jobWorkspace, getDescriptor().getJazzExecutable(), getDescriptor().getJazzSandbox(),
                username, password, repositoryLocation, streamName, workspaceName);
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        return null; // This implementation is not necessary, as this information is obtained from the remote RTC's repository
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
        JazzCLI cmd = getCliInstance(launcher, listener, workspace);
        try {
            return (cmd.hasChanges()) ? PollingResult.SIGNIFICANT : PollingResult.NO_CHANGES;
        } catch (Exception e) {
            return PollingResult.BUILD_NOW;
        }
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        boolean result = false;
        JazzCLI cmd = getCliInstance(launcher, listener, workspace);
        JazzChangeLogFormatter formatter = new JazzChangeLogFormatter();

        try {
            List<JazzChangeSet> changeSetList = cmd.getChanges();
            formatter.format(changeSetList, changelogFile);
            result = cmd.load();
        } catch (Exception e) {
            result = cmd.load();
            createEmptyChangeLog(changelogFile, listener, "changelog");
        }
        return result;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new JazzChangeLogParser();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<JazzSCM> {
        private String jazzExecutable;
        private String jazzSandbox;

        public DescriptorImpl() {
            super(JazzSCM.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "RTC";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            jazzExecutable = Util.fixEmpty(req.getParameter("rtc.jazzExecutable").trim());
            jazzSandbox = Util.fixEmpty(req.getParameter("rtc.jazzSandbox").trim());
            save();
            return true;
        }

        public String getJazzExecutable() {
            if (jazzExecutable == null) {
                return "scm";
            } else {
                return jazzExecutable;
            }
        }

        public String getJazzSandbox() {
            return jazzSandbox;
        }

        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }
    }
}