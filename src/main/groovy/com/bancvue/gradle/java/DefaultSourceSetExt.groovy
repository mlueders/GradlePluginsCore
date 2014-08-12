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

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.collections.DelegatingFileCollection
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.tasks.SourceSet

@Slf4j
public class DefaultSourceSetExt extends DefaultSourceSet implements SourceSet {

	private Project project;
	private Configuration compileConfiguration;
	private Configuration runtimeConfiguration;

	public DefaultSourceSetExt(String name, FileResolver fileResolver, Project project) {
		super(name, fileResolver);
		this.project = project;

		logInfo("Creating extended ${this}")
		compileConfiguration = findOrCreateConfiguration("${name}Compile")
		runtimeConfiguration = findOrCreateConfiguration("${name}Runtime")
		setCompileClasspath(compileConfiguration)
		setRuntimeClasspath(runtimeConfiguration)
	}

	private Configuration findOrCreateConfiguration(String configurationName) {
		Configuration configuration = project.configurations.findByName(configurationName)
		configuration ? configuration : project.configurations.create(configurationName)
	}

	public SourceSet extendsFrom(SourceSet... parentSourceSets) {
		FileCollection parentSourceSetOutput = project.files()

		for (SourceSet parentSourceSet : parentSourceSets) {
			extendFromParentConfigurationIfExists(compileConfiguration, parentSourceSet, "compile")
			extendFromParentConfigurationIfExists(runtimeConfiguration, parentSourceSet, "runtime")
			parentSourceSetOutput += parentSourceSet.output
		}

		compileClasspath = parentSourceSetOutput + compileClasspath
		runtimeClasspath = output + parentSourceSetOutput + runtimeClasspath
		this
	}

	private void extendFromParentConfigurationIfExists(Configuration target, SourceSet parentSourceSet, String configurationQualifier) {
		String extendFromonfigurationName = parentSourceSet.name == "main" ? configurationQualifier :
				"${parentSourceSet.name}${configurationQualifier.capitalize()}"
		extendFromConfigurationIfExists(target, extendFromonfigurationName)
	}

	private void extendFromConfigurationIfExists(Configuration target, String extendFromConfigurationName) {
		Configuration extendsFromConfiguration = project.configurations.findByName(extendFromConfigurationName)
		if (extendsFromConfiguration) {
			target.extendsFrom(extendsFromConfiguration)
			logInfo("${target} extendsFrom ${extendsFromConfiguration}")
		}
	}

	public SourceSet appendTo(SourceSet ... testSourceSets) {
		for (SourceSet testSourceSet : testSourceSets) {
			testSourceSet.compileClasspath += output + new DelegatingFileCollection() {
				@Override
				FileCollection getDelegate() {
					return DefaultSourceSetExt.this.compileClasspath
				}
			}
			testSourceSet.runtimeClasspath += output + new DelegatingFileCollection() {
				@Override
				FileCollection getDelegate() {
					return DefaultSourceSetExt.this.runtimeClasspath
				}
			}
			logInfo("${this} appended to ${testSourceSet} classpaths")
		}
		this
	}

	private void logInfo(String message) {
		log.info("SourceSetExt: ${message}")
	}

}
