/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;
import org.jetbrains.kotlin.descriptors.Visibilities;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.load.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import com.google.common.collect.Lists;
import com.intellij.psi.CommonClassNames;

public class EclipseJavaElementUtil {
    
    @NotNull
    static Visibility getVisibility(@NotNull IBinding member) {
        int flags = member.getModifiers();
        if (Modifier.isPublic(flags)) {
            return Visibilities.PUBLIC;
        } else if (Modifier.isPrivate(flags)) {
            return Visibilities.PRIVATE;
        } else if (Modifier.isProtected(flags)) {
            return Flags.isStatic(flags) ? JavaVisibilities.PROTECTED_STATIC_VISIBILITY : JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        
        return JavaVisibilities.PACKAGE_VISIBILITY;
    }
    
    private static List<ITypeBinding> getSuperTypes(@NotNull ITypeBinding typeBinding) {
        List<ITypeBinding> superTypes = Lists.newArrayList();
        for (ITypeBinding superInterface : typeBinding.getInterfaces()) {
            superTypes.add(superInterface);
        }
        
        ITypeBinding superClass = typeBinding.getSuperclass();
        if (superClass != null) {
            superTypes.add(superClass);
        }
        
        return superTypes;
    }
    
    static ITypeBinding[] getSuperTypesWithObject(@NotNull ITypeBinding typeBinding) {
        List<ITypeBinding> allSuperTypes = Lists.newArrayList();
        
        boolean javaLangObjectInSuperTypes = false;
        for (ITypeBinding superType : getSuperTypes(typeBinding)) {
            if (superType.getQualifiedName().equals(CommonClassNames.JAVA_LANG_OBJECT)) {
                javaLangObjectInSuperTypes = true;
            }
            allSuperTypes.add(superType);
        }
        
        if (!javaLangObjectInSuperTypes && !typeBinding.getQualifiedName().equals(CommonClassNames.JAVA_LANG_OBJECT)) {
            allSuperTypes.add(getJavaLangObjectBinding(typeBinding.getJavaElement().getJavaProject()));
        }
        
        return allSuperTypes.toArray(new ITypeBinding[allSuperTypes.size()]);
    }
    
    @NotNull
    private static ITypeBinding getJavaLangObjectBinding(@NotNull IJavaProject javaProject) {
        try {
            IType javaType = javaProject.findType(CommonClassNames.JAVA_LANG_OBJECT);
            return EclipseJavaClassFinder.createTypeBinding(javaType);
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
            throw new IllegalStateException(e);
        }
    }
    
    @NotNull
    static List<JavaValueParameter> getValueParameters(@NotNull IMethodBinding method) {
        List<JavaValueParameter> parameters = new ArrayList<JavaValueParameter>();
        ITypeBinding[] parameterTypes = method.getParameterTypes();
        
        String[] parameterNames = getParameterNames(method);
        int parameterTypesCount = parameterTypes.length;
        for (int i = 0; i < parameterTypesCount; ++i) {
            boolean isLastParameter = i == parameterTypesCount - 1;
            parameters.add(new EclipseJavaValueParameter(
                    parameterTypes[i], 
                    method.getParameterAnnotations(i),
                    parameterNames[i], 
                    isLastParameter ? method.isVarargs() : false));
        }
        
        return parameters;
    }
    
    @NotNull
    private static String[] getParameterNames(@NotNull IMethodBinding methodBinding) {
        try {
            IMethod methodElement = (IMethod) methodBinding.getJavaElement();
            String[] parameterNames;
            if (methodElement != null) {
                parameterNames = methodElement.getParameterNames();
            } else {
                int parametersCount = methodBinding.getParameterTypes().length;
                parameterNames = new String[parametersCount];
                for (int i = 0; i < parametersCount; ++i) {
                    parameterNames[i] = "arg" + i;
                }
            }
            
            return parameterNames;
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        throw new RuntimeException();
    }
    
    public static JavaAnnotation findAnnotation(@NotNull IAnnotationBinding[] annotationBindings, @NotNull FqName fqName) {
        for (IAnnotationBinding annotation : annotationBindings) {
            String annotationFQName = annotation.getAnnotationType().getQualifiedName();
            if (fqName.asString().equals(annotationFQName)) {
                return new EclipseJavaAnnotation(annotation);
            }
        }
        
        return null;
    }
    
    @Nullable
    public static ClassId computeClassId(@NotNull ITypeBinding classBinding) {
        ITypeBinding container = classBinding.getDeclaringClass();
        if (container != null) {
            ClassId parentClassId = computeClassId(container);
            return parentClassId == null ? null : parentClassId.createNestedClassId(Name.identifier(classBinding.getName()));
        }
        
        String fqName = classBinding.getQualifiedName();
        return fqName == null ? null : ClassId.topLevel(new FqName(fqName));
    }
    
    public static boolean isKotlinLightClass(@NotNull IJavaElement element) {
        IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(element.getPath());
        if (resource == null) {
            return false;
        }
        
        IContainer parent = resource.getParent();
        while (parent != null) {
            if (KotlinFileSystem.SCHEME.equals(parent.getLocationURI().getScheme())) {
                return true;
            }
            parent = parent.getParent();
        }
        
        return false;
    }
}
