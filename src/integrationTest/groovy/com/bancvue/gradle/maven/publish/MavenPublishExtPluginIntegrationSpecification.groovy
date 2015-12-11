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
package com.bancvue.gradle.maven.publish

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification
import com.bancvue.gradle.test.PomFile
import com.bancvue.gradle.test.TestFile
import com.bancvue.zip.ZipArchive

class MavenPublishExtPluginIntegrationSpecification extends AbstractPluginIntegrationSpecification {

	private TestFile mavenRepo

	void setup() {
		mavenRepo = mkdir("build/maven-repo")
	}

	private void setupLocalMavenRepoAndApplyPlugin() {
		buildFile << """
ext.repositoryUsername=''
ext.repositoryPassword=''
ext.repositoryReleaseUrl='${mavenRepo.toURI()}'
ext.artifactId='artifact'

apply plugin: 'com.bancvue.maven-publish-ext'
apply plugin: 'com.bancvue.project-defaults' // set jar baseName to artifactId

group = 'group'
version = '1.0'
"""
	}

	private String getArchiveName(String artifactId, String classifier = null) {
		"${artifactId}-1.0" + (classifier ? "-${classifier}" : "")
	}

	private TestFile getBuildArtifact(String artifactId, String classifier = null) {
		String jarName = getArchiveName(artifactId, classifier)
		file("build/libs/${jarName}.jar")
	}

	private TestFile getUploadedArtifact(String artifactId, String classifier = null) {
		String jarName = getArchiveName(artifactId, classifier)
		mavenRepo.file("group/${artifactId}/1.0/${jarName}.jar")
	}

	private PomFile getPomFile(String artifactId) {
		new PomFile(mavenRepo.file("group/${artifactId}/1.0/${artifactId}-1.0.pom"))
	}

	private ZipArchive assertArchiveBuiltAndUploadedToMavenRepo(String artifactId, String classifier = null) {
		assert getBuildArtifact(artifactId, classifier).exists()
		assert getUploadedArtifact(artifactId, classifier).exists()
		new ZipArchive(getBuildArtifact(artifactId, classifier))
	}

	def "should by default publish main artifact and sources"() {
		given:
		emptyClassFile("src/main/java/Class.java")
		setupLocalMavenRepoAndApplyPlugin()

		when:
		run("publish")

		then:
		ZipArchive archive = assertArchiveBuiltAndUploadedToMavenRepo("artifact")
		archive.getEntry("Class.class")
		ZipArchive sourcesArchive = assertArchiveBuiltAndUploadedToMavenRepo("artifact", "sources")
		sourcesArchive.getEntry("Class.java")
	}

	def "should skip publication if enabled is false"() {
		given:
		emptyClassFile("src/other/java/MainClass.java")
		buildFile << """
apply plugin: 'com.bancvue.maven-publish-ext'

publishing_ext {
	publication("main") {
		enabled false
	}
}
"""

		when:
		run("publish")

		then:
		!getBuildArtifact("artifact").exists()
		!getUploadedArtifact("artifact").exists()
	}

