package com.bancvue.gradle

import com.bancvue.gradle.test.AbstractPluginIntegrationTest
import org.junit.Test

import static org.junit.Assert.fail

class IdeExtPluginIntegrationTest extends AbstractPluginIntegrationTest {

	@Test
	void idea_ShouldAddStandardSourceDirectoriesAndAdditionalTestConfigurations_IfIdePluginDeclaredBeforeTestPlugin() {
		projectDir.newFile('build.gradle') << """
            apply plugin: 'groovy'
            apply plugin: 'ide-ext'
            apply plugin: 'integration-test'
        """
		projectDir.newFolder('src', 'main', 'java')
		projectDir.newFolder('src', 'test', 'groovy')
		projectDir.newFolder('src', 'integrationTest', 'groovy')

		run('idea')

		File expectedImlFile = new File(projectDir.root, "${projectDir.root.name}.iml")
		assert expectedImlFile.exists()
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, 'src/main/java', false)
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, 'src/test/groovy', true)
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, 'src/integrationTest/groovy', true)
		try {
			assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, 'src/someOtherTest/groovy', true)
			fail("Test should have failed, something is likely wrong with positive validations")
		} catch (Throwable ex) {}
	}

	private void assertIdeaModuleFileContainsExpectedSourceFolder(File expectedImlFile, String folderName, boolean isTestFolder) {
		String expectedUrl = "file://\$MODULE_DIR\$/${folderName}"
		def module = new XmlParser().parseText(expectedImlFile.text)

		List result = module.component.content.sourceFolder.findAll {
			it.@url == expectedUrl
		}

		if (!result) {
			fail("Expected sourceFolder url=${expectedUrl} not found in iml content=${expectedImlFile.text}")
		}
		assert result.size() == 1
		assert Boolean.parseBoolean(result[0].@isTestSource) == isTestFolder
	}

	@Test
	void eclipse_ShouldAddStandardSourceDirectoriesAndAdditionalTestConfigurations_IfIdePluginDeclaredAfterTestPlugin() {
		projectDir.newFile('build.gradle') << """
            apply plugin: 'groovy'
            apply plugin: 'component-test'
            apply plugin: 'ide-ext'
        """
		projectDir.newFolder('src', 'main', 'java')
		projectDir.newFolder('src', 'test', 'groovy')
		projectDir.newFolder('src', 'componentTest', 'groovy')

		run('eclipse')

		File expectedClasspathFile = new File(projectDir.root, ".classpath")
		assert expectedClasspathFile.exists()
		assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, 'src/main/java')
		assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, 'src/test/groovy')
		assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, 'src/componentTest/groovy')
		try {
			assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, 'src/someOtherTest/groovy')
			fail("Test should have failed, something is likely wrong with positive validations")
		} catch (Throwable ex) {}
	}

	private void assertEclipseModuleFileContainsExpectedSourceFolder(File expectedClasspathFile, String folderName) {
		def classpath = new XmlParser().parseText(expectedClasspathFile.text)
		List result = classpath.classpathentry.findAll {
			it.@kind == 'src' && it.@path == folderName
		}

		if (!result) {
			fail("Expected classpathentry path=${folderName} not found in .classpath content=${expectedClasspathFile.text}")
		}
	}

}