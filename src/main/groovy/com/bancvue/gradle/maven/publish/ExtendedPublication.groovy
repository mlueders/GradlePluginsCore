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

import com.bancvue.gradle.support.CommonTaskFactory
import com.bancvue.gradle.support.TaskAndConfigurationNamer
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.ConfigureUtil

@Slf4j
class ExtendedPublication {

	String artifactId
	Project project
	AbstractArchiveTask archiveTask
	AbstractArchiveTask sourcesArchiveTask
	AbstractArchiveTask javadocArchiveTask
	SourceSet sourceSet
	Configuration runtimeConfiguration
	boolean enabled
	Boolean publishSources
	Boolean publishJavadoc
	Closure config
	Closure pom

	private TaskAndConfigurationNamer namer

	ExtendedPublication(String id, Project project) {
		this.project = project
		this.enabled = true
		this.namer = new TaskAndConfigurationNamer(id)
	}

	String getName() {
		artifactId.replaceAll(/[-](\S)/) { it[1].toUpperCase() }
	}

	void configure(Closure configure) {
		ConfigureUtil.configure(configure, this)
	}

	/**
	 * Jar tasks are resolved post evaluation since the jar task is dynamically created if it can't be found.
	 * If this was done pre-evaluation, it could conflict with an as-yet-to-be-defined jar task.
	 */
	void deriveUnsetVariables(boolean defaultPublishSources, boolean defaultPublishJavadoc) {
		if (publishSources == null) {
			publishSources = defaultPublishSources
		}
		if (publishJavadoc == null) {
			publishJavadoc = defaultPublishJavadoc
		}

		setArtifactIdIfNotSet()
		setRuntimeConfigurationIfNotSet()
		// NOTE: order is important!  sourceSet must be defined prior to the jar tasks since they may require
		// the sourceSet if the tasks need to be dynamically constructed
		setSourceSetIfNotSet()
		setJarTaskIfNotSet()
		if (publishSources) {
			setSourceJarTaskIfNotSet()
		}
		if (publishJavadoc) {
			setJavadocJarTaskIfNotSet()
		}
	}

	boolean isArchiveAttachedToRuntimeConfiguration() {
		runtimeConfiguration.artifacts.find { PublishArtifact artifact ->
			archiveTask && (artifact.file == archiveTask.archivePath)
		}
	}

	private void setArtifactIdIfNotSet() {
		if (artifactId == null) {
			artifactId = namer.getArtifactId(project)
		}
	}

	private void setJarTaskIfNotSet() {
		if (archiveTask == null) {
			archiveTask = findOrCreateJarTask()
		}
	}

	private void setSourceJarTaskIfNotSet() {
		if (sourcesArchiveTask == null) {
			sourcesArchiveTask = findOrCreateSourcesJarTask()
		}
	}

	private void setJavadocJarTaskIfNotSet() {
		if (javadocArchiveTask == null) {
			javadocArchiveTask = findOrCreateJavadocJarTask()
		}
	}

	private void setSourceSetIfNotSet() {
		if (sourceSet == null) {
			// findByName is used b/c SourceSet is optional; if not set and no archive tasks are available,
			// then no archives will be attached to the maven publication
			sourceSet = project.sourceSets.findByName(namer.sourceSetName)
		}
	}

	private void setRuntimeConfigurationIfNotSet() {
		if (runtimeConfiguration == null) {
			runtimeConfiguration = project.configurations.getByName(namer.runtimeConfigurationName)
		}
	}

	private Jar findOrCreateJarTask() {
		resolveJarTask(
				{ namer.jarTaskName },
				{ CommonTaskFactory generator -> generator.createJarTask(artifactId) }
		)
	}

	private Jar findOrCreateSourcesJarTask() {
		resolveJarTask(
				{ namer.sourcesJarTaskName },
				{ CommonTaskFactory generator -> generator.createSourcesJarTask(artifactId) }
		)
	}

	private Jar findOrCreateJavadocJarTask() {
		resolveJarTask(
				{ namer.javadocJarTaskName },
				{ CommonTaskFactory generator -> generator.createJavadocJarTask(artifactId) }
		)
	}

	private Jar resolveJarTask(Closure getJarTaskName, Closure createJarTask) {
		String jarTaskName = getJarTaskName()
		Jar jarTask = project.tasks.findByName(jarTaskName)

		if (jarTask == null) {
			jarTask = createJarTaskIfSourceSetAvailable(jarTaskName, createJarTask)
		}
		jarTask
	}

	private Jar createJarTaskIfSourceSetAvailable(String jarTaskName, Closure createJarTask) {
		Jar jarTask = null
		if (sourceSet != null) {
			jarTask = createJarTask(new CommonTaskFactory(project, sourceSet, namer))
			project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(jarTask)
		} else {
			log.warn("Failed to resolve archive task=${jarTaskName} and unable to create task due to undefined SourceSet")
		}
		jarTask
	}

	/*
	 * TODO: figure out why this is necessary...
	 * The idea here is to allow configuration in the dsl on par with how regular maven publications are configured, e.g.
	 *
	 * publication("main") {
	 *   enabled false
	 * }
	 *
	 * rather than
	 *
	 * publication("main") {
	 * 	 enabled = false
	 * }
	 *
	 * ConfigureUtil.configure explodes unless the enabled(boolean) method is defined, a regular property won't do.
	 * However, some of the gradle core classes (e.g. DefaultMavenPublication) don't seem to need them, need to
	 * investigate why.
	 */
	def methodMissing(String methodName, args) {
		Object[] argArray = args as Object[]
		MetaProperty property = hasProperty(methodName)

		if (property && (argArray.length == 1)) {
			// yes, this is wonky but it works for primitives whereas property.type.isInstance(argArray[0]) does not...
			try {
				property.setProperty(this, argArray[0])
				return
			} catch (Exception ex) {
			}
		}
		throw new MissingMethodException(methodName, this.class, args)
	}

}
