/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.resolve

import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.analyzer.AnalysisResult
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.getService

public class KotlinResolutionFacade(val javaProject: IJavaProject, val componentProvider: ComponentProvider) : ResolutionFacade {
    override val moduleDescriptor: ModuleDescriptor
        get() = throw UnsupportedOperationException()
    
    override val project: Project
        get() = KotlinEnvironment.getEnvironment(javaProject).getProject()
    
    override fun analyze(element: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        throw UnsupportedOperationException()
    }
    
    override fun analyzeFullyAndGetResult(elements: Collection<JetElement>): AnalysisResult {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T = componentProvider.getService(serviceClass)
    
    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun resolveToDescriptor(declaration: JetDeclaration): DeclarationDescriptor {
        throw UnsupportedOperationException()
    }
}