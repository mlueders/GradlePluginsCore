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
package com.bancvue.gradle.test

import org.apache.commons.io.FilenameUtils

class ProjectFileSystem extends TestFile {

	ProjectFileSystem(File baseDir) {
		super(baseDir)
	}

	void initBuildDir() {
		buildDir.mkdirs()
	}

	TestFile getBuildDir() {
		file("build")
	}

	TestFile getBuildFile() {
		file("build.gradle")
	}

	TestFile emptyClassFile(String filePath, String content = "") {
		TestFile classFile = file(filePath)
		String className = FilenameUtils.getBaseName(classFile.name)
		classFile << """public class ${className} {
${content}
}
"""
		classFile
	}
}

