/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.metalava

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import java.io.File

// MetalavaRunner stores common configuration for executing Metalava

fun Project.getMetalavaConfiguration(): Configuration {
    return configurations.findByName("metalava") ?: configurations.create("metalava") {
        val dependency = dependencies.create("com.android:metalava:1.3.0:shadow@jar")
        it.dependencies.add(dependency)
    }
}

fun Project.runMetalavaWithArgs(configuration: Configuration, args: List<String>) {
    javaexec {
        it.classpath = checkNotNull(configuration) { "Configuration not set." }
        it.main = "com.android.tools.metalava.Driver"
        it.args = listOf(
            "--no-banner",
            "--error",
            "DeprecationMismatch" // Enforce deprecation mismatch
        ) + args
    }
}

val API_LINT_ARGS: List<String> = listOf(
    "--api-lint",
    "--hide",
    listOf(
        // The list of checks that are hidden as they are not useful in androidx
        "Enum", // Enums are allowed to be use in androidx
        "CallbackInterface", // With target Java 8, we have default methods
        "HiddenSuperclass", // We allow having a hidden parent class
        "ProtectedMember", // We allow using protected members in androidx
        "ManagerLookup", // Managers in androidx are not the same as platfrom services
        "ManagerConstructor",

        // List of checks that have bugs, but should be enabled once fixed.
        "GetterSetterNames", // b/135498039
        "StaticUtils", // b/135489083
        "AllUpper", // b/135708486
        "StartWithLower", // b/135710527

        // The list of checks that are API lint warnings and are yet to be enabled
        "MinMaxConstant",
        "IntentBuilderName",
        "OnNameExpected",
        "TopLevelBuilder",
        "MissingBuild",
        "BuilderSetStyle",
        "SetterReturnsThis",
        "PackageLayering",
        "OverlappingConstants",
        "IllegalStateException",
        "ListenerLast",
        "ExecutorRegistration",
        "StreamFiles",
        "ParcelableList",
        "AbstractInner",
        "NotCloseable",
        "ArrayReturn",
        "UserHandle",
        "UserHandleName",
        "MethodNameTense",
        "UseIcu",
        "NoByteOrShort",
        "CommonArgsFirst",
        "SamShouldBeLast",
        "MissingJvmStatic"
    ).joinToString()
)

sealed class GenerateApiMode {
    class PublicApi(val apiLintBaseline: File) : GenerateApiMode()
    object RestrictedApi : GenerateApiMode()
}

fun Project.generateApi(
    bootClasspath: Collection<File>,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    outputFile: File,
    generateApiMode: GenerateApiMode
) {
    val tempOutputFile = if (generateApiMode is GenerateApiMode.RestrictedApi) {
        File(outputFile.path + ".tmp")
    } else {
        outputFile
    }

    // generate public API txt
    val args = mutableListOf("--classpath",
        (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

        "--source-path",
        sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

        "--api",
        tempOutputFile.toString(),

        "--format=v3",
        "--output-kotlin-nulls=yes"
    )

    when (generateApiMode) {
        is GenerateApiMode.PublicApi -> {
            args += API_LINT_ARGS
            if (generateApiMode.apiLintBaseline.exists()) {
                args += listOf("--baseline", generateApiMode.apiLintBaseline.toString())
            }
        }
        is GenerateApiMode.RestrictedApi -> {
            args += listOf("--show-annotation", "androidx.annotation.RestrictTo")
        }
    }

    val metalavaConfiguration = getMetalavaConfiguration()

    runMetalavaWithArgs(metalavaConfiguration, args)

    if (generateApiMode is GenerateApiMode.RestrictedApi) {
        removeRestrictToLibraryLines(tempOutputFile, outputFile)
    }
}

// until b/119617147 is done, remove lines containing "@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)"
private fun removeRestrictToLibraryLines(inputFile: File, outputFile: File) {
    val outputBuilder = StringBuilder()
    val lines = inputFile.readLines()
    var skipScopeUntil: String? = null
    for (line in lines) {
        val skip = line.contains("@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
        if (skip && line.endsWith("{")) {
            skipScopeUntil = line.commonPrefixWith("    ") + "}"
        }
        if (!skip && skipScopeUntil == null) {
            outputBuilder.append(line)
            outputBuilder.append("\n")
        }
        if (line == skipScopeUntil) {
            skipScopeUntil = null
        }
    }
    if (skipScopeUntil != null) {
        throw GradleException("Skipping until `$skipScopeUntil`, but found EOF")
    }
    outputFile.writeText(outputBuilder.toString())
}