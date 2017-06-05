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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Skips tests when necessary. All tests for this project are ordered by the number of times they have
 * successfully been executed. A new test or a test that failed during the last run is automatically
 * given a successful run number of 0.
 *
 * @author Andreas Kutschera
 * @version 0.1
 * @since 0.1
 */
@Mojo(name = "pre-skip-test", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true
, requiresDependencyResolution = ResolutionScope.TEST )
public class PreSkipTestMojo extends AbstractSkipTestMojo {

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/skip.txt")
    private File skipInputFile;


    @Parameter(readonly = true, defaultValue = "${project.build.directory}/skiptest.results")
    private File skiptestsResultsFile;

    /** Defines the percentage of tests to skip. Must be a value between 0 and 100. 0 or 100 are
     * permitted, even though you can achieve the same results by other - cheaper - means.
     * The top &lt;skip.percentage&gt; tests are not executed. The default value is 0 (i.e. no tests
     * are skipped).
     *
     */
    @Parameter( defaultValue = "0" )
    private int skipPercentage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throwExceptionIfParametersAreOutOfRange();
        deleteResultFromPreviousRun();
        if ( skipPercentage == 0 ) {
            getLog().info( "skip percentage is 0, will not skip anything" );
            return;
        }


        Map<String, Integer> allResults = readAllResultsFrom( getAllTestResultsFile() );
        List<Entry<String, Integer>> allResultsAsList = new ArrayList<>(
                allResults.entrySet() );

        Comparator<? super Entry<String, Integer>> compareTestResults = new Comparator<Entry<String, Integer>>() {

            @Override
            public int compare( Entry<String, Integer> o1,
                                Entry<String, Integer> o2 ) {
                return o2.getValue() - o1.getValue();
            }
        };
        Collections.sort( allResultsAsList, compareTestResults );
        int lastTestToSkip = ( allResultsAsList.size() * getSkipPercentage() ) / 100;
        getLog().debug(
                "skip percentage is " + getSkipPercentage() + ", will skip "
                        + lastTestToSkip + " tests." );
        try {
            for ( int i = 0; i < lastTestToSkip; i++ ) {
                String testToExecute = allResultsAsList.get( i ).getKey();
                FileUtils.fileAppend( skipInputFile.getAbsolutePath(),
                        testToExecute + CRLF );
            }
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void deleteResultFromPreviousRun() {
        if ( skiptestsResultsFile.exists() ) {
            if ( !skiptestsResultsFile.delete() ) {
                getLog().error( "could not delete result file " + skiptestsResultsFile.getAbsolutePath()
                        + ". This max lead to inconsistent results history" );
            }
        }
    }

    private void throwExceptionIfParametersAreOutOfRange()
            throws MojoExecutionException {
        if ( getSkipPercentage() < 0 || getSkipPercentage() > 100 ) {
            throw new MojoExecutionException(
                    "skip percentage must be between 0 and 100" );
        }
    }

    public int getSkipPercentage() {
        return skipPercentage;
    }

    public void setSkipPercentage( int skipPercentage ) {
        this.skipPercentage = skipPercentage;
    }

    void setSkiptestsResultsFile( File skiptestsResultsFile ) {
        this.skiptestsResultsFile = skiptestsResultsFile;
    }
}
