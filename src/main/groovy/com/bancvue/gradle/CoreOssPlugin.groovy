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

import com.bancvue.gradle.java.JavaExtPlugin
import com.bancvue.gradle.license.LicenseExtPlugin
import com.bancvue.gradle.support.ProjectSupportPlugin
import com.bancvue.gradle.test.ComponentTestPlugin
import com.bancvue.gradle.test.JacocoExtPlugin
import com.bancvue.gradle.test.TestExtPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class CoreOssPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = 'com.bancvue.core-oss'

	private Project project

	public void apply(Project project) {
		this.project = project
		applyJavaExtPlugin()
		applyGroovyPlugin()
		applyProjectDefaultsPlugin()
		applyLicenseExtPlugin()
		applyTestExtPlugin()
		applyComponentTestPlugin()
		applyJacocoExtPlugin()
		applyIdeExtPlugin()
		applyProjectSupportPlugin()
	}

	private void applyJavaExtPlugin() {
		project.apply(plugin: JavaExtPlugin.PLUGIN_NAME)
	}

	private void applyGroovyPlugin() {
		project.apply(plugin: 'groovy')
	}

	private void applyIdeExtPlugin() {
		project.apply(plugin: IdeExtPlugin.PLUGIN_NAME)
	}

	private void applyLicenseExtPlugin() {
		project.apply(plugin: LicenseExtPlugin.PLUGIN_NAME)
	}

	private void applyTestExtPlugin() {
		project.apply(plugin: TestExtPlugin.PLUGIN_NAME)
	}

	private void applyComponentTestPlugin() {
		project.apply(plugin: ComponentTestPlugin.PLUGIN_NAME)
	}

	private void applyJacocoExtPlugin() {
		project.apply(plugin: JacocoExtPlugin.PLUGIN_NAME)
	}

	private void applyProjectSupportPlugin() {
		project.apply(plugin: ProjectSupportPlugin.PLUGIN_NAME)
	}

	private void applyProjectDefaultsPlugin() {
		project.apply(plugin: ProjectDefaultsPlugin.PLUGIN_NAME)
	}

}
