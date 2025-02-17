plugins {
    kotlin("jvm") version "2.1.20-Beta2"
    alias(libs.plugins.kotlin.serialization)

}

group = "work.delsart.guixu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlinx.io)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}