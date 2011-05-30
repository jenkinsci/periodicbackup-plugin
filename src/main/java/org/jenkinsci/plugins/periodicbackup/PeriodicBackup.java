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

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.IOException;
import java.util.logging.Logger;
import hudson.scheduler.CronTab;

/**
 *
 * PeriodicBackup is responsible for performing backups periodically
 * according to configured first backup time and backup frequency
 */
@Extension
public class PeriodicBackup extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(BackupExecutor.class.getName());

    public PeriodicBackup() {
        super("PeriodicBackup");
    }

    @Override
    protected void execute(TaskListener taskListener) {
        PeriodicBackupLink link = PeriodicBackupLink.get();
        String cron = link.getCron();
        if(cron != null) {
            try {

                CronTab cronTab = new CronTab(link.getCron());
                long currentTime = System.currentTimeMillis();
                if ((cronTab.ceil(currentTime).getTimeInMillis() - currentTime) == 0 || link.isBackupNow()) {
                    link.setBackupNow(false);
                    BackupExecutor executor = new BackupExecutor();
                    try {
                        executor.backup(link.getFileManagerPlugin(), link.getStorages(), link.getLocations(), link.getTempDirectory(), link.getCycleQuantity(), link.getCycleDays());
                    } catch (PeriodicBackupException e) {
                        LOGGER.warning("Backup failure " + e.getMessage());
                    } catch (IOException e) {
                        LOGGER.warning("Backup failure " + e.getMessage());
                    } catch (ArchiverException e) {
                        LOGGER.warning("Backup failure " + e.getMessage());
                    } finally {
                        // Setting message to an empty String will make the "Creating backup..." message disappear in the UI
                        link.setMessage("");
                    }
                }
            } catch (ANTLRException e) {
                LOGGER.warning("Could not parse given cron tab! " + e.getMessage());
            }
        }
        else {
            LOGGER.warning("Cron is not defined.");
        }
    }

    @Override
    public long getRecurrencePeriod() {
        // Recurrence will happened every minute, but the action will be taken according to the cron settings
        return MIN;
    }

    public static PeriodicBackup get() {
        return AsyncPeriodicWork.all().get(PeriodicBackup.class);
    }

}
