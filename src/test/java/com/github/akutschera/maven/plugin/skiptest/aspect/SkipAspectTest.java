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
package com.github.akutschera.maven.plugin.skiptest.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SkipAspectTest {

    private SkipAspect skipAspect;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProceedingJoinPoint pjp;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File resultFile;
    private File skipFile;
    @Mock
    private org.junit.Test annotation;

    @Before
    public void setUp() throws Exception {
        resultFile = temporaryFolder.newFile();
        skipFile = temporaryFolder.newFile();
    }

    @Test
    public void skipWhenTestIsInListOfTestsToSkip() throws Throwable {
        FileUtils.fileWrite( skipFile.getAbsolutePath(),
                "java.lang.String.skipMe" );
        when( pjp.getThis() ).thenReturn( "foo" );
        when( pjp.getSignature().getName() ).thenReturn( "skipMe" );

        skipAspect = new SkipAspect( skipFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        verify( pjp, never() ).proceed();

        String resultsFile = FileUtils.fileRead( resultFile );
        assertThat( "results file should be empty", resultsFile, is( "" ) );
    }

    @Test
    public void executeWhenShortNameIsNotInListOfTestsToSkip() throws Throwable {
        FileUtils.fileWrite( skipFile.getAbsolutePath(), "skipMe" );
        when( pjp.getSignature().getName() ).thenReturn( "executeMe" );

        skipAspect = new SkipAspect( skipFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        verify( pjp ).proceed();
    }

    @Test
    public void testShouldBeExecutedWhenSkipFileDoesNotExist() throws Throwable {
        FileUtils.fileDelete( skipFile.getAbsolutePath() );

        skipAspect = new SkipAspect( skipFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        verify( pjp ).proceed();
    }

    @Test
    public void executeShouldCatchExceptionsWhenTheyAreDeclaredInTestAnnotations()
            throws Throwable {
        Mockito.doReturn( Exception.class ).when( annotation ).expected();

        when( pjp.getSignature().getName() ).thenReturn( "executeMe" );
        when( pjp.proceed() ).thenThrow( new Exception( "foo" ) );

        skipAspect = new SkipAspect( skipFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

    }

    @Test(expected = Exception.class)
    public void executeShouldRethrowExceptionsWhenTheyAreNotDeclaredInTestAnnotations()
            throws Throwable {

        Mockito.doReturn( IllegalArgumentException.class ).when( annotation )
                .expected();
        when( pjp.getSignature().getName() ).thenReturn( "executeMe" );
        when( pjp.proceed() ).thenThrow( new Exception( "foo" ) );

        skipAspect = new SkipAspect();
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

    }

    @Test
    public void executedTestShouldWriteResultsFile() throws Throwable {
        doReturn( Test.None.class ).when( annotation ).expected();
        FileUtils.fileDelete( skipFile.getAbsolutePath()  );
        FileUtils.fileWrite( resultFile.getAbsolutePath(),
                "previousTest:0" + SkipAspect.CRLF );
        when( pjp.getThis() ).thenReturn( "foo" );
        when( pjp.getSignature().getName() ).thenReturn( "executeMe" );

        skipAspect = new SkipAspect( skipFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        assertTrue( "results file should have been written",
                resultFile.exists() );

        String resultsFile = FileUtils.fileRead( resultFile );
        assertThat( "results should contain name of executed test", resultsFile,
                containsString( "java.lang.String.executeMe:1" ) );
        assertThat( "results should contain name of previous test", resultsFile,
                containsString( "previousTest:0" ) );
    }

    @Test
    public void executedTestShouldAppendFalseToResultFileIfExpectedExceptionIsNotThrown()
            throws Throwable {
        when( pjp.getSignature().getName() ).thenReturn( "first" );
        Mockito.doReturn( Exception.class ).when( annotation ).expected();

        skipAspect = new SkipAspect( skipFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        when( pjp.getSignature().getName() ).thenReturn( "second" );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        String resultsFile = FileUtils.fileRead( resultFile );
        String[] lines = resultsFile.split( SkipAspect.CRLF );
        assertThat( "results file should contain one line for each test",
                lines.length, is( 2 ) );
        assertThat(
                "results should contain false result of first executed test",
                lines[0], containsString( "first:0" ) );
        assertThat(
                "results should contain false result of second executed test",
                lines[1], containsString( "second:0" ) );
    }

    @Test
    public void testShouldBeExecutedWhenSkipFileCannotBeRead() throws Throwable {
        File nonReadableFile = temporaryFolder.newFolder();
        skipAspect = new SkipAspect( nonReadableFile.getName(), resultFile.getName(),
                skipFile.getParentFile() );
        skipAspect.aroundJUnitTestCaseMethods( pjp, annotation );

        verify( pjp ).proceed();
    }

}
