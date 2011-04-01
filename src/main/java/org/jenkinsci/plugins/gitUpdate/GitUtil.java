package org.jenkinsci.plugins.gitUpdate;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.git.GitTool;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.TreeMap;

public class GitUtil {

    public static int run(final Launcher launcher, final BuildListener listener, final FilePath workDir, final ArgumentListBuilder args) {
        try {
            return launcher.launch().cmds(args).pwd(workDir).stdout(listener).join();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public static TreeMap<String, String> getEnvironmentVariables(final AbstractBuild<?, ?> build, final TaskListener listener) {
        try {
            final TreeMap<String, String> env = build.getEnvironment(listener);
            env.putAll(build.getBuildVariables());
            return env;
        } catch (Exception e) {
            throw new RuntimeException(Messages.exception_failedToGetEnvVars(), e);
        }
    }

    public static String getGitExe(final AbstractBuild build, final TaskListener listener, final String gitToolName) {
        final Node builtOn = build.getBuiltOn();
        final GitTool[] gitToolInstallations = getGitTools();
        final String actualToolName = gitToolName == null ? gitToolInstallations[0].getName() : gitToolName;
        for(final GitTool gitTool : gitToolInstallations) {
            if(gitTool.getName().equals(actualToolName)) {
                if(builtOn != null){
                    try {
                        return gitTool.forNode(builtOn, listener).getGitExe();
                    } catch (IOException e) {
                        listener.getLogger().println("Failed to get git executable");
                    } catch (InterruptedException e) {
                        listener.getLogger().println("Failed to get git executable");
                    }
                }
            }
        }
        return null;
    }

    public static GitTool[] getGitTools() {
        return Hudson.getInstance().getDescriptorByType(GitTool.DescriptorImpl.class).getInstallations();
    }

}
