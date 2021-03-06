/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.daemon.configuration

import org.gradle.api.JavaVersion
import org.gradle.api.internal.file.FileResolver
import org.gradle.initialization.BuildLayoutParameters
import org.gradle.internal.jvm.JavaInfo
import org.gradle.process.internal.JvmOptions
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.SetSystemProperties
import org.gradle.util.UsesNativeServices
import org.junit.Rule
import spock.lang.Specification

import java.nio.charset.Charset

@UsesNativeServices
class BuildProcessTest extends Specification {
    @Rule
    final TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    @Rule
    final SetSystemProperties systemPropertiesSet = new SetSystemProperties()

    private def fileResolver = Mock(FileResolver)
    private def currentJvm = Stub(JavaInfo)


    def "current and requested build vm match if vm arguments match"() {
        given:
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.minHeapSize = "16m"
        currentJvmOptions.maxHeapSize = "256m"
        currentJvmOptions.jvmArgs = ["-XX:+HeapDumpOnOutOfMemoryError"]

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        buildProcess.configureForBuild(buildParameters(["-Xms16m", "-Xmx256m", "-XX:+HeapDumpOnOutOfMemoryError"]))
    }

    def "current and requested build vm do not match if vm arguments differ"() {
        given:
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.minHeapSize = "16m"
        currentJvmOptions.maxHeapSize = "1024m"
        currentJvmOptions.jvmArgs = ["-XX:+HeapDumpOnOutOfMemoryError"]

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        !buildProcess.configureForBuild(buildParameters(["-Xms16m", "-Xmx256m", "-XX:+HeapDumpOnOutOfMemoryError"]))
    }

    def "current and requested build vm match if java home matches"() {
        when:
        def buildProcess = new BuildProcess(currentJvm, new JvmOptions(fileResolver))

        then:
        buildProcess.configureForBuild(buildParameters(currentJvm))
        !buildProcess.configureForBuild(buildParameters(Stub(JavaInfo)))
    }

    def "all requested immutable jvm arguments and all immutable system properties need to match"() {
        given:
        def notDefaultEncoding = ["UTF-8", "US-ASCII"].collect { Charset.forName(it) } find { it != Charset.defaultCharset() }
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.setAllJvmArgs(["-Dfile.encoding=$notDefaultEncoding", "-Xmx100m", "-XX:SomethingElse"])

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        !buildProcess.configureForBuild(buildParameters(["-Dfile.encoding=$notDefaultEncoding"])) //only properties match
        !buildProcess.configureForBuild(buildParameters(["-Xmx100m", "-XX:SomethingElse"])) //only jvm argument match
        !buildProcess.configureForBuild(buildParameters(["-Dfile.encoding=${Charset.defaultCharset().name()}", "-Xmx100m", "-XX:SomethingElse"])) //encoding does not match
        !buildProcess.configureForBuild(buildParameters(["-Dfile.encoding=$notDefaultEncoding", "-Xmx120m", "-XX:SomethingElse"])) //memory does not match
        buildProcess.configureForBuild(buildParameters(["-Dfile.encoding=$notDefaultEncoding", "-Xmx100m", "-XX:SomethingElse"])) //both match
    }

    def "current and requested build vm match if no arguments are requested"() {
        given:
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.minHeapSize = "16m"
        currentJvmOptions.maxHeapSize = "1024m"
        currentJvmOptions.jvmArgs = ["-XX:+HeapDumpOnOutOfMemoryError"]
        def emptyRequest = buildParameters([])

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        buildProcess.configureForBuild(emptyRequest)
    }

    def "current VM does not match if it was started with the default client heap size"() {
        given:
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.maxHeapSize = "64m"
        def defaultRequest = buildParameters(null as Iterable)

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        !buildProcess.configureForBuild(defaultRequest)
    }

    def "current and requested build vm match if no arguments are requested even if the daemon defaults are applied"() {
        //if the user does not configure any jvm args Gradle uses some defaults
        //however, we don't want those defaults to influence the decision whether to use existing process or not
        given:
        def requestWithDefaults = buildParameters((Iterable) null)

        when:
        def buildProcess = new BuildProcess(currentJvm, new JvmOptions(fileResolver))

        then:
        requestWithDefaults.getEffectiveJvmArgs().containsAll(DaemonParameters.DEFAULT_JVM_ARGS)
        buildProcess.configureForBuild(requestWithDefaults)
    }

    def "current and requested build vm match if only mutable arguments are requested"() {
        given:
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.minHeapSize = "16m"
        currentJvmOptions.maxHeapSize = "1024m"
        currentJvmOptions.jvmArgs = ["-XX:+HeapDumpOnOutOfMemoryError"]
        def requestWithMutableArgument = buildParameters(["-Dfoo=bar"])

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        buildProcess.configureForBuild(requestWithMutableArgument)
    }

