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

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Hudson;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class NullStorage extends Storage {

    private transient File destinationDirectory;

    private static final Logger LOGGER = Logger.getLogger(NullStorage.class.getName());

    @DataBoundConstructor
    public NullStorage() {
        super();
    }

    @Override
    public void backupStart(String tempDirectoryPath, String archiveFilenameBase) throws PeriodicBackupException {
        File tempDir = new File(tempDirectoryPath);
        destinationDirectory = new File(tempDir, archiveFilenameBase + ".null");
        if(destinationDirectory.exists()) {
            LOGGER.info("Destination directory " + destinationDirectory.getAbsolutePath() + " exists. Deleting...");
            try {
                FileUtils.deleteDirectory(destinationDirectory);
            } catch (IOException e) {
                LOGGER.warning("Could not delete destination directory " + destinationDirectory.getAbsolutePath());
            }
        }
    }

    @Override
    public void backupAddFile(File fileToStore) throws PeriodicBackupException {
        try {
            FileUtils.copyFile(fileToStore, new File(destinationDirectory,
                    Util.getRelativePath(fileToStore, Hudson.getInstance().getRootDir())));
        } catch (IOException e) {
            LOGGER.warning("Could not copy " + fileToStore.getAbsolutePath() + " to " + destinationDirectory);
        }
    }

    @Override
    public Iterable<File> backupStop() throws PeriodicBackupException {
        return Lists.newArrayList(destinationDirectory);
    }

    @Override
    public void unarchiveFiles(Iterable<File> archives, File finalResultDir) {
        // There will be just one File object (archive directory)
        for (File archive : archives) {
            try {
                LOGGER.info("Copying " + archive.getAbsolutePath() + " to " + finalResultDir.getAbsolutePath());
                if(archive.isDirectory()) {
                    FileUtils.copyDirectory(archive, finalResultDir);
                }
                else {
                    FileUtils.copyFile(archive, new File(finalResultDir, archive.getName()));
                }
            } catch (IOException e) {
                LOGGER.warning("Error during copying " + archive.getAbsolutePath() + " to " + finalResultDir.getAbsolutePath());
            }
        }
    }

    public String getDisplayName() {
        return "NullStorage";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NullStorage;
    }

    @Override
    public int hashCode() {
        return 101;
    }

    @SuppressWarnings("unused")
    @Extension
    public static class DescriptorImpl extends StorageDescriptor {
        public String getDisplayName() {
            return "NullStorage";
        }

        @Override
        public String getArchiveFileExtension() {
            return "null";
        }

    }
}
