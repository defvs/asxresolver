plugins {
	kotlin("jvm") version "1.9.21"
	application
}

group = "dev.defvs"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	implementation("net.jthink", "jaudiotagger", "3.0.1")
}

kotlin {
	jvmToolchain(8)
}

application {
	mainClass.set("dev.defvs.asxresolver.MainKt")
}