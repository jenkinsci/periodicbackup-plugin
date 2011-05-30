/*
 * The MIT License
 *
 * Copyright (c) 2010 - 2011, Tomasz Blaszczynski, Emanuele Zattin
 *
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
import com.google.common.collect.Maps;
import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.*;
import hudson.scheduler.CronTab;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 *
 * Main class of the plugin
 *
 * This plugin is based on and inspired by
 * the backup plugin developed by: Vincent Sellier, Manufacture Franï¿½aise des Pneumatiques Michelin, Romain Seguy
 * and the PXE plugin developed by: Kohsuke Kawaguchi
 */
@Extension
public class PeriodicBackupLink extends ManagementLink implements Describable<PeriodicBackupLink>, Saveable {

    private FileManager fileManagerPlugin = null;
    private final DescribableList<Location, LocationDescriptor> locationPlugins = new DescribableList<Location, LocationDescriptor>(this);
    private final DescribableList<Storage, StorageDescriptor> storagePlugins = new DescribableList<Storage, StorageDescriptor>(this);

    private transient String message;   // Message shown on the web page when the backup/restore is performed
    private boolean backupNow = false;  // Flag to determine if backup is triggered by cron or manually
    private String tempDirectory;       // Temporary directory for local storage of files, it should not be placed anywhere inside the Jenkins homedir
    private String cron;                // Backup schedule (cron like)
    private int cycleQuantity;          // Maximum amount of backups allowed
    private int cycleDays;              // Maximum number of days to keep the backup for

    public PeriodicBackupLink() throws IOException {
        load();
    }

    @SuppressWarnings("unused")
    public String getTempDirectory() {
        return tempDirectory;
    }

    @SuppressWarnings("unused")
    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    @SuppressWarnings("unused")
    public String getCron() {
        return cron;
    }

    @SuppressWarnings("unused")
    public void setCron(String cron) {
        this.cron = cron;
    }

    public boolean isBackupNow() {
        return backupNow;
    }

    public void setBackupNow(boolean backupNow) {
        this.backupNow = backupNow;
    }

    @SuppressWarnings("unused")
    public int getCycleQuantity() {
        return cycleQuantity;
    }

    @SuppressWarnings("unused")
    public void setCycleQuantity(int cycleQuantity) {
        this.cycleQuantity = cycleQuantity;
    }

    @SuppressWarnings("unused")
    public int getCycleDays() {
        return cycleDays;
    }

    @SuppressWarnings("unused")
    public void setCycleDays(int cycleDays) {
        this.cycleDays = cycleDays;
    }


    public String getDisplayName() {
        return Messages.displayName();
    }

    @SuppressWarnings("unused")
    public void doBackup(StaplerRequest req, StaplerResponse rsp) throws Exception {
        backupNow = true;
        PeriodicBackup.get().doRun();
        message = "Creating backup...";
        rsp.sendRedirect(".");
    }

    /**
     *
     * Performing restore when triggered form restore web page, backupHash of selected backup is passed to determine which backup in this location should be chosen
     *
     * @param req StaplerRequest
     * @param rsp StaplerResponse
     * @param backupHash hash code of the selected BackupObject set to be restored
     * @throws IOException If an IO problem occurs
     * @throws PeriodicBackupException If other problem occurs
     */
    @SuppressWarnings("unused")
    public void doRestore(StaplerRequest req, StaplerResponse rsp, @QueryParameter("backupHash") int backupHash) throws IOException, PeriodicBackupException {
        Map<Integer, BackupObject> backupObjectMap = Maps.newHashMap();
        // Populate the map with key=hashcode of value
        for (Location location : locationPlugins) {
            if (location.getAvailableBackups() != null) {
                for (BackupObject backupObject : location.getAvailableBackups()) {
                    backupObjectMap.put(backupObject.hashCode(), backupObject);
                }
            }
        }
        if(!backupObjectMap.keySet().contains(backupHash)) {
            throw new PeriodicBackupException("The provided hash code was not found in the map");
        }
        // Perform the restore of the matching BackupObject
        RestoreExecutor restoreExecutor = new RestoreExecutor(backupObjectMap.get(backupHash), tempDirectory);
        Thread t = new Thread(restoreExecutor);
        t.start();
        message = "Restoring backup...";
        rsp.sendRedirect(".");
    }

