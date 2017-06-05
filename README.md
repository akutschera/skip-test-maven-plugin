[Travis CI:  ![build status badge](https://travis-ci.org/akutschera/skip-test-maven-plugin.svg?branch=master)](https://travis-ci.org/akutschera/skip-test-maven-plugin)

The skiptest-maven-plugin allows you to run only a portion of your tests, thus reducing the time until your
build is finished.

Motivation
==========

The general idea behind this plugin is that tests that were successful the last time you ran your test suite will
have a higher probability of succeeding this time than a new test or a test that failed previously.
If we believe that, then it should make sense to skip the tests with the highest success rate and only run the tests
we do not have a lot of confidence in yet.

How do I use it?
================
You need to add the maven failsafe plugin to your plugins

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <executions>
          <execution>
            <id>integration-test</id>
            <goals>
              <goal>integration-test</goal>
              <goal>verify</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <argLine>-javaagent:${settings.localRepository}/org/aspectj/aspectjweaver/${aspectjweaver.version}/aspectjweaver-${aspectjweaver.version}.jar</argLine>
        </configuration>
    </plugin>
    
Then you need the skiptest-maven-plugin

    <plugin>
        <groupId>com.github.akutschera</groupId>
        <artifactId>skiptest-maven-plugin</artifactId>
        <version>0.1</version>
        <configuration>
          <allTestResultsFile>/some/dir/outside/of/your/project/all-test-results.txt</allTestResultsFile>
          <skipPercentage>25</skipPercentage>
        </configuration>
        <executions>
          <execution>
            <id>pre-integration-test</id>
            <goals>
              <goal>pre-skip-test</goal>
            </goals>
          </execution>
          <execution>
            <id>post-integration-test</id>
            <goals>
              <goal>post-skip-test</goal>
            </goals>
          </execution>
        </executions>
    </plugin>
    
Because the skiptest-maven-plugin internally uses aspectj to achieve its goals you also need it in your dependency section
    
    <depencencies>
        <dependency>
            <groupId>com.github.akutschera</groupId>
            <artifactId>skiptest-maven-plugin</artifactId>
            <version>0.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

Which tests are run/skipped?
============================

The plugin ties to the maven integration test phase, i.e. it takes all tests that are in classes whose name ends with
"IT". All methods from those classes that are annotated with @org.junit.Test are taken into the pool of test methods
from which we decide what to run and what to skip.

How is it done?
===============

For each test of a project (or multiple projects) we remember the number of successful **consecutive** runs.
Before the integration tests are run, we sort all successful runs by this number and ignore the top n
percent. All the other tests are given to the maven failsafe-plugin to run. New tests or tests that failed the last time
are **always** run and do not count against this percentage.

After all tests are finished, we collect the successful and failed tests and "merge" them with the previous run total.
Successful tests get their number of consecutive succcesses increased by one, failed tests will get that number reset
to 0.

So over time you should get a pretty good list of the tests that have a high success rate and do not need to run that
often. The algorithm ensures that each test will run eventually (depending on the number of new tests introduced and
the skip percentage, this may be sooner or later).

The plugin takes all tests that are in classes that end with IT (the default for tha maven failsafe plugin) and that are
annotated with @org.junit.Test, weaves a little aspect around them that is used to decide if the test should be skipped.

When should I NOT use this plugin?
==============================

This plugin runs in the maven integration test phase. You should not use it if all or most of your tests run in the
maven test phase.
 
This plugin uses code instrumentation to find out which tests should be run. There is a slight performance penalty
when doing that, so you should not use it when all your tests execute fast anyway.
 
Currently this plugin is woven around the test methods only, all code in setup and teardown methods will run, so if you
have a lot of code in those methods, the plugin will work, but you won't save much time.
 
 