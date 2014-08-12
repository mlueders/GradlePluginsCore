/**
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
package com.bancvue.gradle.java;

import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.internal.tasks.TaskResolver;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.internal.reflect.Instantiator;

public class DefaultSourceSetExtContainer extends AbstractNamedDomainObjectContainer<SourceSet> implements SourceSetContainer {

	public static final String NAME = "sourceSets_ext";

	private final Project project;
	private final SourceSetContainer primarySourceSetContainer;
    private final FileResolver fileResolver;
    private final TaskResolver taskResolver;
    private final Instantiator instantiator;

    public DefaultSourceSetExtContainer(Project project, SourceSetContainer primarySourceSetContainer,
                                        FileResolver fileResolver, Instantiator classGenerator) {
        super(SourceSet.class, classGenerator, new Namer<SourceSet>() { public String determineName(SourceSet ss) { return ss.getName(); }});
	    this.project = project;
	    this.primarySourceSetContainer = primarySourceSetContainer;
        this.fileResolver = fileResolver;
        this.taskResolver = (TaskResolver) project.getTasks();
        this.instantiator = classGenerator;
    }

    @Override
    protected SourceSet doCreate(String name) {
        DefaultSourceSetExt sourceSet = instantiator.newInstance(DefaultSourceSetExt.class, name, fileResolver, project);
        sourceSet.setClasses(instantiator.newInstance(DefaultSourceSetOutput.class, sourceSet.getDisplayName(), fileResolver, taskResolver));
	    // add to the primary SourceSetContainer so they will git wired up like the other source sets (e.g. compilation
	    // tasks created, etc)
	    primarySourceSetContainer.add(sourceSet);
        return sourceSet;
    }

	/**
	 * Override findByName so source sets from the primary container may be referenced within the configuration closure
	 * of the extended source set.
	 */
	@Override
	public SourceSet findByName(String name) {
		SourceSet sourceSet = super.findByName(name);
		if (sourceSet == null) {
			sourceSet = primarySourceSetContainer.findByName(name);
		}
		return sourceSet;
	}
}
