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

import hudson.model.Hudson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * This RestorePolicy assumes that at this stage all the files in tempDir are the files to be restored.
 * The whole content of the tempDir will be copied to the home directory and the existing files will be overwritten.
 */
public class OverwriteRestorePolicy implements RestorePolicy {

    private static final Logger LOGGER = Logger.getLogger(OverwriteRestorePolicy.class.getName());

    public void restore(File tempDir) throws IOException {
        File hudsonRoot = Hudson.getInstance().getRootDir();

        FileUtils.copyDirectory(tempDir, hudsonRoot);
        LOGGER.info("Restoring of files finished");
    }

}
