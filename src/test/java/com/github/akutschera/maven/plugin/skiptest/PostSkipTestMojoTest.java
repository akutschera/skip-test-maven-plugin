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

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostSkipTestMojoTest {

    private PostSkipTestMojo mojo;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File allResultsFile;
    private File testReportsFile;

    @Before
    public void setUp() throws Exception {
        mojo = new PostSkipTestMojo();

        testReportsFile = temporaryFolder.newFile();
        mojo.setTestReportsFile( testReportsFile );
    }

    @Test
    public void allResultsFileShouldBeCreatedFromTestResultsFileIfItDoesNotExist()
            throws Exception {
        allResultsFile = new File( temporaryFolder.newFolder(), "does-not-exist" );
        mojo.setAllTestResultsFile( allResultsFile );
        FileUtils.fileAppend( testReportsFile.getAbsolutePath(), "test1:1" );
        assertFalse( "all results file should NOT exist before execution",
                allResultsFile.exists() );

        mojo.execute();

        assertTrue( "all results file should  exist after execution",
                allResultsFile.exists() );
        String allResults = FileUtils.fileRead( allResultsFile );
        assertThat( "all results should contain data from test", allResults,
                containsString( "test1:1" ) );

    }

    @Test
    public void executeShouldMergeTestReportWithAllResults() throws Exception {
        allResultsFile = temporaryFolder.newFile();
        mojo.setAllTestResultsFile( allResultsFile );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test1:1" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test2:1" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test3:1" + AbstractSkipTestMojo.CRLF );

        FileUtils.fileAppend( testReportsFile.getAbsolutePath(), "test1:1" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( testReportsFile.getAbsolutePath(), "test2:0" );

        mojo.execute();

        String allResults = FileUtils.fileRead( allResultsFile );
        assertThat( "all results should contain merged data from test",
                allResults, containsString( "test1:2" ) );
        assertThat( "all results should NOT contain data from failed test",
                allResults, not( containsString( "test2" ) ) );
        assertThat(
                "all results should contain data from all tests (even those that did not run)",
                allResults, containsString( "test3:1" ) );
    }

    @Test
    public void executeShouldLeaveTestReportsUntouchedIfAllTestsWereSkipped()
            throws Exception {
        allResultsFile = temporaryFolder.newFile();
        mojo.setAllTestResultsFile( allResultsFile );
        mojo.setTestReportsFile( new File( temporaryFolder.getRoot(),
                "does-not-exist" ) );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test1:1" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test2:2" + AbstractSkipTestMojo.CRLF );

        mojo.execute();

        String allResults = FileUtils.fileRead( allResultsFile );
        assertThat( "all results should contain original data from test 1",
                allResults, containsString( "test1:1" ) );
        assertThat(
                "all results should contain data from all tests (even those that did not run)",
                allResults, containsString( "test2:2" ) );
    }

}