	def "should publish both main and custom configuration if custom configuration manually configured"() {
		given:
		emptyClassFile("src/main/java/MainClass.java")
		emptyClassFile("src/custom/java/CustomClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
configurations {
	custom
}

sourceSets {
	custom {
		java {
			srcDir 'src/custom/java'
		}
	}
}

publishing_ext {
	publication("custom")
}
"""

		when:
		run("publish")

		then:
		assertArchiveBuiltAndUploadedToMavenRepo("artifact")
		assertArchiveBuiltAndUploadedToMavenRepo("artifact", "sources")
		assertArchiveBuiltAndUploadedToMavenRepo("artifact-custom")
		assertArchiveBuiltAndUploadedToMavenRepo("artifact-custom", "sources")
	}

	def "should respect artifactId defined in extended publication block"() {
		given:
		emptyClassFile("src/custom/java/CustomClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
configurations {
	custom
}

sourceSets {
	custom {
		java {
			srcDir 'src/custom/java'
		}
	}
}

publishing_ext {
	publication("custom") {
		artifactId = "custom-override"
	}
}
"""

		when:
		run("publish")

		then:
		assertArchiveBuiltAndUploadedToMavenRepo("custom-override")
		assertArchiveBuiltAndUploadedToMavenRepo("custom-override", "sources")
	}

	def "should use custom source set and configuration if configured"() {
		given:
		emptyClassFile("src/other/java/MainClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
configurations {
	doesNotMatchConvention
}

sourceSets {
	doesNotMatchConvention {
		java {
			srcDir 'src/other/java'
		}
	}
}

publishing_ext {
	publication("other") {
		sourceSet sourceSets.doesNotMatchConvention
		runtimeConfiguration configurations.doesNotMatchConvention
	}
}
"""

		when:
		run("publish")

		then:
		assertArchiveBuiltAndUploadedToMavenRepo("artifact-other")
		assertArchiveBuiltAndUploadedToMavenRepo("artifact-other", "sources")
	}

	def "should use custom archive tasks if configured"() {
		given:
		emptyClassFile("src/main/java/MainClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
task outZip(type: Zip) {
	from sourceSets.main.output
}

task srcZip(type: Zip) {
    classifier = 'sources'
	from sourceSets.main.allSource
}

publishing_ext {
	publication("main") {
		archiveTask outZip
		sourcesArchiveTask srcZip
	}
}
"""

		when:
		run("publish")

		then:
		String archiveName = getArchiveName("artifact")
		mavenRepo.file("group/artifact/1.0/${archiveName}.zip").exists()
		mavenRepo.file("group/artifact/1.0/${archiveName}-sources.zip").exists()
	}

	def "should not publish sources if publish sources set to false"() {
		given:
		emptyClassFile("src/main/java/MainClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
task outZip(type: Zip) {
	from sourceSets.main.output
}

publishing_ext {
	publication("main") {
		archiveTask outZip
		publishSources false
	}
}
"""

		when:
		run("publish")

		then:
		String archiveName = getArchiveName("artifact")
		mavenRepo.file("group/artifact/1.0/${archiveName}.zip").exists()
		File sourcesArchive = mavenRepo.file("group/artifact/1.0/").listFiles().find { File file ->
			file.name =~ /sources/
		}
		!sourcesArchive
	}

	def "should apply compile and runtime dependencies to main pom"() {
		given:
		emptyClassFile("src/main/java/MainClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
repositories {
	mavenCentral()
}

dependencies {
	compile "org.slf4j:log4j-over-slf4j:1.7.5"
	runtime "ch.qos.logback:logback:0.5"
}
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact")
		pomFile.exists()
		pomFile.assertDependency("logback")
		pomFile.assertDependency("log4j-over-slf4j")
	}

	def "should exclude dependencies from pom which have been excluded in gradle build - fix for http://issues.gradle.org//browse/GRADLE-2945"() {
		given:
		emptyClassFile("src/main/java/MainClass.java")
		emptyClassFile("src/mainTest/java/MainTestClass.java")
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
apply plugin: 'com.bancvue.test-ext'

repositories {
	mavenCentral()
}

dependencies {
    compile ('org.codehaus.groovy.modules.http-builder:http-builder:0.6') {
        exclude module: 'commons-lang'
    }
	mainTestCompile('org.spockframework:spock-core:0.7-groovy-1.8') {
		exclude group: 'org.codehaus.groovy'
		exclude group: 'org.hamcrest'
	}
}

configurations.mainTestCompile { exclude module: 'commons-logging' }
configurations.all { exclude group: 'xml-resolver', module: 'xml-resolver' }

publishing_ext {
	publication('mainTest')
}
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact")
		pomFile.assertExclusion("http-builder", "*", "commons-lang")
		pomFile.assertExclusion("http-builder", "xml-resolver", "xml-resolver")

		and:
		PomFile testPomFile = getPomFile("artifact-test")
		testPomFile.assertExclusion("spock-core", "org.codehaus.groovy", "*")
		testPomFile.assertExclusion("spock-core", "org.hamcrest", "*")
		testPomFile.assertExclusion("spock-core", "*", "commons-logging")
		testPomFile.assertExclusion("spock-core", "xml-resolver", "xml-resolver")
	}

	def "should publish dependency pom without project archives if no source set is defined"() {
		given:
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
repositories {
	mavenCentral()
}

configurations {
	clientCompile
	clientRuntime.extendsFrom(clientCompile)
}

dependencies {
	clientCompile "org.slf4j:log4j-over-slf4j:1.7.5"
}

publishing_ext {
	publication('client')
}
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact-client")
		pomFile.assertDependency("log4j-over-slf4j")
		!getUploadedArtifact("artifact-client").exists()
	}

	def "should augment pom with content of pom closure, both globally and per-publication"() {
		given:
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
publishing_ext {
	pom {
		packaging "custom"
	}

	publication('main') {
		pom {
			url "http://publication-url"
		}
	}
}
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact")
		pomFile.text =~ "<packaging>custom</packaging>"
		pomFile.text =~ "<url>http://publication-url</url>"
	}

	def "should apply config closure to maven publication, both globally and per-publication"() {
		given:
		setupLocalMavenRepoAndApplyPlugin()
		buildFile << """
publishing_ext {
	config {
		pom.withXml {
	        asNode().children().last() + {
	            resolveStrategy = Closure.DELEGATE_FIRST
	            packaging "custom"
            }
        }
	}

	publication('main') {
		config {
			pom.withXml {
				asNode().children().last() + {
					resolveStrategy = Closure.DELEGATE_FIRST
					url "http://publication-url"
				}
			}
		}
	}
}
"""

		when:
		run("publish")

		then:
		PomFile pomFile = getPomFile("artifact")
		pomFile.text =~ "<packaging>custom</packaging>"
		pomFile.text =~ "<url>http://publication-url</url>"
	}

}
