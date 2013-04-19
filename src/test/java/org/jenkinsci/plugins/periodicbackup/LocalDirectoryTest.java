package org.jenkinsci.plugins.periodicbackup;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Author: tblaszcz
 * Date: 17-01-11
 */
public class LocalDirectoryTest extends HudsonTestCase {
    @Test
    public void testGetAvailableBackups() {
        File path = new File(Resources.getResource("data/").getFile());
        LocalDirectory localDirectory = new LocalDirectory(path, true);
        Date expectedDate = new Date(123);
        BackupObject expected = new BackupObject(
                new FullBackup(""),
                new ZipStorage(false, 0),
                new LocalDirectory(new File("c:\\Temp"), true),
                expectedDate);

        Iterable<BackupObject> obtainedResult = localDirectory.getAvailableBackups();
        List<BackupObject> listFromResult = Lists.newArrayList(obtainedResult);
        assertEquals(listFromResult.size(), 1);
        assertEquals(expected, listFromResult.get(0));
    }

    @Test
    public void testStoreBackupInLocation() throws IOException {
        File destination =      new File(Resources.getResource("data/destination/").getFile());
        File archive1 =         new File(Resources.getResource("data/archive1").getFile());
        File archive2 =         new File(Resources.getResource("data/archive2").getFile());
        File backupObjectFile = new File(Resources.getResource("data/test.pbobj").getFile());
        List<File> archives = Lists.newArrayList(archive1, archive2);
        LocalDirectory localDirectory = new LocalDirectory(destination, true);

        localDirectory.storeBackupInLocation(archives, backupObjectFile);
        File backupObjectFileInLocation = new File(Resources.getResource("data/destination/test.pbobj").getFile());
        assertTrue(backupObjectFileInLocation.exists());
        assertEquals(backupObjectFileInLocation.getUsableSpace(), backupObjectFile.getUsableSpace());
        File[] filesInLocation = destination.listFiles();
        assertTrue(filesInLocation.length == 4);  //2 archives + backup object file + dummy == 4

        for(File f : filesInLocation) {
            if(!f.delete()) {
                throw new IOException("Could not delete file " + f.getAbsolutePath());
            }
        }
    }

    @Test
    public void testRetrieveBackupFromLocation() throws Exception {
        File sourceDir = new File(Thread.currentThread().getContextClassLoader().getResource("data/").getFile());
        File tempDirectory = new File(Thread.currentThread().getContextClassLoader().getResource("data/temp").getFile());
        Date testDate = new Date(123-(Calendar.getInstance().get(Calendar.ZONE_OFFSET)));
        LocalDirectory localDirectory = new LocalDirectory(sourceDir, true);
        BackupObject backupObject = new BackupObject(new FullBackup(""), new ZipStorage(false, 0), localDirectory, testDate);

        Iterable<File> result = localDirectory.retrieveBackupFromLocation(backupObject, tempDirectory);
        File expectedResult = new File(tempDirectory, Util.createFileName(Util.generateFileNameBase(testDate), "zip"));

        assertEquals(result.iterator().next(), expectedResult );
        assertTrue(expectedResult.exists());
    }
}
