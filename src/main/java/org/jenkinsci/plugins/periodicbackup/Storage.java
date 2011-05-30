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

import hudson.DescriptorExtensionList;
import hudson.model.AbstractModelObject;
import hudson.model.Describable;
import hudson.model.Hudson;

import java.io.File;

/**
 *
 * Storage is responsible for operating on backup archives
 */
public abstract class Storage extends AbstractModelObject implements Describable<Storage> {

    /**
     *
     * Initializes Storage for the archiving process
     *
     * @param tempDirectoryPath String with a path to the temporary directory, where the archive(s) will be created
     * @param archiveFilenameBase first part of the archive filename
     * @throws PeriodicBackupException if something goes wrong
     */
    public abstract void backupStart(String tempDirectoryPath, String archiveFilenameBase) throws PeriodicBackupException;

    /**
     *
     * Adds a file to an archive.
     *
     * @param fileToStore The file that will be added to the archive
     * @throws PeriodicBackupException if something goes wrong
     */
    public abstract void backupAddFile(File fileToStore) throws PeriodicBackupException;

    /**
     *
     * Finalizes the archiving process
     *
     * @return Archive files
     * @throws PeriodicBackupException if something goes wrong
     */
    public abstract Iterable<File> backupStop() throws PeriodicBackupException;

    /**
     *
     * Extracts backup files from given archives into the temporary directory
     * after successful extraction the archives will be deleted
     *
     * @param archives backup archives
     * @param finalResultDir Directory where the files will be extracted to, it should be empty at this point
     */
    public abstract void unarchiveFiles(Iterable<File> archives, File finalResultDir);

    public StorageDescriptor getDescriptor() {
        return (StorageDescriptor) Hudson.getInstance().getDescriptor(getClass());
    }

    public String getSearchUrl() {
        return "Storage";
    }

    /**
     *
     * This will allow to retrieve the list of plugins at runtime
     *
     * @return Collection of FileManager Descriptors
     */
    public static DescriptorExtensionList<Storage, StorageDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(Storage.class);
    }

}
