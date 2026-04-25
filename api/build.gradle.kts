plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "api"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "flotti.MainKt"
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin.srcDirs("src/main")
        }
        test {
            kotlin.srcDirs("src/test")
        }
    }
}

dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.dotenv.kotlin)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
