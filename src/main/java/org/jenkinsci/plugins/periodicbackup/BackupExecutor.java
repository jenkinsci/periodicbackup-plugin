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
import com.google.common.collect.Sets;
import hudson.util.DescribableList;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class BackupExecutor {

    private final Set<File> filesToBackup = Sets.newHashSet();
    private static final Logger LOGGER = Logger.getLogger(BackupExecutor.class.getName());

    public void backup(FileManager fileManager,
                       DescribableList<Storage, StorageDescriptor> storages,
                       DescribableList<Location, LocationDescriptor> locations,
                       String tempDirectory,
                       int cycleQuantity,
                       int cycleDays) throws ArchiverException, PeriodicBackupException, IOException {
        long start = System.currentTimeMillis(); // Measure the duration of the backup
        // Collecting files for backup
        for(File f: fileManager.getFilesToBackup()) {
            filesToBackup.add(f);
        }

        File backupObjectFile;
        Date timestamp;
        String fileNameBase;

        // timeThreshold is used to compare timestamps of backups in each location.
        // Older backups will be deleted after creating the backup.
        Calendar timeThreshold = Calendar.getInstance();
        timeThreshold.add(Calendar.DAY_OF_MONTH, (-1 * cycleDays));

        // Creating backup archives for each storage defined
        for (Storage storage : storages) {
            timestamp = new Date(); // Timestamp for the BackupObject
            fileNameBase = Util.generateFileNameBase(timestamp);

            storage.backupStart(tempDirectory, fileNameBase);
            for (File fileToBackup : filesToBackup) {
                storage.backupAddFile(fileToBackup);
            }
            Iterable<File> archives = storage.backupStop();
            for (Location location : locations) {
                // Sends all the backup archives and backup files to all the active locations
                if(location.enabled) {
                    BackupObject backupObject = new BackupObject(fileManager, storage, location, timestamp);
                    backupObjectFile = Util.createBackupObjectFile(backupObject, tempDirectory, fileNameBase);
                    location.storeBackupInLocation(archives, backupObjectFile);

                    // Delete the temporary BackupObject file
                    LOGGER.info("Deleting the temporary file " + backupObjectFile.getAbsolutePath());
                    if (!backupObjectFile.delete()) {
                        LOGGER.warning("Could not delete " + backupObjectFile.getAbsolutePath());
                    }

                    // Checking for redundant and old backups in the location
                    if (location.getAvailableBackups() != null) {
                        List<BackupObject> backupsInLocation = Lists.newArrayList(location.getAvailableBackups());
                        LOGGER.info("Checking for redundant and old backups in the location.");

                        int index1 = -1; // index in backupsInLocation if the number of backups exceeds the allowed one
                        int index2 = -1; // index in backupsInLocation if the backups are older than allowed

                        if (backupsInLocation.size() > cycleQuantity) {
                            index1 = backupsInLocation.size() - cycleQuantity;
                        }
                        for (BackupObject backupObj : backupsInLocation) {
                            if (backupObj.getTimestamp().before(timeThreshold.getTime())) {
                                index2++;
                            }
                        }
                        if (index1 != -1 || index2 != -1) {
                            for (int index = 0; index < Math.max(index1, index2); index++) {
                                location.deleteBackupFiles(backupsInLocation.get(index));
                            }
                        }
                    }
                }
                else {
                    LOGGER.info(location.getDisplayName() + " is disabled, ignoring.");
                }
            }

            // Delete the temporary archive files
            for (File f : archives) {
                if(f.isDirectory()) {
                    LOGGER.info("Deleting temporary archive directory " + f.getAbsolutePath());
                    FileUtils.deleteDirectory(f);
                }
                else {
                    LOGGER.info("Deleting temporary file " + f.getAbsolutePath());
                    if (!f.delete()) {
                        LOGGER.warning("Could not delete " + f.getAbsolutePath());
                    }
                }

            }
        }
        LOGGER.info("Backup finished successfully after " + (System.currentTimeMillis() - start) + " ms" );
    }
}
