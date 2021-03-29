plugins {
    kotlin("jvm") version "1.4.31"
    id("io.gitlab.arturbosch.detekt").version("1.16.0")
}


repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("pl.setblack:nee-ctx-web-ktor:0.6.8")
    detektPlugins("pl.setblack:kure-potlin:0.4.0")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.16.0")

    //ktor server
    implementation("io.ktor:ktor-server-core:1.5.2")
    implementation("io.ktor:ktor-server-netty:1.5.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}
