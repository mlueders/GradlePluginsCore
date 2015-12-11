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
package com.bancvue.gradle

import com.bancvue.gradle.categories.ProjectCategory
import java.text.DateFormat
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.BaseForkOptions
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

class ProjectDefaultsPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = 'com.bancvue.project-defaults'

	private Project project
	private ProjectDefaultsProperties defaultsProperties

	public void apply(Project project) {
		this.project = project
		this.defaultsProperties = new ProjectDefaultsProperties(project)
		project.apply(plugin: 'java')
		addBuildDateAndJdkToJarManifest()
		setDefaultBaseNameForJarTasks()
		project.afterEvaluate {
			setDefaultCompileMemorySettings()
			setDefaultTestMemorySettings()
			propagateJavaTmpDir()
			setCompilerEncoding()
			setJavaCompatibilityVersion()
		}
	}

	private void setDefaultCompileMemorySettings() {
		project.tasks.withType(GroovyCompile) { GroovyCompile compile ->
			setCompileMemorySettings(compile.groovyOptions.forkOptions)
		}
		project.tasks.withType(JavaCompile) { JavaCompile compile ->
			setCompileMemorySettings(compile.options.forkOptions)
		}
	}

	private void setCompileMemorySettings(BaseForkOptions forkOptions) {
		forkOptions.memoryInitialSize = forkOptions.memoryInitialSize ?: defaultsProperties.minHeapSize
		forkOptions.memoryMaximumSize = forkOptions.memoryMaximumSize ?: defaultsProperties.maxHeapSize
		if (shouldSetMaxPermSize(forkOptions)) {
			forkOptions.jvmArgs << "-XX:MaxPermSize=${defaultsProperties.maxPermSize}".toString()
		}
	}

	private boolean shouldSetMaxPermSize(def jvmArgContainer) {
		isMaxPermSizeAvailable() && isMissingJvmArg(jvmArgContainer, "-XX:MaxPermSize")
	}

	private boolean isMaxPermSizeAvailable() {
		try {
			return ((defaultsProperties.javaVersion as float) < 1.8)
		} catch (Exception ex) {
			return false
		}
	}

	private boolean isMissingJvmArg(def jvmArgContainer, String jvmArg) {
		!jvmArgContainer.jvmArgs.find { it.startsWith(jvmArg) }
	}

	private void setDefaultTestMemorySettings() {
		project.tasks.withType(Test) { Test test ->
			test.minHeapSize = test.minHeapSize ?: defaultsProperties.minTestHeapSize
			test.maxHeapSize = test.maxHeapSize ?: defaultsProperties.maxTestHeapSize
			if (shouldSetMaxPermSize(test)) {
				test.jvmArgs("-XX:MaxPermSize=${defaultsProperties.maxTestPermSize}".toString())
			}
		}
	}

	private void propagateJavaTmpDir() {
		String javaTmpDir = System.getProperty("java.io.tmpdir")
		if (javaTmpDir) {
			String jvmArgTmpDir = "-Djava.io.tmpdir=${javaTmpDir}".toString()

			project.tasks.withType(Test) { Test test ->
				if (isMissingJvmArg(test, "-Djava.io.tmpdir")) {
					test.jvmArgs(jvmArgTmpDir)
				}
			}
			project.tasks.withType(GroovyCompile) { GroovyCompile compile ->
				if (isMissingJvmArg(compile.groovyOptions.forkOptions, "-Djava.io.tmpdir")) {
					compile.groovyOptions.forkOptions.jvmArgs << jvmArgTmpDir
				}
			}
			project.tasks.withType(JavaCompile) { JavaCompile compile ->
				if (isMissingJvmArg(compile.options.forkOptions, "-Djava.io.tmpdir")) {
					compile.options.forkOptions.jvmArgs << jvmArgTmpDir
				}
			}

		}
	}

	private void setCompilerEncoding() {
		project.tasks.withType(JavaCompile) { JavaCompile compile ->
			compile.options.encoding = compile.options.encoding ?: defaultsProperties.compilerEncoding
		}

		project.tasks.withType(GroovyCompile) { GroovyCompile compile ->
			compile.groovyOptions.encoding = compile.groovyOptions.encoding ?: defaultsProperties.compilerEncoding
		}
	}

	private void setJavaCompatibilityVersion() {
		project.setProperty('sourceCompatibility', defaultsProperties.javaVersion)
		project.setProperty('targetCompatibility', defaultsProperties.javaVersion)
	}

	private void addBuildDateAndJdkToJarManifest() {
		project.tasks.withType(Jar) { Jar jar ->
			manifest {
				attributes 'Build-Date': getDateAsString()
				attributes 'Build-Jdk': System.getProperty('java.version')
			}
		}
	}

	private String getDateAsString() {
		return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date())
	}

	private void setDefaultBaseNameForJarTasks() {
		project.tasks.withType(Jar) { Jar jar ->
			jar.conventionMapping.baseName = { getDefaultBaseNameForTask(jar) }
		}
	}

	private String getDefaultBaseNameForTask(Jar jar) {
		String artifactId = ProjectCategory.getArtifactIdOrNull(project)
		artifactId ?: jar.baseName
	}

}
