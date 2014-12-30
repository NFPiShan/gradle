/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.nativeplatform.toolchain.internal.gcc;

import org.gradle.internal.Transformers;
import org.gradle.internal.operations.BuildOperationProcessor;
import org.gradle.nativeplatform.toolchain.internal.CommandLineToolInvocation;
import org.gradle.nativeplatform.toolchain.internal.CommandLineToolInvocationWorker;
import org.gradle.nativeplatform.toolchain.internal.compilespec.AssembleSpec;

class Assembler extends GccCompatibleNativeCompiler<AssembleSpec> {

    Assembler(BuildOperationProcessor buildOperationProcessor, CommandLineToolInvocationWorker commandLineTool, CommandLineToolInvocation baseInvocation, String objectFileSuffix, boolean useCommandFile) {
        super(buildOperationProcessor, commandLineTool, baseInvocation, new AssemblerArgsTransformer(), Transformers.<AssembleSpec>noOpTransformer(), objectFileSuffix, useCommandFile);
    }

    private static class AssemblerArgsTransformer  extends GccCompilerArgsTransformer<AssembleSpec> {
        protected String getLanguage() {
            return "assembler";
        }
    }
}
