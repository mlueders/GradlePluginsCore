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
package com.bancvue.gradle.custom

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification
import com.bancvue.gradle.test.TestFile
import com.bancvue.zip.ZipArchive

class CustomGradlePluginIntegrationSpecification extends AbstractPluginIntegrationSpecification {

	def "should download standard gradle distribution and bundle gradle customization script"() {
		given:
		TestFile mavenRepo = mkdir("build/maven-repo")
		TestFile zipBaseDir = mkdir("zipBase")
		zipBaseDir.file('emptyfile.txt') << ""
		file('custom.gradle') << """
ext {
    repositoryName = 'repo'
    repositoryPublicUrl = 'http://repo.domain/public'
    repositorySnapshotUrl = 'http://repo.domain/snapshots'
    repositoryReleaseUrl = 'http://repo.domain/releases'
}
"""
		buildFile << """
ext {
    customGradleBaseVersion = "1.7"
    customGradleVersion = "1.7-bv.1.0"
    customGradleGroupId = "com.bancvue"
    customGradleArtifactId = "gradle-bancvue"
    customGradleScriptResourcePath = "custom.gradle"

	repositoryUsername=''
	repositoryPassword=''
	repositoryReleaseUrl='${mavenRepo.toURI()}'
}

apply plugin: 'com.bancvue.custom-gradle'

// fake out the download and replace it with an empty zip
task createDownloadArchive(type: Zip) {
    from 'zipBase'
	includeEmptyDirs=true
	archiveName=downloadGradle.downloadFileName
	destinationDir=project.buildDir
}
downloadGradle.dependsOn { createDownloadArchive }
downloadGradle.gradleDownloadBase = "file:///\${createDownloadArchive.destinationDir}"

println "Created cutomized gradle dist at \${buildCustomGradleDistro.archivePath}"
        """

		when:
		run('publishCustomGradleDistroPublicationToRepoRepository')

		then:
		File expectedZipFile = mavenRepo.file("com/bancvue/gradle-bancvue/1.7-bv.1.0/gradle-bancvue-1.7-bv.1.0-bin.zip")
		expectedZipFile.exists()
		ZipArchive archive = new ZipArchive(expectedZipFile)
		String expectedCustomScript = archive.acquireContentForEntryWithNameLike('customized.gradle')
		expectedCustomScript =~ "repositoryPublicUrl = 'http://repo.domain/public'"
		expectedCustomScript =~ "repositorySnapshotUrl = 'http://repo.domain/snapshots'"
		expectedCustomScript =~ "repositoryReleaseUrl = 'http://repo.domain/releases'"
	}

}
