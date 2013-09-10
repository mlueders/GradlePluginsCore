/**
 * Copyright 2013 BancVue, LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bancvue.gradle.tasks

import com.bancvue.gradle.test.AbstractPluginIntegrationTest
import org.gradle.testkit.functional.ExecutionResult
import org.junit.Test

import static org.junit.Assert.fail


class PrintClasspathIntegrationTest extends AbstractPluginIntegrationTest {


	@Test
	void shouldPrintCompileAndRuntimeClasspathsToConsoleForAllSourceSets() {
		projectFS.buildFile() << """
apply plugin: 'java'
dependencies {
    compile localGroovy()
}
task printClasspath(type: com.bancvue.gradle.tasks.PrintClasspath)
        """

		ExecutionResult result = run("printClasspath")

		String output = result.standardOutput
		assert output =~ /main.compileClasspath/
		assert output =~ /main.runtimeClasspath/
		assert output =~ /test.compileClasspath/
		assert output =~ /test.runtimeClasspath/
		assert output =~ /groovy-all.*jar/
	}

	@Test
	void shouldFilterSourceSetByName() {
		projectFS.buildFile() << """
apply plugin: 'java'
task printClasspath(type: com.bancvue.gradle.tasks.PrintClasspath)
        """

		ExecutionResult result = run("printClasspath", "-PsourceSetName=main")

		String output = result.standardOutput
		assert output =~ /main.compileClasspath/
		assert output =~ /main.runtimeClasspath/
		assert !(output =~ /test.compileClasspath/)
		assert !(output =~ /test.runtimeClasspath/)
	}

}