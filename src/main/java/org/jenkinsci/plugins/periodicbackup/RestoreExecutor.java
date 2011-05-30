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

import hudson.model.Hudson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class RestoreExecutor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(RestoreExecutor.class.getName());
    private final BackupObject backupObject;
    private final String tempDirectoryPath;

    public RestoreExecutor(BackupObject backupObject, String tempDirectoryPath) {
        this.backupObject = backupObject;
        this.tempDirectoryPath = tempDirectoryPath;
    }

    public void run() {
        // RestoreExecutor thread is not handled by Jenkins therefore we need to be sure that the safeRestart will not be performed during the restore execution
        PeriodicBackupRestartListener restartListener = PeriodicBackupRestartListener.get();
        restartListener.notReady();

        long start = System.currentTimeMillis(); // Measure the duration of the restore
        File tempDir = new File(tempDirectoryPath);
        if(!Util.isWritableDirectory(tempDir)) {
            LOGGER.warning("Restoration Failure! The temporary folder " + tempDir.getAbsolutePath() + " is not writable. ");
            // Setting message to an empty String will make the "Creating backup..." message disappear in the UI
            PeriodicBackupLink.get().setMessage("");
            return;
        }

        // Result of RestoreExecutor will be place in /finalResult directory
        File finalResultDir = new File(tempDir, "finalResult");

        // The /finalResult directory should be empty at this point
        File[] finalResultDirFileList = finalResultDir.listFiles();
        if(finalResultDir.exists() && finalResultDirFileList.length > 0) {
            LOGGER.warning("The final result directory " + finalResultDir.getAbsolutePath() + " is not empty, deleting...");
            try {
                FileUtils.deleteDirectory(finalResultDir);
            } catch (IOException e) {
                LOGGER.warning("Could not delete " + finalResultDir.getAbsolutePath() + " " + e.getMessage());
            }
        }
        if (!finalResultDir.exists()) {
            LOGGER.info(finalResultDir.getAbsolutePath() + " does not exist, making new directory");
            if (!finalResultDir.mkdir()) {
                LOGGER.warning("Restoration Failure! Could not create " + finalResultDir.getAbsolutePath());
                // Setting message to an empty String will make the "Creating backup..." message disappear in the UI
                PeriodicBackupLink.get().setMessage("");
                return;
            }
        }

        // Retrieving archive files related to the given BackupObject
        Iterable<File> archives = null;
        try {
            archives = backupObject.getLocation().retrieveBackupFromLocation(backupObject, tempDir);
        } catch (Exception e) {
            LOGGER.warning("Could not retrieve backup from location. " + e.getMessage());
            e.printStackTrace();
        }

        // Extracting the backup archives to the final result directory
        backupObject.getStorage().unarchiveFiles(archives, finalResultDir);
        // At this point in the /finalResult directory should be only the extracted backup archives
        try {
            backupObject.getFileManager().restoreFiles(finalResultDir);
        } catch (Exception e) {
            LOGGER.warning("Could not restore files. " + e.getMessage());
        }
        LOGGER.info("Reloading configuration...");
        try {
            Hudson.getInstance().doReload();
        } catch (IOException e) {
            LOGGER.warning("Error reloading config files from disk.");
        }
        LOGGER.info("Restoration finished successfully after " + (System.currentTimeMillis() - start) + " ms");
        // Setting message to an empty String will make the "Creating backup..." message disappear in the UI
        PeriodicBackupLink.get().setMessage("");
        restartListener.ready();
    }
}
