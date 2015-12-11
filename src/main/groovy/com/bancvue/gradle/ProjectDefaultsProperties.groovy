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

import org.gradle.api.JavaVersion
import org.gradle.api.Project


class ProjectDefaultsProperties extends DefaultProjectPropertyContainer {

	private static final String NAME = 'default'

	private static final class Props {
		/**
		 * Sets the project's sourceCompatibility and targetCompatibility
		 */
		String javaVersion = JavaVersion.VERSION_1_8.majorVersion

		/**
		 * Default options.encoding for all JavaCompile tasks
		 */
		String compilerEncoding = 'UTF-8'

		String minHeapSize = '64m'
		String maxHeapSize = '256m'
		String maxPermSize = '64m'

		String minTestHeapSize = '64m'
		String maxTestHeapSize = '512m'
		String maxTestPermSize = '64m'
	}

	@Delegate
	private Props props = new Props()

	ProjectDefaultsProperties(Project project) {
		super(project, NAME)
	}

}
