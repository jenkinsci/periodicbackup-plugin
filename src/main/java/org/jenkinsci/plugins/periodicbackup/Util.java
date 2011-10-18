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

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {
    /**
     *
     * This returns relative path of given file with respect to given base directory
     *
     * @param file input file
     * @param baseDir base directory
     * @return String with relative @file path with respect to its @baseDir
     */
    public static String getRelativePath(final File file, final File baseDir) {
        return baseDir.toURI().relativize(file.toURI()).getPath();
    }

    /**
     *
     * Generates unique file name (without extension)
     *
     * @param date Date object for the timestamp
     * @return unique filename
     */
    public static String generateFileNameBase(Date date) {
        return "backup_" + getFormattedDate(BackupObject.FILE_TIMESTAMP_PATTERN, date);
    }

    /**
     *
     * This returns timestamp String
     *
     * @param pattern A pattern used to format the timestamp
     * @param date Date used as timestamp
     * @return timestamp String
     */
    public static String getFormattedDate(String pattern, Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat.format(date);
    }

    /**
     *
     * Puts the filename and the extension together
     *
     * @param fileName filename without extension
     * @param extension extension of the file
     * @return filename with extension
     */
    public static String createFileName(String fileName, String extension){
        return fileName + "." + extension;
    }

    /**
     *
     * Creates the backupObject File from given BackupObject, fileNameBase and destination path
     *
     * @param backupObject BackupObject given to be serialized
     * @param destinationDir String with path to the directory where the file will be created
     * @param fileNameBase first part of the filename
     * @return serialized BackupObject File
     * @throws IOException If an IO problem occurs
     */
    public static File createBackupObjectFile(BackupObject backupObject, String destinationDir, String fileNameBase) throws IOException {
        File backupObjectFile = new File(destinationDir, createFileName(fileNameBase, BackupObject.EXTENSION));
        String xml = backupObject.getAsString();
        Files.write(xml, backupObjectFile, Charsets.UTF_8);
        return backupObjectFile;
    }

    /**
     *
     * This test if a given file is a valid serialized BackupObject file
     *
     * @param backupObjectFile File to test
     * @return true if valid, false otherwise
     * @throws IOException If an IO problem occurs
     */
    public static boolean isValidBackupObjectFile(File backupObjectFile) throws IOException {
        if(!backupObjectFile.exists() || !(backupObjectFile.getUsableSpace() > 0)) return false;
        else {
            String fileAsString = Files.toString(backupObjectFile, Charsets.UTF_8);
            return fileAsString.contains("fileManager class=\"org.jenkinsci.plugins.periodicbackup") &&
                   fileAsString.contains("storage class=\"org.jenkinsci.plugins.periodicbackup") &&
                   fileAsString.contains("location class=\"org.jenkinsci.plugins.periodicbackup");
        }
    }

    /**
     *
     * Creates FileFilter for files with the given extension
     *
     * @param extension file extension
     * @return FileFilter
     */
    public static FileFilter extensionFileFilter(final String extension) {
        return new FileFilter() {
            public boolean accept(File file) {
                String fileExtension = getExtension(file);
                return fileExtension != null && fileExtension.equals(extension);
            }
        };
    }

    /**
     *
     * This returns extension of the given File object, returns null if the file has no extension
     *
     * @param f given File
     * @return String with the extension of the given file
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if(i == -1) {
            return null;
        }
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static boolean isWritableDirectory(File directory) {
        return (directory.exists() && directory.isDirectory() && directory.canWrite());
    }
}
