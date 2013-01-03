/*
 * The MIT License
 *
 * Copyright (c) 2010 - 2011, Tomasz Blaszczynski, Emanuele Zattin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.periodicbackup;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Hudson;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * This implementation of FileManager will only select the .xml files from the Jenkins homedir
 * and the config.xml files of all the jobs during backup.
 * During restore it will try to overwrite the existing files.
 */
public class ConfigOnly extends FileManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigOnly.class.getName());

    @DataBoundConstructor
    public ConfigOnly() {
        super();
        this.restorePolicy = new OverwriteRestorePolicy();
    }

    public String getDisplayName() {
        return "ConfigOnly";
    }

    @Override
    public Iterable<File> getFilesToBackup() throws PeriodicBackupException {
        File rootDir = Hudson.getInstance().getRootDir();
        List<File> filesToBackup = Lists.newArrayList();
        // First find the xml files in the home directory
        File[] xmlsInRoot = rootDir.listFiles(Util.extensionFileFilter("xml"));
        filesToBackup.addAll(Arrays.asList(xmlsInRoot));
        addJobFiles(rootDir, filesToBackup);
        addUserFiles(rootDir, filesToBackup);
        return filesToBackup;
    }

    private void addJobFiles(File rootDir, List<File> filesToBackup) {
        File jobsDir = new File(rootDir, "jobs");
        if (jobsDir.exists() && jobsDir.isDirectory()) {
            // Each job directory should have a config.xml file
            File[] dirsInJobs = jobsDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
            for (File job : dirsInJobs) {
                File jobConfig = new File(job, "config.xml");
                if (jobConfig.exists() && jobConfig.isFile()) {
                    filesToBackup.add(jobConfig);
                    // There might be some config file from the Promotion plugin
                    File promotionDir = new File(job, "promotions");
                    if (promotionDir.exists()) {
                        File[] promotionDirs = promotionDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
                        for (File dir : promotionDirs) {
                            File promotionConfig = new File(dir, "config.xml");
                            if (promotionConfig.exists() && promotionConfig.isFile()) {
                                filesToBackup.add(promotionConfig);
                            }
                        }
                    }
                }
                else {
                    LOGGER.warning(jobConfig.getAbsolutePath() + " does not exist or is not a file.");
                }
            }
        }
    }

    private void addUserFiles(File rootDir, List<File> filesToBackup) {
        File usersDir = new File(rootDir, "users");
        if (usersDir.exists() && usersDir.isDirectory()) {
            // Each user directory should have a config.xml file
            File[] dirsInUsers = usersDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
            for (File user : dirsInUsers) {
                File userConfig = new File(user, "config.xml");
                if (userConfig.exists() && userConfig.isFile()) {
                    filesToBackup.add(userConfig);
                }
                else {
                    LOGGER.warning(userConfig.getAbsolutePath() + " does not exist or is not a file.");
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConfigOnly) {
            ConfigOnly that = (ConfigOnly) o;
            return Objects.equal(this.restorePolicy, that.restorePolicy);
        }
        return false;

    }

    @Override
    public int hashCode() {
        return 97;
    }

    @SuppressWarnings("unused")
    @Extension
    public static class DescriptorImpl extends FileManagerDescriptor {
        public String getDisplayName() {
            return "ConfigOnly";
        }
    }
}
