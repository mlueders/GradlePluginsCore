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
package com.bancvue.gradle.maven

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification
import com.bancvue.gradle.test.TestFile

class MavenExtPluginIntegrationSpecification extends AbstractPluginIntegrationSpecification {

	private TestFile localMavenRepo

	void setup() {
		localMavenRepo = mkdir("build/maven-repo")
		buildFile << """
ext.repositoryUsername=''
ext.repositoryPassword=''
ext.repositoryReleaseUrl='${localMavenRepo.toURI()}'
ext.repositorySnapshotUrl='${localMavenRepo.toURI()}'
ext.artifactId='artifact'

group = 'group'
version = '1.0'
"""
	}

	def "should publish artifact and sources"() {
		given:
		emptyClassFile("src/main/java/Class.java")
		buildFile << """
apply plugin: 'com.bancvue.project-defaults'
apply plugin: 'com.bancvue.maven-ext'
		"""

		when:
		run("publish")

		then:
		file("build/libs/artifact-1.0.jar").exists()
		file("build/libs/artifact-1.0-sources.jar").exists()
		localMavenRepo.file("group/artifact/1.0/artifact-1.0.jar").exists()
		localMavenRepo.file("group/artifact/1.0/artifact-1.0-sources.jar").exists()
	}

	def "should supportdeployer customizations in build file"() {
		given:
		buildFile << """
apply plugin: 'com.bancvue.maven-ext'

project.uploadArchives {
	repositories.mavenDeployer {
		pom.project {
			reporting {
				outputDirectory = "build/reporting"
			}
		}
	}
}
"""

		when:
		run("publish")

		then:
		TestFile pomFile = file("build/poms/pom-default.xml")
		pomFile.text =~ /build\/reporting/
	}

}
