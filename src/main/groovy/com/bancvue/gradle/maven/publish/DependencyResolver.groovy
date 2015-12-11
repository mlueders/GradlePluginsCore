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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication

class DependencyResolver {
	private Project project

	DependencyResolver(Project project) {
		this.project = project
	}

	List<Exclusion> getDependencyExclusions(Dependency dependency) {
		List<ExcludeRule> excludeRules = getDependencyExcludeRules(dependency)

		excludeRules.findAll { ExcludeRule excludeRule ->
			excludeRule.group || excludeRule.module
		}.collect { ExcludeRule excludeRule ->
			new Exclusion(excludeRule)
		}
	}

	List<ExcludeRule> getDependencyExcludeRules(Dependency dependency) {
		Set<ExcludeRule> excludeRules = []

		if (dependency instanceof ModuleDependency) {
			excludeRules.addAll(((ModuleDependency) dependency).excludeRules)
			excludeRules.addAll(getConfigurationExcludeRules(dependency))
		}
		excludeRules as List
	}

	private Set<ExcludeRule> getConfigurationExcludeRules(Dependency dependency) {
		Configuration configuration = getConfigurationWithAssociatedDependency(dependency)
		configuration ? configuration.excludeRules : []
	}

	private Configuration getConfigurationWithAssociatedDependency(Dependency dependency) {
		project.configurations.find { Configuration config ->
			config.dependencies.find {
				dependency.is(it)
			}
		}
	}

	Set<Dependency> getRuntimeDependencies(ExtendedPublication publication) {
		Set runtimeDependencies = []
		DependencySet allDependencies = publication.runtimeConfiguration.allDependencies

		allDependencies.each { Dependency aDependency ->
			if (aDependency instanceof ProjectDependency) {
				runtimeDependencies.addAll(getConfigurationExternalDependencies(aDependency as ProjectDependency))
			} else if (aDependency.group && aDependency.name && aDependency.version) {
				runtimeDependencies.add(aDependency)
			}
		}
		runtimeDependencies
	}

	private List<Dependency> getConfigurationExternalDependencies(ProjectDependency dependency) {
		Configuration configuration = getAssociatedConfiguration(dependency)
		ProjectConfigurationExternalDependencyResolver configurationDependencyResolver =
				new ProjectConfigurationExternalDependencyResolver(dependency.dependencyProject, configuration)
		configurationDependencyResolver.getConfigurationExternalDependencies()
	}

	private Configuration getAssociatedConfiguration(ProjectDependency projectDependency) {
		String configurationName = projectDependency.configuration
		if (configurationName == "default") {
			configurationName = "runtime"
		}
		projectDependency.dependencyProject.getConfigurations().getByName(configurationName)
	}


	private static final class ProjectConfigurationExternalDependencyResolver {

		private Project project
		private Configuration configuration

		ProjectConfigurationExternalDependencyResolver(Project project, Configuration configuration) {
			this.project = project
			this.configuration = configuration
		}

		List<Dependency> getConfigurationExternalDependencies() {
			List<MavenPublication> publications = getAssociatedMavenPublications()

			publications.collect { MavenPublication publication ->
				new DefaultExternalModuleDependency(publication.groupId, publication.artifactId, publication.version)
			}
		}

		private List<MavenPublication> getAssociatedMavenPublications() {
			List<MavenPublication> publications = []
			configuration.artifacts.each { PublishArtifact artifact ->
				MavenPublication matchingPublication = getMavenPublicationWithMatchingArtifact(artifact)
				if (matchingPublication) {
					publications.add(matchingPublication)
				}
			}
			publications
		}

		private MavenPublication getMavenPublicationWithMatchingArtifact(PublishArtifact artifact) {
			project.publishing.publications.findAll {
				it instanceof MavenPublication
			}.find { MavenPublication publication ->
				MavenArtifact mavenArtifact = getPrimaryPublicationArtifact(publication)
				mavenArtifact?.file == artifact.file
			}
		}

		private MavenArtifact getPrimaryPublicationArtifact(MavenPublication publication) {
			publication.artifacts.find { MavenArtifact artifact ->
				artifact.classifier == null
			}
		}

	}

}

