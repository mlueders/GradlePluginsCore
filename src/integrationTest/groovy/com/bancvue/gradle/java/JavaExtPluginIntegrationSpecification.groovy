/*
 * Copyright 2014 BancVue, LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bancvue.gradle.java

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification
import org.gradle.testkit.functional.ExecutionResult

class JavaExtPluginIntegrationSpecification extends AbstractPluginIntegrationSpecification {

	def "should install DefaultSourceSetExtContainer"() {
		given:
		buildFile << """
apply plugin: 'com.bancvue.java-ext'
apply plugin: 'com.bancvue.component-test'

sourceSets_ext {
	other {
		extendsFrom main
		appendTo test, componentTest

		java {
			srcDir "src/other/java"
		}
	}
}

repositories {
	mavenCentral()
}

dependencies {
	compile "com.google.guava:guava:17.0"
	testCompile "junit:junit:4.11"
}

        """
		emptyClassFile("src/main/java/Main.java")
		emptyClassFile("src/other/java/Other.java", """
public Main createMain() {
	// verify dependency from parent source set's configuration is accessible
    com.google.common.collect.ImmutableSet.of("one", "two");
    // verify class from parent source set is accessible
	return new Main();
}
""")
		// verify the extended source set has been integrated into the configured testSourceSets
		["src/test/java/SomeTest.java", "src/componentTest/java/SomeComponentTest.java"].each { String testClassFilePath ->
			emptyClassFile(testClassFilePath, """
@org.junit.Test
public void testOther() {
	Main main = new Other().createMain();
}
""")
		}

		when:
		ExecutionResult result = run("check", "--info")

		then:
		// verify expected tasks executed and were not UP-TO-DATE
		result.standardOutput =~ /(?m)^:compileOtherJava$/
		result.standardOutput =~ /(?m)^:test$/
		result.standardOutput =~ /(?m)^:componentTest$/
	}
}
