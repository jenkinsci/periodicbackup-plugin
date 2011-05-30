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
import hudson.DescriptorExtensionList;
import hudson.model.AbstractModelObject;
import hudson.model.Describable;
import hudson.model.Hudson;

import java.io.File;
import java.io.IOException;

/**
 *
 * FileManager determines the files selection for backup and restore policies
 */
public abstract class FileManager extends AbstractModelObject implements Describable<FileManager> {

    RestorePolicy restorePolicy;

    /**
     *
     * This method determines files and folders for Storage
     *
     * @return Files to be included in the backup
     * @throws PeriodicBackupException if anything bad happens
     */
    public abstract Iterable<File> getFilesToBackup() throws PeriodicBackupException;

    /**
     *
     * This will restore files to their right place in the HUDSON directory
     *
     * @param finalResultDir the temporary directory where ONLY the files for restoring are
     * @throws java.io.IOException IOException when IO problem
     * @throws PeriodicBackupException if anything else bad happens
     */
    public void restoreFiles(File finalResultDir) throws IOException, PeriodicBackupException {
        restorePolicy.restore(finalResultDir);
    }

    /**
     * This will allow to retrieve the list of plugins at runtime
     *
     * @return Collection of FileManager Descriptors
     */
    public static DescriptorExtensionList<FileManager, FileManagerDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(FileManager.class);
    }

    public FileManagerDescriptor getDescriptor() {
        return (FileManagerDescriptor) Hudson.getInstance().getDescriptor(getClass());
    }

    public String getSearchUrl() {
        return "FileManager";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileManager) {
            FileManager that = (FileManager) o;
            return Objects.equal(this.restorePolicy, that.restorePolicy);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(restorePolicy);
    }
}
