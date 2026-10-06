plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":core:common"))
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}
