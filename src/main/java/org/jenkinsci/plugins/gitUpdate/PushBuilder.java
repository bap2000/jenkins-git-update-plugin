package org.jenkinsci.plugins.gitUpdate;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.git.GitTool;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class PushBuilder extends Builder {

    private final String gitToolName;
    private final String directory;
    private final CommitMessage commitMessage;

    @DataBoundConstructor
    public PushBuilder(final String gitToolName, final String directory, final CommitMessage commitMessage) {
        this.gitToolName = gitToolName;
        this.directory = directory;
        this.commitMessage = commitMessage;
    }

    public String getGitToolName() {
        return gitToolName;
    }

    public String getDirectory() {
        return directory;
    }

    public CommitMessage getCommitMessage() {
        return commitMessage;
    }

    public boolean isUseFile() {
        return (commitMessage != null) && commitMessage.isUseFile();
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        GitPushRunner gitRunner = new GitPushRunner(build, launcher, listener);
        return gitRunner.add() && gitRunner.commit() && gitRunner.push();
    }

    private class GitPushRunner extends GitRunner {
        public GitPushRunner(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
            super(build, launcher, listener, gitToolName, directory);
        }
        public boolean add() {
            final int exitStatus = run(createGitBuilder().add("add", "-A"));
            if (exitStatus == 0)
                return true;
            listener.getLogger().println(Messages.exit_bad_add(exitStatus));
            return false;
        }
        public boolean commit() {
            final ArgumentListBuilder commit = createGitBuilder().add("commit");
            if (isUseFile()) {
                final FilePath messageFile = build.getWorkspace().child(replaceMacro(commitMessage.messageFile));
                commit.add("-F").add(messageFile.getRemote(), true);
            } else {
                commit.add("-m", replaceMacro(commitMessage.message));
            }
            final int exitStatus = run(commit);
            if (exitStatus == 0)
                return true;
            listener.getLogger().println(Messages.exit_bad_commit(exitStatus));
            return false;
        }
        public boolean push() {
            final int exitStatus = run(createGitBuilder().add("push"));
            if (exitStatus == 0)
                return true;
            listener.getLogger().println(Messages.exit_bad_push(exitStatus));
            return false;
        }
    }

    public static class CommitMessage {
        private final String message;
        private final String messageFile;

        @DataBoundConstructor
        public CommitMessage(final String value, final String message, final String messageFile) {
            if (Descriptor.FILE.equals(value)) {
                this.messageFile = messageFile;
                this.message = null;
            } else {
                this.message = message;
                this.messageFile = null;
            }
        }

        public String getMessage() {
            return message;
        }

        public String getMessageFile() {
            return messageFile;
        }

        public boolean isUseFile() {
            return Util.fixEmptyAndTrim(messageFile) != null;
        }
    }

    @Extension(ordinal = CloneBuilder.Descriptor.ORDINAL -1)
    public static final class Descriptor extends BuildStepDescriptor<Builder> {
        public static final String MESSAGE = "message";
        public static final String FILE = "file";
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
        public String getDisplayName() {
            return Messages.push_descriptor_displayName();
        }
        public GitTool[] getGitTools() {
            return GitUtil.getGitTools();
        }
        public FormValidation doCheckDirectory(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
        public FormValidation doCheckMessage(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
        public FormValidation doCheckMessageFile(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
        public static String getDefaultCommitMessage() {
            return Messages.default_commit_message();
        }
    }

}
