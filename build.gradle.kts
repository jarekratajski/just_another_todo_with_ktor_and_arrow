plugins {
    kotlin("jvm") version "1.4.31"
    id("io.gitlab.arturbosch.detekt").version("1.16.0")
}


repositories {
    mavenCentral()
    jcenter()
    maven { url= uri("https://dl.bintray.com/arrow-kt/arrow-kt/") }
}

val arrow_version="0.13.0"
val ktor_version="1.5.2"

dependencies {
    implementation("pl.setblack:nee-ctx-web-ktor:0.6.8")
    detektPlugins("pl.setblack:kure-potlin:0.4.0")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.16.0")

    //ktor server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.vavr:vavr-jackson:0.10.3")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.12.0")

    //arrow
//    implementation ("io.arrow-kt:arrow-fx:$arrow_version")
    implementation ("io.arrow-kt:arrow-fx-coroutines:$arrow_version")
//    implementation ("io.arrow-kt:arrow-syntax:$arrow_version")

    testImplementation("io.ktor:ktor-server-test-host:1.5.2")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.4.3")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.4.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.apply {
    jvmTarget = "1.8"
    javaParameters = true
    allWarningsAsErrors = true
    freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}
