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

import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MojoExecutor.class)
public class PreSkipTestMojoTest {
    private PreSkipTestMojo mojo;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File allResultsFile;
    private File skipInputFile;

    @Before
    public void setUp() throws Exception {
        mojo = new PreSkipTestMojo();

        allResultsFile = temporaryFolder.newFile();
        mojo.setAllTestResultsFile( allResultsFile );

        skipInputFile = temporaryFolder.newFile();
        setFieldValue( mojo, "skipInputFile", skipInputFile );
        setFieldValue( mojo, "skiptestsResultsFile", temporaryFolder.newFile() );

        PowerMockito.mockStatic( MojoExecutor.class );
    }

    @Test
    public void executeShouldWriteNothingToSkipInputFileIfSkipPercentageIsZero()
            throws Exception {
        mojo.execute();

        String contents = FileUtils.fileRead( skipInputFile );

        assertTrue( "skipInputFile should have no contents", contents.isEmpty() );
    }

    @Test
    public void executeShouldWriteTwoOfThreeTestsToSkipInputFileIfSkipPercentageIs67()
            throws Exception {
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test1:1" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test2:2" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test3:3" + AbstractSkipTestMojo.CRLF );

        mojo.setSkipPercentage( 67 );
        mojo.execute();

        String contents = FileUtils.fileRead( skipInputFile );
        String[] testsToSkip = contents.split( AbstractSkipTestMojo.CRLF );

        assertThat( "number of tests to skip is unexpected", testsToSkip.length,
                is( 2 ) );
        assertThat(
                "tests to skip should contain test with highest success rate",
                testsToSkip[0], is( "test3" ) );
        assertThat(
                "tests to skip should contain test with second highest success rate",
                testsToSkip[1], is( "test2" ) );

    }

    @Test(expected = MojoExecutionException.class)
    public void executeShouldComplainIfSkipPercentageIsNegative()
            throws Exception {
        mojo.setSkipPercentage( -1 );
        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void executeShouldFailIfSkipPercentageIsMoreThan100()
            throws Exception {
        mojo.setSkipPercentage( 101 );
        mojo.execute();
    }

    @Test
    public void readAllResultsShouldSkipLinesThatDoNotIncludeColon() throws Exception {
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test1:1" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "no colon" + AbstractSkipTestMojo.CRLF );
        FileUtils.fileAppend( allResultsFile.getAbsolutePath(), "test3:3" + AbstractSkipTestMojo.CRLF );

        Map<String, Integer> allResults = mojo.readAllResultsFrom( allResultsFile );

        assertNotNull( "readAllResults must never return null", allResults );
        assertThat( "map should contain both elements with colon", allResults.size(), is( 2 ) );
    }

    @Test
    public void readAllResultsShouldReturnEmptyMapIfFileIsEmpty() throws Exception {
        File emptyFile = temporaryFolder.newFile();
        Map<String, Integer> allResults = mojo.readAllResultsFrom( emptyFile );

        assertNotNull( "readAllResults must never return null", allResults );
        assertTrue( "empty file should produce empty map", allResults.isEmpty() );
    }

    @Test
    public void executeShouldOverwriteResultsFileFromPreviousRunIfItExists() throws Exception {
        File previousResult = temporaryFolder.newFile();

        mojo.setSkiptestsResultsFile( previousResult );
        mojo.execute();

        assertFalse( "previous result should have been deleted first", previousResult.exists() );
    }

    @Test
    public void executeShouldContinueIfNoPreviousResultFileExists() throws Exception {
        mojo.setSkiptestsResultsFile( new File( temporaryFolder.getRoot(), "does-not-exist" ) );
        mojo.execute();
    }

    private void setFieldValue( PreSkipTestMojo plugin, String fieldName,
                                Object value ) throws NoSuchFieldException, IllegalAccessException {
        Field field = findField( plugin.getClass(), fieldName );
        field.setAccessible( true );
        field.set( plugin, value );

    }

    private Field findField( Class clazz, String fieldName ) {
        while ( clazz != null ) {
            try {
                return clazz.getDeclaredField( fieldName );
            } catch ( NoSuchFieldException e ) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new IllegalArgumentException( "Field not found" );
    }
}
