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


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Gather test results and merge them with the test results from previous runs. All test results
 * are written to the testReportsFile. This goal "merges" the test results file with the results
 * from previous runs. If a test was successful, its number of successful consecutive runs is
 * incremented by one, otherwise it is reset to 0, thus ensuring that this test will be executed
 * next time (unless you skip all tests).
 *
 * @author Andreas Kutschera
 * @version 0.1
 * @since 0.1
 */
@Mojo(name = "post-skip-test", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class PostSkipTestMojo extends AbstractSkipTestMojo {

    /**
     * File where all test reports were written to.
     */
    @Parameter(readonly = true, defaultValue = "${project.build.directory}/skiptest.results")
    private File testReportsFile;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, Integer> allResults = readAllResultsFrom( getAllTestResultsFile() );


        Map<String, Integer> resultsFromThisRun = readAllResultsFrom( testReportsFile );
        getLog().debug(
                "will merge " + resultsFromThisRun.size() + " with all " + allResults.size() + " tests" );

        for ( Entry<String, Integer> entry : resultsFromThisRun.entrySet() ) {
            mergeTestResultWithAllResults( allResults, entry );
        }

        writeToOutputfile( allResults );

    }

    private void writeToOutputfile( Map<String, Integer> allResults ) {
        FileUtils.fileDelete( getAllTestResultsFile().getAbsolutePath() );
        try {
            for ( String testName : allResults.keySet() ) {
                FileUtils.fileAppend( getAllTestResultsFile().getAbsolutePath(),
                        testName + ":" + allResults.get( testName ) + CRLF );
            }
            getLog().debug(
                    "merged test results are in " + getAllTestResultsFile().getAbsolutePath() );
        } catch ( IOException e ) {
            getLog().error(
                    "cannot write results file: "
                            + getAllTestResultsFile().getAbsolutePath()
                            + ", this may lead to unexpected runs in the future",
                    e );
        }
    }

    private void mergeTestResultWithAllResults( Map<String, Integer> allResults,
                                                Entry<String, Integer> oneTest ) {
        Integer previousSuccesses = getPreviousNumberOfSuccessfulTestRuns(
                allResults, oneTest.getKey() );
        if ( wasSucessful( oneTest ) ) {
            allResults.put( oneTest.getKey(), previousSuccesses + 1 );
        } else {
            allResults.remove( oneTest.getKey() );
        }
    }

    private boolean wasSucessful( Entry<String, Integer> oneTest ) {
        return oneTest.getValue() == 1;
    }

    private Integer getPreviousNumberOfSuccessfulTestRuns(
            Map<String, Integer> allResults, String testName ) {
        Integer previousSuccesses;
        if ( allResults.containsKey( testName ) ) {
            previousSuccesses = allResults.get( testName );
        } else {
            previousSuccesses = 0;
        }
        return previousSuccesses;
    }

    // I use aspects, but I do not like reflection, so I write a setter that I can use in tests, hm...
    final void setTestReportsFile( File testReportsFile ) {
        this.testReportsFile = testReportsFile;
    }


}
