plugins {
    `kotlin-dsl`
}

/** 모듈이 공유하는 설정을 플러그인으로 묶는다. 버전은 루트의 libs.versions.toml 하나만 본다. */
dependencies {
    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.kotlin.plugin.serialization))
    implementation(plugin(libs.plugins.ktlint))
}

fun plugin(dependency: Provider<PluginDependency>): Provider<String> {
    return dependency.map { plugin -> "${plugin.pluginId}:${plugin.pluginId}.gradle.plugin:${plugin.version}" }
}
