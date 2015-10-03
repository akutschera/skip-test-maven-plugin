/*
 * Copyright 2015 Andreas Kutschera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.akutschera.maven.plugin.skiptest;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public abstract class AbstractSkipTestMojo extends AbstractMojo {

    protected static final String CRLF = System.getProperty( "line.separator" );

    /**
     * This file contains all test results from all runs (including previous runs).
     * By default it is written to ${project.build.directory}/all-test-results.txt
     * If you want to keep your test results so you can skip some tests later,
     * you should overwrite this property to point to a file that maven clean
     * will not erase.<p>
     *     <b>NB:</b> If you remove tests from your test suite, they will remain
     *     in this file until somebody removes them by hand (and until then
     *     they will count against tests to be run or skipped).
     * </p>
     */
    @Parameter(defaultValue = "${project.build.directory}/all-test-results.txt")
    private File allTestResultsFile;


    protected Map<String, Integer> readAllResultsFrom( File file ) {
        Map<String, Integer> allTestResults = new HashMap<>();
        if ( !file.exists() ) {
            return allTestResults;
        }
        try {
            for ( String line : FileUtils.fileRead( file ).split( CRLF ) ) {
                if ( line.contains( ":" ) ) {
                    String[] split = line.split( ":" );
                    allTestResults.put( split[0], new Integer( split[1] ) );
                }
            }
        } catch ( IOException e ) {
            getLog().error(
                    "cannot read results file: " + file.getAbsolutePath()
                            + ", will treat it as non-existent", e );
        }
        return allTestResults;
    }

    public File getAllTestResultsFile() {
        return allTestResultsFile;
    }

    public void setAllTestResultsFile( File allTestResultsFile ) {
        this.allTestResultsFile = allTestResultsFile;
    }
}
