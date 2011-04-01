package org.jenkinsci.plugins.gitUpdate;

import hudson.Extension;
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

import java.io.IOException;

public class CloneBuilder extends Builder {

    private final String gitToolName;
    private final String repository;
    private final String directory;
    private final String branch;
    private final String username;
    private final String email;

    @DataBoundConstructor
    public CloneBuilder(final String gitToolName, final String repository, final String directory, final String branch,
                        final String username, final String email) {
        this.gitToolName = gitToolName;
        this.repository = repository;
        this.directory = directory;
        this.branch = branch;
        this.username = username;
        this.email = email;
    }

    public String getGitToolName() {
        return gitToolName;
    }

    public String getRepository() {
        return repository;
    }

    public String getDirectory() {
        return directory;
    }

    public String getBranch() {
        return branch;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final GitCloneRunner gitRunner = new GitCloneRunner(build, launcher, listener);
        gitRunner.cleanRepositoryDir();
        return gitRunner.cloneRepository() && gitRunner.configureName() && gitRunner.configureEmail();
    }

    private class GitCloneRunner extends GitRunner {
        public GitCloneRunner(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
            super(build, launcher, listener, gitToolName, directory);
        }
        private void cleanRepositoryDir() {
            try {
                if (repositoryDir.exists())
                    repositoryDir.deleteRecursive();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
        private boolean cloneRepository() {
            final ArgumentListBuilder clone = createGitBuilder().add("clone", "-q");
            final String cleanBranchName = Util.fixEmptyAndTrim(branch);
            if (cleanBranchName != null)
                clone.add("-b", cleanBranchName);
            clone.add(repository, directory);
            final int exitStatus = runInWorkspace(clone);
            if (exitStatus == 0)
                return true;
            listener.getLogger().println(Messages.exit_bad_clone(repository, branch, exitStatus));
            return false;
        }
        private boolean configureName() {
            final ArgumentListBuilder configName = createGitBuilder().add("config", "user.name");
            final String name = getValue(username, build.getProject().getDisplayName());
            final int exitStatus = run(configName.add(name));
            if (exitStatus == 0)
                return true;
            listener.getLogger().println(Messages.exit_bad_configName(name, exitStatus));
            return false;
        }
        private boolean configureEmail() {
            final ArgumentListBuilder configEmail = createGitBuilder().add("config", "user.email");
            final String userEmail = getValue(email, System.getProperty("user.name") + "@" + Util.getHostName());
            final int exitStatus = run(configEmail.add(userEmail));
            if (exitStatus == 0)
                return true;
            listener.getLogger().println(Messages.exit_bad_configEmail(userEmail, exitStatus));
            return false;
        }
    }

    @Extension(ordinal = Descriptor.ORDINAL)
    public static final class Descriptor extends BuildStepDescriptor<Builder> {
        public static final int ORDINAL = -4321;
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
        public String getDisplayName() {
            return Messages.clone_descriptor_displayName();
        }
        public GitTool[] getGitTools() {
            return GitUtil.getGitTools();
        }
        public FormValidation doCheckRepository(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
        public FormValidation doCheckDirectory(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }
    }

}
