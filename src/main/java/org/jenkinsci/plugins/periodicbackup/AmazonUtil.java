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

import java.util.Collections;
import java.util.Optional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;

import hudson.security.ACL;
import jenkins.model.Jenkins;

class AmazonUtil {

    static AmazonS3 getAmazonS3Client(String region, String credentialsId) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        // Use credentials if provided. If not, it'll use the credentials in aws profile
        // ~/.aws from host
        if (hudson.Util.fixEmpty(region) != null) {
            builder.setRegion(region);
        }
        if (hudson.Util.fixEmpty(credentialsId) != null) {
            builder.setCredentials(getCredentials(credentialsId));
        }

        return builder.build();
    }

    static AmazonWebServicesCredentials getCredentials(String credentialsId) {
        Optional<AmazonWebServicesCredentials> credential = CredentialsProvider
                .lookupCredentials(AmazonWebServicesCredentials.class, Jenkins.get(), ACL.SYSTEM,
                        Collections.emptyList())
                .stream().filter(it -> it.getId().equals(credentialsId)).findFirst();
        if (credential.isPresent()) {
            return credential.get();
        } else {
            return null;
        }
    }
}
