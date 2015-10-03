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
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.util.FileUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Aspect
public class SkipAspect {

    private static final String SKIP_INPUT_FILE = "skip.txt";
    private static final String SKIPTEST_OUTPUTFILE = "skiptest.results";
    static final String CRLF = System.getProperty( "line.separator" );
    private Set<String> testsToSkip;
    private final File buildDir;
    private final String inputFileName;
    private final String outputFileName;

    public SkipAspect() {
        this( SKIP_INPUT_FILE, SKIPTEST_OUTPUTFILE, new File(
                System.getProperty( "user.dir" ), "target" ) );
    }

    public SkipAspect( String inputFile, String outputFile, File buildDir ) {
        inputFileName = inputFile;
        outputFileName = outputFile;
        this.buildDir = buildDir;
        getListOfTestsToSkip();
    }

    @Pointcut(value = "@annotation(expected)")
    protected void annotatedWithTest( Test expected ) {
    }

    @Around("execution(public * *.*IT.*(..)) && annotatedWithTest(ann)")
    public void aroundJUnitTestCaseMethods( ProceedingJoinPoint pjp, Test ann )
            throws Throwable {
        int isTestSuccessful = 0;

        if ( shouldTestBeSkipped( pjp ) ) {
            return;
        }
        try {
            pjp.proceed();
            if ( ann.expected() == Test.None.class ) {
                isTestSuccessful = 1;
            }
        } catch ( Throwable throwable ) {
            Class<? extends Throwable> expectedException = ann.expected();
            if ( expectedException.isAssignableFrom( throwable.getClass() ) ) {
                isTestSuccessful = 1;
            } else {
                throw throwable;
            }
        } finally {
            appendTestresultToOutputFile( getNameOfTestFrom( pjp ),
                    isTestSuccessful );
        }
    }

    private void appendTestresultToOutputFile( String testName,
                                               int isTestSuccessful ) throws IOException {
        File outputFile = new File( buildDir, outputFileName );
        try ( FileWriter fw = new FileWriter( outputFile, true ) ) {
            fw.write( testName + ":" + isTestSuccessful + CRLF );
        }
    }

    private boolean shouldTestBeSkipped( ProceedingJoinPoint pjp ) {
        return testsToSkip.contains( getNameOfTestFrom( pjp ) );
    }

    private String getNameOfTestFrom( ProceedingJoinPoint pjp ) {
        String className = pjp.getThis().getClass().getName();
        String testToExecute = pjp.getSignature().getName();
        return className + "." + testToExecute;
    }

    private void getListOfTestsToSkip() {
        testsToSkip = new HashSet<>();
        File skipFile = new File( buildDir, inputFileName );

        if ( skipFile.exists() ) {
            try {
                String skipList = FileUtil.readAsString( skipFile );
                for ( String singleTest : skipList.split( CRLF ) ) {
                    testsToSkip.add( singleTest );
                }
            } catch ( IOException e ) {
                // file cannot be read, execute everything
            }
        }
    }
}
