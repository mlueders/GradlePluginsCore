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
package com.bancvue.gradle.maven.publish

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification
import com.bancvue.gradle.test.PomFile
import com.bancvue.gradle.test.TestFile

class MavenPublishExtPluginMultiModuleIntegrationSpecification extends AbstractPluginIntegrationSpecification {

	private TestFile mavenRepo

	void setup() {
		mavenRepo = mkdir("build/maven-repo")
	}

	private PomFile getPomFile(String artifactId) {
		new PomFile(mavenRepo.file("group/${artifactId}/1.0/${artifactId}-1.0.pom"))
	}

	private TestFile initMultiModuleBuildFile() {
		buildFile << """
apply plugin: 'com.bancvue.maven-publish-ext'

ext.artifactId='artifact'

repositories {
	mavenCentral()
}

allprojects {
	apply plugin: 'com.bancvue.project-defaults' // set jar baseName to artifactId

	ext.repositoryUsername=''
	ext.repositoryPassword=''
	ext.repositoryReleaseUrl='${mavenRepo.toURI()}'
	group = 'group'
	version = '1.0'
}

subprojects {
	ext.artifactId="\${project.name}-artifact"
}
"""
		buildFile
	}

	def "should include sub-module dependency as transitive dependency in parent project pom if dependency is published"() {
		given:
		emptyClassFile("src/main/java/Class.java")
		emptyClassFile("publishedDependency/src/main/java/Class.java")
		emptyClassFile("localDependency/src/main/java/Class.java")
		file("settings.gradle") << "include 'publishedDependency', 'localDependency'"
		initMultiModuleBuildFile() << """
dependencies {
	compile project(":publishedDependency")
	compile project(":localDependency")
}
"""
		file("publishedDependency/build.gradle") << """
apply plugin: 'com.bancvue.maven-publish-ext'
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact")
		pomFile.assertDependency("publishedDependency-artifact")
		pomFile.assertNoDependency("localDependency-artifact")
	}

	def "should include sub-module dependency as transitive dependency in sibling project pom if dependency is published"() {
		given:
		emptyClassFile("subModule/src/main/java/Class.java")
		emptyClassFile("subModulePublishedDependency/src/main/java/Class.java")
		emptyClassFile("subModuleLocalDependency/src/main/java/Class.java")
		file("settings.gradle") << "include 'subModule', 'subModulePublishedDependency', 'subModuleLocalDependency'"
		initMultiModuleBuildFile()
		file("subModule/build.gradle") << """
apply plugin: 'com.bancvue.maven-publish-ext'

dependencies {
	compile project(":subModulePublishedDependency")
	compile project(":subModuleLocalDependency")
}
"""
		file("subModulePublishedDependency/build.gradle") << """
apply plugin: 'com.bancvue.maven-publish-ext'
"""

		when:
		run("publish")

		then:
		PomFile subModulePomFile = getPomFile("subModule-artifact")
		subModulePomFile.assertDependency("subModulePublishedDependency-artifact")
		subModulePomFile.assertNoDependency("subModuleLocalDependency-artifact")
	}

	def "should include project dependency from multi-module build when configuration specified"() {
		given:
		emptyClassFile("src/main/java/Class.java")
		emptyClassFile("publishedDependency/src/main/java/Class.java")
		emptyClassFile("publishedDependency/src/mainTest/java/Class.java")
		file("settings.gradle") << "include 'publishedDependency'"
		initMultiModuleBuildFile() << """
dependencies {
	compile project(":publishedDependency")
	compile project(path: ":publishedDependency", configuration: "mainTestRuntime")
}
"""
		file("publishedDependency/build.gradle") << """
apply plugin: 'com.bancvue.test-ext'
apply plugin: 'com.bancvue.maven-publish-ext'

publishing_ext {
	publication("mainTest")
}
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact")
		pomFile.assertDependency("publishedDependency-artifact")
		pomFile.assertDependency("publishedDependency-artifact-test")
	}

}
