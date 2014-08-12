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

import com.bancvue.gradle.GradlePluginMixin
import com.bancvue.gradle.java.JavaExtPlugin
import com.bancvue.gradle.maven.MavenRepositoryProperties
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

@Slf4j
@Mixin(GradlePluginMixin)
class MavenPublishExtPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = 'com.bancvue.maven-publish-ext'

	private Project project
	private MavenRepositoryProperties repositoryProperties

	public void apply(Project project) {
		this.project = project
		this.repositoryProperties = new MavenRepositoryProperties(project)
		project.apply(plugin: JavaExtPlugin.PLUGIN_NAME)
		addArtifactDependencyAndPublishingSupport()
		addPublishingExtExtension()
	}

	private void addPublishingExtExtension() {
		project.extensions.create(MavenPublishExtExtension.NAME, MavenPublishExtExtension, project)
	}

	private void addArtifactDependencyAndPublishingSupport() {
		project.apply(plugin: 'maven-publish')
		createPublishLocalAlias()
		addMavenLocalAndOrganizationArtifactRepository()
		addOrganizationPublishingRepository()
	}

	private void createPublishLocalAlias() {
		Task publishToMavenLocal = project.tasks.findByName('publishToMavenLocal')
		Task publishLocal = project.tasks.create('publishLocal')
		publishLocal.dependsOn { publishToMavenLocal }
		publishLocal.group = publishToMavenLocal.group
		publishLocal.description = 'Publishes all Maven publications produced by this project to the local Maven cache.'
	}

	private void addMavenLocalAndOrganizationArtifactRepository() {
		project.repositories.mavenLocal()
		project.repositories {
			maven {
				name repositoryProperties.name
				url repositoryProperties.publicUrl
			}
		}
	}

	private boolean isSnapshotProject() {
		project.version =~ /SNAPSHOT$/
	}

	private String acquireRepositoryPublishUrl() {
		if (isSnapshotProject()) {
			repositoryProperties.snapshotUrl
		} else {
			repositoryProperties.releaseUrl
		}
	}

	private void addOrganizationPublishingRepository() {
		project.publishing {
			repositories {
				maven {
					name repositoryProperties.name
					url acquireRepositoryPublishUrl()
					if (repositoryProperties.hasCredentialsDefined()) {
						credentials {
							username repositoryProperties.username
							password repositoryProperties.password
						}
					}
				}
			}
		}
	}

}
