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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import hudson.Extension;
import hudson.util.FormValidation;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * LocalDirectory defines the local folder to store the backup files
 */
public class LocalDirectory extends Location {

    private File path;
    private static final Logger LOGGER = Logger.getLogger(LocalDirectory.class.getName());

    @DataBoundConstructor
    public LocalDirectory(File path, boolean enabled) {
        super(enabled);
        this.path = path;
    }

    @Override
    public Iterable<BackupObject> getAvailableBackups() {
        if( ! Util.isWritableDirectory(path)) {
            LOGGER.warning(path.getAbsolutePath() + " is not a existing/writable directory.");
            return Sets.newHashSet();
        }
        if(path.listFiles().length == 0) {
            return Sets.newHashSet();
        }
        File[] files = path.listFiles(Util.extensionFileFilter(BackupObject.EXTENSION));
        List<File> backupObjectFiles = Lists.newArrayList(files);
        // The sorting will be performed according to the timestamp
        Collections.sort(backupObjectFiles);

        return Iterables.transform(backupObjectFiles, BackupObject.getFromFile());
    }

    @Override
    public void storeBackupInLocation(Iterable<File> archives, File backupObjectFile) throws IOException {
        if (this.enabled && path.exists()) {
            for (File archive : archives) {
                File destination = new File(path, archive.getName());
                if(archive.isDirectory()) {
                    FileUtils.copyDirectory(archive, destination);
                }
                else {
                    Files.copy(archive, destination);
                }
                LOGGER.info(archive.getName() + " copied to " + destination.getAbsolutePath());
            }
            File backupObjectFileDestination = new File(path, backupObjectFile.getName());
            Files.copy(backupObjectFile, backupObjectFileDestination);
            LOGGER.info(backupObjectFile.getName() + " copied to " + backupObjectFileDestination.getAbsolutePath());
        }
        else {
            LOGGER.warning("skipping location " + this.path + " since it is disabled or it does not exist.");
        }
    }

    @Override
    public Iterable<File> retrieveBackupFromLocation(final BackupObject backup, File tempDir) throws IOException, PeriodicBackupException {
        // Get the list of archive files related to the given BackupObject
        File[] files = path.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.getName().contains( Util.getFormattedDate(BackupObject.FILE_TIMESTAMP_PATTERN, backup.getTimestamp())) &&
                        !pathname.getName().endsWith(BackupObject.EXTENSION));
            }
        });
        if(files.length <= 0) {
            throw new PeriodicBackupException("Backup archives do not exist in " + path.getAbsolutePath());
        }
        Set<File> archivesInTemp = Sets.newHashSet();

        // Copy every archive to the temp dir
        for(File file : files) {
            File copiedFile = new File(tempDir, file.getName());
            if(copiedFile.exists()) {
                LOGGER.warning(copiedFile.getAbsolutePath() + " already exists, deleting... ");
                if(copiedFile.isDirectory()) {
                    FileUtils.deleteDirectory(copiedFile);
                }
                else {
                    if(!copiedFile.delete()) {
                        throw new PeriodicBackupException("Could not delete " + copiedFile.getAbsolutePath());
                    }
                }
            }
            LOGGER.info("Copying " + file.getAbsolutePath() + " to " + copiedFile.getAbsolutePath());
            if(file.isDirectory()) {
                FileUtils.copyDirectory(file, copiedFile);
            }
            else {
                FileUtils.copyFile(file, copiedFile);
            }
            LOGGER.info("Archive " + file.getAbsolutePath() + " copied to " + copiedFile.getAbsolutePath());
            archivesInTemp.add(copiedFile);
        }
        return archivesInTemp;
    }

    @Override
    public void deleteBackupFiles(BackupObject backupObject) {
        String filenamePart = Util.generateFileNameBase(backupObject.getTimestamp());
        File[] files = path.listFiles();

        // Delete all the files containing the timestamp of the given BackupObject in their names
        for(File file : files) {
            if (file.getAbsolutePath().contains(filenamePart)) {
                if(file.isDirectory()) {
                    LOGGER.info("Deleting old/redundant backup archive directory " + file.getAbsolutePath());
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        LOGGER.warning("Could not delete the temporary archive directory. " + e.getMessage());
                    }
                }
                else {
                    LOGGER.info("Deleting old/redundant backup file " + file.getAbsolutePath());
                    if(!file.delete()) {
                        LOGGER.warning("Could not delete file " + file.getAbsolutePath());
                    }
                }

            }
        }
    }

    public String getDisplayName() {
        return "LocalDirectory: " + path;
    }

    @SuppressWarnings("unused")
    public File getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public void setPath(File path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LocalDirectory) {
            LocalDirectory that = (LocalDirectory) o;
            return Objects.equal(this.path, that.path)
                && Objects.equal(this.enabled, that.enabled);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(path, enabled);
    }

    @SuppressWarnings("unused")
    @Extension
    public static class DescriptorImpl extends LocationDescriptor {
        public String getDisplayName() {
            return "LocalDirectory";
        }

        @SuppressWarnings("unused")
        public FormValidation doTestPath(@QueryParameter String path) {
            try {
                return FormValidation.ok(validatePath(path));
            } catch (FormValidation f) {
                return f;
            }
        }

        private String validatePath(String path) throws FormValidation {
            File fileFromString = new File(path);
            if ( ! Util.isWritableDirectory(fileFromString))
                throw FormValidation.error(path + " doesn't exists or is not a writable directory");
            return "directory \"" + path + "\" OK";
        }

    }
}