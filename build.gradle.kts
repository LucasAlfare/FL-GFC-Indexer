plugins {
  kotlin("jvm") version "2.3.0"
  kotlin("plugin.serialization") version "2.3.0"
}

group = "com.lucasalfare"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
  testImplementation(kotlin("test"))
}

kotlin {
  jvmToolchain(25)
}

tasks.test {
  useJUnitPlatform()
}