    def "current and requested build vm match if only mutable arguments vary"() {
        given:
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.setAllJvmArgs(["-Xmx100m", "-XX:SomethingElse", "-Dfoo=bar", "-Dbaz"])

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        !buildProcess.configureForBuild(buildParameters(["-Xms10m", "-Dfoo=bar", "-Dbaz"]))
        !buildProcess.configureForBuild(buildParameters(["-XX:SomethingElse", "-Dfoo=bar", "-Dbaz"]))
        buildProcess.configureForBuild(buildParameters(["-Xmx100m", "-XX:SomethingElse", "-Dfoo=bar", "-Dbaz"]))
        buildProcess.configureForBuild(buildParameters(["-Xmx100m", "-XX:SomethingElse"]))
    }

    def "debug is an immutable argument"() {
        given:
        def debugEnabled = buildParameters([])
        debugEnabled.setDebug(true)
        def debugDisabled = buildParameters([])
        debugDisabled.setDebug(false)

        when:
        BuildProcess buildProcess = new BuildProcess(currentJvm, new JvmOptions(fileResolver))

        then:
        !buildProcess.configureForBuild(debugEnabled)
        buildProcess.configureForBuild(debugDisabled)
    }

    def "immutable system properties are treated as immutable"() {
        given:
        def notDefaultEncoding = ["UTF-8", "US-ASCII"].collect { Charset.forName(it) } find { it != Charset.defaultCharset() }
        def notDefaultLanguage = ["es", "jp"].find { it != Locale.default.language }
        def currentJvmOptions = new JvmOptions(fileResolver)
        currentJvmOptions.setAllJvmArgs(["-Xmx100m", "-XX:SomethingElse", "-Dfoo=bar", "-Dbaz"])

        when:
        def buildProcess = new BuildProcess(currentJvm, currentJvmOptions)

        then:
        buildProcess.configureForBuild(buildParameters(["-Dfile.encoding=${Charset.defaultCharset().name()}"]))
        buildProcess.configureForBuild(buildParameters(["-Duser.language=${Locale.default.language}"]))
        !buildProcess.configureForBuild(buildParameters(["-Dfile.encoding=$notDefaultEncoding"]))
        !buildProcess.configureForBuild(buildParameters(["-Duser.language=$notDefaultLanguage"]))
        !buildProcess.configureForBuild(buildParameters(["-Dcom.sun.management.jmxremote"]))
        !buildProcess.configureForBuild(buildParameters(["-Djava.io.tmpdir=/some/custom/folder"]))
    }

    def "immutable system properties passed into the daemon parameter constructor are handled"() {
        given:
        def notDefaultEncoding = ["UTF-8", "US-ASCII"].collect { Charset.forName(it) } find { it != Charset.defaultCharset() }

        when:
        BuildProcess buildProcess = new BuildProcess(currentJvm, new JvmOptions(fileResolver))

        then:
        buildProcess.configureForBuild(buildParameters([], [ "file.encoding" : Charset.defaultCharset().name() ]))
        !buildProcess.configureForBuild(buildParameters([], [ "file.encoding" : notDefaultEncoding.toString() ]))
    }

    def "sets all mutable system properties before running build"() {
        when:
        def parameters = buildParameters(["-Dfoo=bar", "-Dbaz"])

        then:
        new BuildProcess(currentJvm, new JvmOptions(fileResolver)).configureForBuild(parameters)

        and:
        System.getProperty('foo') == 'bar'
        System.getProperty('baz') != null
    }

    def "user can explicitly disable default daemon args by setting jvm args to empty"() {
        given:
        def emptyBuildParameters = buildParameters([])

        when:
        new BuildProcess(currentJvm, new JvmOptions(fileResolver)).configureForBuild(emptyBuildParameters)

        then:
        !emptyBuildParameters.getEffectiveJvmArgs().containsAll(DaemonParameters.DEFAULT_JVM_ARGS)
    }

    def "user-defined vm args that correspond to daemon default are considered during matching"() {
        given:
        def parametersWithDefaults = buildParameters(DaemonParameters.DEFAULT_JVM_ARGS)

        when:
        def buildProcess = new BuildProcess(currentJvm, new JvmOptions(fileResolver))

        then:
        !buildProcess.configureForBuild(parametersWithDefaults)
    }

    private DaemonParameters buildParameters(Iterable<String> jvmArgs) {
        return buildParameters(currentJvm, jvmArgs)
    }

    private DaemonParameters buildParameters(Iterable<String> jvmArgs, Map<String, String> extraSystemProperties) {
        return buildParameters(currentJvm, jvmArgs, extraSystemProperties)
    }

    private static DaemonParameters buildParameters(JavaInfo jvm, Iterable<String> jvmArgs = [], Map<String, String> extraSystemProperties = Collections.emptyMap()) {
        def parameters = new DaemonParameters(new BuildLayoutParameters(), extraSystemProperties)
        parameters.setJvm(jvm)
        if (jvmArgs != null) {
            parameters.setJvmArgs(jvmArgs)
        }
        parameters.applyDefaultsFor(JavaVersion.VERSION_1_7)
        return parameters
    }
}
