package org.jenkinsci.plugins.periodicbackup;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.ArchiverException;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Author: tblaszcz
 * Date: 19-01-11
 */
public class ZipStorageTest extends HudsonTestCase {

    private String baseFileName;
    private ZipStorage zipStorage;
    private File tempDirectory;
    private File archive1;
    private File archive2;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        baseFileName = "baseFileName";
        zipStorage = new ZipStorage(false, 0);
        tempDirectory = new File(Resources.getResource("data/temp2/").getFile());
        if(tempDirectory.exists()) {
            FileUtils.deleteDirectory(tempDirectory);
        }
        assertTrue(tempDirectory.mkdir());
        assertTrue(new File(tempDirectory, "dummy").createNewFile());

        archive1 = new File(Resources.getResource("data/archive1").getFile());
        archive2 = new File(Resources.getResource("data/archive2").getFile());
        assertTrue(tempDirectory.getAbsolutePath() != null);
    }

    @Test
    public void testBackupStart() throws IOException, ArchiverException, PeriodicBackupException {
        assertTrue(tempDirectory.getAbsolutePath() != null);
        zipStorage.backupStart(tempDirectory.getAbsolutePath(), baseFileName);

        assertEquals(zipStorage.getArchivesNumber(), 1);
        assertEquals(zipStorage.getCurrentArchiveFilesCount(), 0);
        assertEquals(zipStorage.getCurrentArchiveTotalFilesSize(), 0l);

        zipStorage.backupStop();
    }

    @Test
    public void testBackupAddFile() throws IOException, ArchiverException, PeriodicBackupException {
        zipStorage.backupStart(tempDirectory.getAbsolutePath(), baseFileName);
        int sizeBefore = zipStorage.getArchiver().getFiles().size();
        int filesInArchiveBefore = zipStorage.getCurrentArchiveFilesCount();
        long sizeOfFilesInArchiveBefore = zipStorage.getCurrentArchiveTotalFilesSize();
        long sizeOfTheFile = archive1.length();

        zipStorage.backupAddFile(archive1);
        int sizeAfter = zipStorage.getArchiver().getFiles().size();
        int filesInArchiveAfter = zipStorage.getCurrentArchiveFilesCount();
        long sizeOfFilesInArchiveAfter = zipStorage.getCurrentArchiveTotalFilesSize();

        assertEquals(sizeBefore + 1, sizeAfter);
        assertEquals(filesInArchiveBefore + 1, filesInArchiveAfter);
        assertEquals (sizeOfFilesInArchiveBefore + sizeOfTheFile, sizeOfFilesInArchiveAfter);

        zipStorage.backupStop();
    }

    @Test
    public void testBackupStop() throws Exception {
        File expectedResult = new File(tempDirectory, baseFileName + "_1." + zipStorage.getDescriptor().getArchiveFileExtension());

        zipStorage.backupStart(tempDirectory.getAbsolutePath(), baseFileName);
        zipStorage.backupAddFile(archive1);
        zipStorage.backupAddFile(archive2);
        zipStorage.backupStop();

        assertTrue(expectedResult.exists());

        if(!expectedResult.delete()) {
            throw new IOException("Could not delete file " + expectedResult.getAbsolutePath());
        }
    }

    @Test
    public void testUnarchiveFiles() throws IOException {
        File zipArchive1 = new File(Resources.getResource("data/zipfile1.zip").getFile());
        File zipArchive2 = new File(Resources.getResource("data/zipfile2.zip").getFile());
        assertTrue(zipArchive1.exists() && zipArchive2.exists());
        List<File> archives = Lists.newArrayList(zipArchive1, zipArchive2);
        int filesCountBefore = tempDirectory.listFiles().length;
        int expectedResult = filesCountBefore + 2;

        zipStorage.unarchiveFiles(archives, tempDirectory);
        int filesCountAfter = tempDirectory.listFiles().length;

        assertEquals(filesCountAfter, expectedResult);
    }
}
