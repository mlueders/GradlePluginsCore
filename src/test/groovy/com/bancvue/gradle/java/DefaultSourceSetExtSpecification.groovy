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

import com.bancvue.gradle.test.AbstractProjectSpecification
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

class DefaultSourceSetExtSpecification extends AbstractProjectSpecification {

	SourceSetContainer primarySourceSetContainer
	DefaultSourceSetExt sourceSetExt

	@Override
	String getProjectName() {
		return "root"
	}

	def setup() {
		project.apply(plugin: "java")

		primarySourceSetContainer = project.sourceSets
		sourceSetExt = new DefaultSourceSetExt("other", project.fileResolver, project)
		sourceSetExt.classes = new DefaultSourceSetOutput("other", project.fileResolver, project.tasks)
		sourceSetExt.output.dir(projectFS.file("build/classes/other"))
		sourceSetExt.compileClasspath += project.files("otherCompileClasspath")
		sourceSetExt.runtimeClasspath += project.files("otherRuntimeClasspath")
	}

	private List files(String... partialPaths) {
		partialPaths.collect {
			projectFS.file(it)
		}
	}

	private List configurations(String... configurationNames) {
		configurationNames.collect {
			project.configurations.getByName(it)
		}
	}

	def "extendsFrom should augment classpaths with input source set's output and extend configurations with input source set's configurations"() {
		given:
		SourceSet main = primarySourceSetContainer.getByName("main")

		when:
		sourceSetExt.extendsFrom(main)

		then:
		sourceSetExt.compileClasspath.files as List == files("build/classes/main", "build/resources/main", "otherCompileClasspath")
		sourceSetExt.runtimeClasspath.files as List == files("build/classes/other", "build/classes/main", "build/resources/main",
				"otherRuntimeClasspath")

		and:
		Configuration otherCompile = project.configurations.getByName("otherCompile")
		otherCompile.getExtendsFrom() as List == configurations("compile")
		Configuration otherRuntime = project.configurations.getByName("otherRuntime")
		otherRuntime.getExtendsFrom() as List == configurations("runtime")
	}

	def "extendsFrom should augment classpaths and configurations with inputs in the order they are provided"() {
		given:
		SourceSet main = primarySourceSetContainer.getByName("main")
		SourceSet notMain = primarySourceSetContainer.create("notMain")

		when:
		sourceSetExt.extendsFrom(notMain, main)

		then:
		sourceSetExt.compileClasspath.files as List == files("build/classes/notMain", "build/resources/notMain",
				"build/classes/main", "build/resources/main", "otherCompileClasspath")
		sourceSetExt.runtimeClasspath.files as List == files("build/classes/other", "build/classes/notMain", "build/resources/notMain",
				"build/classes/main", "build/resources/main", "otherRuntimeClasspath")

		and:
		Configuration otherCompile = project.configurations.getByName("otherCompile")
		otherCompile.getExtendsFrom() as List == configurations("notMainCompile", "compile")
		Configuration otherRuntime = project.configurations.getByName("otherRuntime")
		otherRuntime.getExtendsFrom() as List == configurations("notMainRuntime", "runtime")
	}

	def "appendTo should append classpaths to input source set's classpaths"() {
		given:
		SourceSet test = primarySourceSetContainer.findByName("test")

		when:
		sourceSetExt.appendTo(test)

		then:
		test.compileClasspath.files as List == files("build/classes/main", "build/resources/main",
				"build/classes/other", "otherCompileClasspath")
		test.runtimeClasspath.files as List == files("build/classes/test", "build/resources/test",
				"build/classes/main", "build/resources/main", "build/classes/other", "otherRuntimeClasspath")
	}

	def "appendTo should incorporate extended source sets if extendsFrom is invoked after appendTo"() {
		given:
		SourceSet notMain = primarySourceSetContainer.create("notMain")
		SourceSet componentTest = primarySourceSetContainer.create("componentTest")

		when:
		sourceSetExt.appendTo(componentTest)
		sourceSetExt.extendsFrom(notMain)

		then:
		componentTest.compileClasspath.files as List == files("build/classes/other", "build/classes/notMain", "build/resources/notMain",
				"otherCompileClasspath")
		componentTest.runtimeClasspath.files as List == files("build/classes/componentTest", "build/resources/componentTest",
				"build/classes/other", "build/classes/notMain", "build/resources/notMain", "otherRuntimeClasspath")
	}

}
