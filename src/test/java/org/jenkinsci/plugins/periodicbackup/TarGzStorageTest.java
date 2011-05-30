
package org.jenkinsci.plugins.periodicbackup;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * Author: tblaszcz
 * Date: 14-04-11
 */

public class TarGzStorageTest extends HudsonTestCase {

    private String baseFileName;
    private TarGzStorage tarGzStorage;
    private File tempDirectory;
    private File archive1;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        baseFileName = "baseFileName";
        tarGzStorage = new TarGzStorage();
        tempDirectory = new File(Resources.getResource("data/temp/").getFile());
        if(tempDirectory.exists()) {
            FileUtils.deleteDirectory(tempDirectory);
        }
        assertTrue(tempDirectory.mkdir());
        assertTrue(new File(tempDirectory, "dummy").createNewFile());
        archive1 = new File(Resources.getResource("data/archive1").getFile());
    }

    @Test
    public void testBackupStop() throws Exception {
        tarGzStorage.backupStart(tempDirectory.getAbsolutePath(), baseFileName);
        tarGzStorage.backupAddFile(archive1);
        File expectedResult = new File(tempDirectory, baseFileName + "." + tarGzStorage.getDescriptor().getArchiveFileExtension());

        Iterable<File> files = tarGzStorage.backupStop();

        assertEquals(files.iterator().next(), expectedResult);
    }

    @Test
    public void testUnarchiveFiles() throws IOException {
        File zipArchive1 = new File(Resources.getResource("data/targzfile.tar.gz").getFile());
        assertTrue(zipArchive1.exists());
        List<File> archives = Lists.newArrayList(zipArchive1);
        int filesCountBefore = tempDirectory.listFiles().length;
        int expectedResult = filesCountBefore + 1;

        tarGzStorage.unarchiveFiles(archives, tempDirectory);
        int filesCountAfter = tempDirectory.listFiles().length;

        assertEquals(filesCountAfter, expectedResult);
    }

}
