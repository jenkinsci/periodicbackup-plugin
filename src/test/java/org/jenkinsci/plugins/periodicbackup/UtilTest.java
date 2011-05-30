package org.jenkinsci.plugins.periodicbackup;

import com.google.common.io.Resources;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * Author: tblaszcz
 * Date: 19-01-11
 */
public class UtilTest extends TestCase {
    @Test
    public void testGetRelativePath() throws Exception {
        File file = new File(Resources.getResource("data/temp/dummy").getFile());
        File baseDirectory = new File(Resources.getResource("data/").getFile());

        String result = Util.getRelativePath(file, baseDirectory);
        String expectedResult = "temp/dummy";

        assertTrue(result.equals(expectedResult));
    }

    @Test
    public void testGenerateFileName() throws Exception {
        String filenameBase = Util.generateFileNameBase(new Date());

        assertTrue(filenameBase.length() > 0);
    }

    @Test
    public void testCreateFileName() throws Exception {
        String filenameBase = "backup";
        String extension = "pbobj";
        String filename = Util.createFileName(filenameBase, extension);

        assertTrue(filename.length() == (filenameBase.length() + extension.length() + 1));
    }

    @Test
    public void testCreateBackupObjectFile() throws Exception {
        File tempDirectory = new File(Resources.getResource("data/temp").getFile());
        BackupObject backupObject = new BackupObject(new FullBackup(), new ZipStorage(false, 0), new LocalDirectory(tempDirectory, true), new Date());
        String fileNameBase = "backupfile";

        File result = Util.createBackupObjectFile(backupObject, tempDirectory.getAbsolutePath(), fileNameBase);
        String expectedFileName = fileNameBase + "." + BackupObject.EXTENSION;

        assertTrue(result.exists());
        assertEquals(result.getName(), expectedFileName);
    }

    @Test
    public void testIsValidBackupObjectFile() throws Exception {
        File backupObjectFile = new File(Resources.getResource("data/test.pbobj").getFile());
        File notBackupObjectFile = new File(Resources.getResource("data/archive1").getFile());
        assertFalse(Util.isValidBackupObjectFile(notBackupObjectFile));
        assertTrue(Util.isValidBackupObjectFile(backupObjectFile));
    }

}