    @Override
    public String getUrlName() {
        return "periodicbackup";
    }

    @Override
    public String getIconFileName() {
        return "/plugin/periodicbackup/images/48x48/periodicbackup.png";
    }

    @Override
    public String getDescription() {
        return Messages.description();
    }

    protected void load() throws IOException {
        XmlFile xml = getConfigXml();
        if (xml.exists())
            xml.unmarshal(this);  // Loads the contents of this file into an existing object.
    }

    public void save() throws IOException {
        if (BulkChange.contains(this)) return;
        getConfigXml().write(this);
    }

    protected XmlFile getConfigXml() {
        return new XmlFile(Hudson.XSTREAM,
                new File(Hudson.getInstance().getRootDir(), "periodicBackup.xml"));
    }

    @SuppressWarnings("unused")
    public String getRootDirectory() {
        return Hudson.getInstance().getRootDir().getAbsolutePath();
    }

    @SuppressWarnings("unused")
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, ClassNotFoundException {
        JSONObject form = req.getSubmittedForm(); // Submitted configuration form

        // Persist the setting
        BulkChange bc = new BulkChange(this);
        try {
            tempDirectory = form.getString("tempDirectory");
            JSONObject fileManagerDescribableJson = form.getJSONObject("fileManagerPlugin");
            fileManagerPlugin = (FileManager) req.bindJSON(Class.forName(fileManagerDescribableJson.getString("stapler-class")), fileManagerDescribableJson);
            cron = form.getString("cron");
            cycleQuantity = form.getInt("cycleQuantity");
            cycleDays = form.getInt("cycleDays");
            locationPlugins.rebuildHetero(req, form, getLocationDescriptors(), "Location");
            storagePlugins.rebuildHetero(req, form, getStorageDescriptors(), "Storage");

        } catch (Descriptor.FormException e) {
            e.printStackTrace();
        } finally {
            bc.commit();
        }
        rsp.sendRedirect(".");
    }

    public DescriptorImpl getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    /**
     *
     * Descriptor is only used for UI form bindings
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<PeriodicBackupLink> {

        public String getDisplayName() {
            return null; // unused
        }

        @SuppressWarnings("unused")
        public FormValidation doTestCron(@QueryParameter String cron) {
            try {
                return FormValidation.ok(validateCron(cron));
            } catch (FormValidation f) {
                return f;
            }
        }

        private String validateCron(String cron) throws FormValidation {
            try {
                new CronTab(cron);
            } catch (ANTLRException e) {
                throw FormValidation.error(cron + " is not a valid cron syntax! " + e.getMessage());
            }
            return "This cron is OK";
        }
    }

    @SuppressWarnings("unused")
    public Collection<FileManagerDescriptor> getFileManagerDescriptors() {
        return FileManager.all();
    }

    public Collection<StorageDescriptor> getStorageDescriptors() {
        return Storage.all();
    }

    public Collection<LocationDescriptor> getLocationDescriptors() {
        return Location.all();
    }

    public FileManager getFileManagerPlugin() {
        return fileManagerPlugin;
    }

    @SuppressWarnings("unused")
    public void setFileManagerPlugin(FileManager fileManagerPlugin) {
        this.fileManagerPlugin = fileManagerPlugin;
    }

    @SuppressWarnings("unused")
    public DescribableList<Storage, StorageDescriptor> getStorages() {
        return storagePlugins;
    }

    @SuppressWarnings("unused")
    public DescribableList<Location, LocationDescriptor> getLocations() {
        return locationPlugins;
    }

    public static PeriodicBackupLink get() {
        return ManagementLink.all().get(PeriodicBackupLink.class);
    }

    @SuppressWarnings("unused")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

