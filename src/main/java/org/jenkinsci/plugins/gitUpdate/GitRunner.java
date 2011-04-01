package org.jenkinsci.plugins.gitUpdate;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;

import java.util.TreeMap;

public class GitRunner {
        protected final AbstractBuild build;
        protected final Launcher launcher;
        protected final BuildListener listener;
        protected final TreeMap<String, String> envVars;
        protected final FilePath repositoryDir;
        protected final String git;
        protected GitRunner(final AbstractBuild build, final Launcher launcher, final BuildListener listener,
                         final String gitToolName, final String directory) {
            this.build = build;
            this.launcher = launcher;
            this.listener = listener;
            envVars = GitUtil.getEnvironmentVariables(build, listener);
            repositoryDir = build.getWorkspace().child(replaceMacro(directory));
            git = GitUtil.getGitExe(build, listener, gitToolName);
        }
        protected final String replaceMacro(final String toExpand) {
            return Util.replaceMacro(toExpand, envVars);
        }
        protected ArgumentListBuilder createGitBuilder() {
            return new ArgumentListBuilder(git);
        }
        protected String getValue(final String variable, final String defaultValue) {
            final String trimmed = Util.fixEmptyAndTrim(variable);
            return trimmed == null ? defaultValue : trimmed;
        }
        protected int runInWorkspace(final ArgumentListBuilder command) {
            return run(build.getWorkspace(), command);
        }
        protected int run(final ArgumentListBuilder command) {
            return run(repositoryDir, command);
        }
        protected int run(final FilePath workDir, final ArgumentListBuilder command) {
            return GitUtil.run(launcher, listener, workDir, command);
        }
    }