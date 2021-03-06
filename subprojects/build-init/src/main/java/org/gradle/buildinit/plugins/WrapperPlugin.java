/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.buildinit.plugins;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.wrapper.Wrapper;

/**
 * The wrapper plugin.
 */
public class WrapperPlugin implements Plugin<Project> {
    public void apply(Project project) {
        if (project.getParent() == null) {
            project.getTasks().register("wrapper", Wrapper.class, new Action<Wrapper>() {
                @Override
                public void execute(Wrapper wrapper) {
                    wrapper.setGroup("Build Setup");
                    wrapper.setDescription("Generates Gradle wrapper files.");
                }
            });
        }
    }
}
