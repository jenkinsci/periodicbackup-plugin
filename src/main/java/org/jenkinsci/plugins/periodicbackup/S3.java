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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.common.base.Objects;
import hudson.Extension;
import hudson.RestrictedSince;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.acegisecurity.AccessDeniedException;
import org.apache.commons.io.IOUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * S3 defines Amazon S3 (Simple Storage Service) to store the backup files
 */
public class S3 extends Location {

    private String bucket;
    private String prefix;
    private String tmpDir;
    private String region;
    private String credentialsId;

    private static final Logger LOGGER = Logger.getLogger(S3.class.getName());

    @DataBoundConstructor
    public S3(String bucket, boolean enabled, String tmpDir, String region, String credentialsId) {
        super(enabled);
        this.bucket = bucket;
        this.setTmpDir(tmpDir);
        this.setRegion(region);
        this.setCredentialsId(credentialsId);
    }

    @Override
    public Iterable<BackupObject> getAvailableBackups() {
        AmazonS3 client = AmazonUtil.getAmazonS3Client(region, credentialsId);

        List<S3ObjectSummary> objectSummarys = getObjectSummaries(client);
        return objectSummarys
                .parallelStream()
                .filter(objectSummary ->
                        (objectSummary.getKey() != null && objectSummary.getKey().endsWith(BackupObject.EXTENSION))
                                && isMatchPrefix(objectSummary.getKey())
                )
                .map(objectSummary -> {
                            try {
                                S3ObjectInputStream content = client.getObject(bucket, objectSummary.getKey()).getObjectContent();
                                return BackupObject.getFromInputStream().apply(content);
                            } catch (Exception e) {
                                LOGGER.warning("Exception while getting available backups from S3: " + e);
                                return null;
                            }
                        }
                )
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(BackupObject::getTimestamp))
                ::iterator;
    }

    private boolean isMatchPrefix(String key) {
        Path s3ParentFolder = Paths.get(key).getParent();
        if (s3ParentFolder == null) {
            return hudson.Util.fixEmpty(prefix) == null;
        } else {
            if (hudson.Util.fixEmpty(prefix) == null) {
                return false;
            } else {
                return s3ParentFolder.startsWith(prefix);
            }
        }


    }

    @Override
    public void storeBackupInLocation(Iterable<File> archives, File backupObjectFile) throws IOException {
        if (this.enabled && isBucketExists()) {
            AmazonS3 client = AmazonUtil.getAmazonS3Client(region, credentialsId);
            for (File archive : archives) {
                String backupPath = Paths.get(prefix, archive.getName()).toString().replace("\\", "/");
                LOGGER.info(archive.getName() + " copying to s3 bucket " + bucket + " > " + backupPath);
                client.putObject(bucket, backupPath, archive);
                LOGGER.info(archive.getName() + " copied to s3 bucket " + bucket + " > " + backupPath);
            }
            File dir = new File(tmpDir);
            if (!dir.isDirectory()) {
                if (!dir.mkdir()) {
                    LOGGER.warning("Unable to make temp directory: " + tmpDir);
                    throw new IOException();
                }
            }
            String backupPath = Paths.get(prefix, backupObjectFile.getName()).toString().replace("\\", "/");
            LOGGER.info(backupObjectFile.getName() + " copying to s3 bucket " + bucket + " > " + backupPath);
            client.putObject(bucket, backupPath, backupObjectFile);
            LOGGER.info(backupObjectFile.getName() + " copied to " + bucket + " > " + backupPath);
        } else {
            LOGGER.warning("skipping location " + this.bucket + " since it is disabled or it does not exist.");
        }
    }

    @Override
    public Iterable<File> retrieveBackupFromLocation(final BackupObject backup, File tempDir)
            throws IOException, PeriodicBackupException {
        AmazonS3 client = AmazonUtil.getAmazonS3Client(region, credentialsId);

        List<S3ObjectSummary> objectSummarys = getObjectSummaries(client);
        return objectSummarys
                .parallelStream()
                .filter(objectSummary -> objectSummary.getKey()
                        .contains(Util.getFormattedDate(BackupObject.FILE_TIMESTAMP_PATTERN, backup.getTimestamp()))
                        && !objectSummary.getKey().endsWith(BackupObject.EXTENSION)
                        && isMatchPrefix(objectSummary.getKey()))
                .map(S3ObjectSummary::getKey)
                .map(backupFilename -> {
                            Path p = Paths.get(backupFilename).getFileName();
                            if (p == null) {
                                LOGGER.warning("Unable to get file name from: " + backupFilename);
                                return null;
                            }
                            p = Paths.get(tmpDir, p.toString());
                            File copiedFile = p.toFile();
                            try {
                                LOGGER.fine("Copying from: " + bucket + " > " + backupFilename + " to " + copiedFile.getAbsolutePath());
                                IOUtils.copy(client.getObject(bucket, backupFilename).getObjectContent(),
                                        new FileOutputStream(copiedFile));
                                return copiedFile;
                            } catch (Exception e) {
                                LOGGER.warning("Exception while retriving the backup file from S3: " + e);
                                return null;
                            }
                        }
                )
                .filter(java.util.Objects::nonNull)
                ::iterator;
    }

    private List<S3ObjectSummary> getObjectSummaries(AmazonS3 client) {
        ObjectListing objectListing = hudson.Util.fixEmpty(prefix) == null ? client.listObjects(bucket) : client.listObjects(bucket, prefix);
        return objectListing.getObjectSummaries();
    }

    @Override
    public void deleteBackupFiles(BackupObject backupObject) {
        LOGGER.info("Deleting backupObject...");
        String filenamePart = Util.generateFileNameBase(backupObject.getTimestamp());
        AmazonS3 client = AmazonUtil.getAmazonS3Client(region, credentialsId);

        List<S3ObjectSummary> objectSummarys = getObjectSummaries(client);
        for (S3ObjectSummary objectSummary : objectSummarys) {
            if ((objectSummary.getKey() != null && objectSummary.getKey().contains(filenamePart))) {
                LOGGER.info("Deleting backupObject..." + objectSummary.getKey());
                client.deleteObject(bucket, objectSummary.getKey());
                LOGGER.info("Deleted backupObject..." + objectSummary.getKey());
            }
        }
    }

    public String getDisplayName() {
        if (hudson.Util.fixEmpty(prefix) == null) {
            return "S3 bucket: " + bucket;
        } else {
            return "S3 bucket: " + bucket + " > " + prefix;
        }
    }

    @SuppressWarnings("unused")
    public String getBucket() {
        return bucket;
    }

    @SuppressWarnings("unused")
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    @SuppressWarnings("unused")
    public String getTmpDir() {
        return tmpDir;
    }

    @SuppressWarnings("unused")
    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    private boolean isBucketExists() {
        AmazonS3 client = AmazonUtil.getAmazonS3Client(region, credentialsId);

        return client.doesBucketExistV2(bucket);
    }

    public String getPrefix() {
        return prefix;
    }

    @DataBoundSetter
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof S3) {
            S3 that = (S3) o;
            return Objects.equal(this.bucket, that.bucket) && Objects.equal(this.enabled, that.enabled);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hudson.Util.fixEmpty(prefix) == null) {
            return Objects.hashCode(bucket, enabled);
        } else {
            return Objects.hashCode(bucket, enabled, prefix);
        }
    }

    @SuppressWarnings("deprecation")
    @Extension
    public static class DescriptorImpl extends LocationDescriptor {
        public String getDisplayName() {
            return "Amazon S3";
        }

        @RequirePOST
        @Restricted(NoExternalUse.class)
        @RestrictedSince("1.4")
        public FormValidation doTestBucket(@QueryParameter String bucket, @QueryParameter String region,
                                           @QueryParameter String credentialsId) throws AccessDeniedException {
            Jenkins.getActiveInstance().checkPermission(Jenkins.ADMINISTER);
            try {
                return FormValidation.ok(validatePath(bucket, region, credentialsId));
            } catch (FormValidation f) {
                return f;
            }
        }

        private String validatePath(String bucket, String region, String credentialsId) throws FormValidation {
            AmazonS3 client = AmazonUtil.getAmazonS3Client(region, credentialsId);
            if (!client.doesBucketExistV2(bucket)) {
                throw FormValidation.error(bucket + " doesn't exist or I don't have access to it!");
            }
            return "bucket \"" + bucket + "\" OK";
        }

        public ListBoxModel doFillRegionItems() {
            ListBoxModel regions = new ListBoxModel();
            regions.add("Auto", "");
            for (Regions s : Regions.values()) {
                regions.add(s.getDescription(), s.getName());
            }
            return regions;
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems() {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            ListBoxModel credentials = new ListBoxModel();
            credentials.add("IAM instance Profile/user AWS configuration", "");
            credentials.addAll(CredentialsProvider.listCredentials(AmazonWebServicesCredentials.class, Jenkins.get(),
                    ACL.SYSTEM, Collections.emptyList(),
                    CredentialsMatchers.instanceOf(AmazonWebServicesCredentials.class)));
            return credentials;
        }
    }
}
