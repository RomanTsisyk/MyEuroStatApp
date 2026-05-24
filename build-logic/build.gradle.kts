plugins { `kotlin-dsl` }

dependencies {
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.plugins.kotlin.multiplatform.toDep())
    implementation(libs.plugins.kotlin.android.toDep())
    implementation(libs.plugins.kotlin.serialization.toDep())
    implementation(libs.plugins.android.application.toDep())
    implementation(libs.plugins.android.library.toDep())
    implementation(libs.plugins.compose.multiplatform.toDep())
    implementation(libs.plugins.compose.compiler.toDep())
    implementation(libs.plugins.sqldelight.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}"
}
