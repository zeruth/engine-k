plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.1"
}

group = "io.github.nullpops"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("Server")
}

dependencies {

    implementation(files("./lib/neptune-serverscript-compiler-0.0.1-SNAPSHOT.jar"))
    implementation("io.github.nullpops:eventbus:1.0.1")
    implementation("org.apache.commons:commons-compress:1.28.0")

    // Argon2 password hashing
    implementation("de.mkammerer:argon2-jvm:2.12")

    //MySQL JDBC Driver
    implementation("com.mysql:mysql-connector-j:9.5.0")

    implementation("ch.qos.logback:logback-classic:1.5.21")

    //HikariCP connection pool
    implementation("com.zaxxer:HikariCP:7.0.2")

    //ktor
    implementation("io.ktor:ktor-server-netty:3.3.1")
    implementation("io.ktor:ktor-server-core:3.3.1")

    //kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0-Beta4")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}