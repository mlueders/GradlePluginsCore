/**
 * Copyright 2013 BancVue, LTD
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

import com.bancvue.gradle.license.LicenseExtPlugin
import com.bancvue.gradle.license.LicenseExtProperties
import com.bancvue.gradle.license.LicenseModel
import com.bancvue.gradle.multiproject.PostEvaluationNotifier
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ConfigureUtil

@Slf4j
class MavenPublishExtExtension {

	static final String NAME = "publishing_ext"

	private Configurator publicationConfigurator

	private static final PostEvaluationNotifier POST_EVAL_NOTIFIER = new PostEvaluationNotifier(
			{ Project project ->
				MavenPublishExtExtension publishExtExtension = project.extensions.findByName(NAME)
				publishExtExtension?.attachDependenciesToMavenPublications()
			})


	MavenPublishExtExtension(Project project) {
		POST_EVAL_NOTIFIER.addProject(project)

		this.publicationConfigurator = new Configurator(project)

		Configurator configurator = publicationConfigurator
		project.publishing {
			publications {
				configurator.getPublicationsToApply().each { ExtendedPublication extendedPublication ->
					extendedPublication.deriveUnsetVariables(configurator.publishSources, configurator.publishJavadoc)

					"${extendedPublication.name}"(MavenPublication) { MavenPublication mavenPublication ->
						configurator.configureMavenPublication(extendedPublication, mavenPublication)
					}
				}
			}
		}
	}

	/**
	 * NOTE: this hack is here b/c I couldn't figure out any other way to resolve module dependencies of a
	 * multi-module build as external dependencies.
	 * For example, if one module (moduleA) depends on another (moduleB), and both modules are published, moduleB
	 * needs to be recognized as a transitive dependency of moduleA.  Specifically, the artifact published
	 * from moduleB needs to be recognized as a transitive dependency in the pom file of moduleA.
	 * For single builds, this is not a problem.  For multi-module builds, the publication of the
	 * dependant build may not yet be set up and so it's not possible to derive the maven publication
	 * from the project configuration.  To work around, dependencies are attached to the pom after
	 * all sub-modules have been evaluated.
	 *
	 * See MavenPublishExtPluginMultiModuleIntegrationSpecification for test cases
	 */
	void attachDependenciesToMavenPublications() {
		Configurator configurator = publicationConfigurator

		configurator.project.publishing.publications {
			configurator.getPublicationsToApply().each { ExtendedPublication extendedPublication ->
				"${extendedPublication.name}"(MavenPublication) { MavenPublication mavenPublication ->
					configurator.attachDependenciesToMavenPublication(extendedPublication, mavenPublication)
				}
			}
		}
	}

	void publication(String id, Closure configure = null) {
		publicationConfigurator.addPublication(id, configure)
	}

	void config(Closure closure) {
		publicationConfigurator.config = closure
	}

	void pom(Closure closure) {
		publicationConfigurator.pom = closure
	}

	void publishSources(boolean publishSources) {
		publicationConfigurator.publishSources = publishSources
	}

	void publishJavadoc(boolean publishJavadoc) {
		publicationConfigurator.publishJavadoc = publishJavadoc
	}


	private static class Configurator {

		private Project project
		private DependencyResolver dependencyResolver
		private Map<String, ExtendedPublication> publicationMap = [:]
		private Closure config
		private Closure pom
		private boolean publishSources = true
		private boolean publishJavadoc = false

		Configurator(Project project) {
			this.project = project
			this.dependencyResolver = new DependencyResolver(project)
		}

		void addPublication(String id, Closure configure = null) {
			ExtendedPublication publication = createExtendedPublication(id, configure)
			publicationMap.put(id, publication)
		}

		private ExtendedPublication createExtendedPublication(String id, Closure configure) {
			ExtendedPublication publication = new ExtendedPublication(id, project)

			if (configure != null) {
				publication.configure(configure)
			}
			publication
		}

		List<ExtendedPublication> getPublicationsToApply() {
			if (!publicationMap.containsKey("main")) {
				addPublication("main")
			}

			publicationMap.values().findAll { ExtendedPublication publication ->
				publication.enabled
			}
		}

		private void configureMavenPublication(ExtendedPublication extendedPublication, MavenPublication mavenPublication) {
			if (extendedPublication.artifactId != null) {
				mavenPublication.artifactId = extendedPublication.artifactId
			}

			attachArtifactsToMavenPublication(extendedPublication, mavenPublication)
			addArtifactToRuntimeConfigurationIfNotAlreadyAdded(extendedPublication)
			addBasicDescriptionToMavenPOM(extendedPublication, mavenPublication)
			attachLicenseToMavenPOMIfLicenseExtPluginApplied(mavenPublication)
			applyConfigurationClosuresToMavenPublication(extendedPublication, mavenPublication)
		}

		private void attachArtifactsToMavenPublication(ExtendedPublication extendedPublication, MavenPublication mavenPublication) {
			if (extendedPublication.archiveTask) {
				mavenPublication.artifact(extendedPublication.archiveTask)
			}

			if (extendedPublication.sourcesArchiveTask && extendedPublication.publishSources) {
				mavenPublication.artifact(extendedPublication.sourcesArchiveTask)
			}

			if (extendedPublication.javadocArchiveTask && extendedPublication.publishJavadoc) {
				mavenPublication.artifact(extendedPublication.javadocArchiveTask)
			}
		}

		private void addArtifactToRuntimeConfigurationIfNotAlreadyAdded(ExtendedPublication extendedPublication) {
			if (extendedPublication.archiveTask && !extendedPublication.isArchiveAttachedToRuntimeConfiguration()) {
				project.artifacts.add(extendedPublication.runtimeConfiguration.name, extendedPublication.archiveTask)
			}
		}

		private void addBasicDescriptionToMavenPOM(ExtendedPublication extendedPublication, MavenPublication mavenPublication) {
			mavenPublication.pom.withXml {
				asNode().children().last() + {
					name extendedPublication.artifactId
					// TODO: add description
					// description project.description
					// TODO: add project url
					// url projectUrl
				}
			}
		}

		private void attachLicenseToMavenPOMIfLicenseExtPluginApplied(MavenPublication mavenPublication) {
			if (project.getPlugins().findPlugin(LicenseExtPlugin)) {
				LicenseExtProperties licenseProperties = new LicenseExtProperties(project)
				LicenseModel licenseModel = licenseProperties.getLicenseModel()

				if (licenseModel != null) {
					attachLicenseModelToMavenPOM(mavenPublication, licenseModel)
				} else {
					log.warn("license-ext plugin applied but no license model found, bypassing augmentation of maven POM with license info")
				}
			} else {
				log.info("license-ext plugin not applied, bypassing augmentation of maven POM with license info")
			}
		}

		private void attachLicenseModelToMavenPOM(MavenPublication publication, LicenseModel licenseModel) {
			publication.pom.withXml {
				asNode().children().last() + {
					licenses {
						license {
							name licenseModel.name
							url licenseModel.url
							distribution licenseModel.distribution
						}
					}
				}
			}
		}

		private void applyConfigurationClosuresToMavenPublication(ExtendedPublication extendedPublication, MavenPublication mavenPublication) {
			applyConfigurePomClosureToMavenPublication(mavenPublication, pom)
			applyConfigurePomClosureToMavenPublication(mavenPublication, extendedPublication.pom)
			applyConfigureClosureToMavenPublication(mavenPublication, config)
			applyConfigureClosureToMavenPublication(mavenPublication, extendedPublication.config)
		}

		private void applyConfigureClosureToMavenPublication(MavenPublication mavenPublication, Closure config) {
			if (config != null) {
				ConfigureUtil.configure(config, mavenPublication)
			}
		}

		private void applyConfigurePomClosureToMavenPublication(MavenPublication mavenPublication, Closure configurePom) {
			if (configurePom != null) {
				configurePom.resolveStrategy = Closure.DELEGATE_FIRST

				mavenPublication.pom.withXml {
					asNode().children().last() + configurePom
				}
			}
		}

		private void attachDependenciesToMavenPublication(ExtendedPublication extendedPublication, MavenPublication mavenPublication) {
			Set runtimeDependencies = dependencyResolver.getRuntimeDependencies(extendedPublication)

			log.info("Attaching dependencies to maven publication ${mavenPublication.name}")
			applyConfigurePomClosureToMavenPublication(mavenPublication) {
				dependencies {
					runtimeDependencies.each { Dependency aDependency ->
						List<Exclusion> exclusionList = dependencyResolver.getDependencyExclusions(aDependency)

						dependency {
							groupId aDependency.group
							artifactId aDependency.name
							version aDependency.version
							scope "runtime"

							if (exclusionList) {
								exclusions {
									exclusionList.each { Exclusion item ->
										exclusion {
											groupId item.groupId
											artifactId item.artifactId
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}
