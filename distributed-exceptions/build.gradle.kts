import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.3.4.RELEASE"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	kotlin("jvm") version "1.4.10"
	kotlin("plugin.spring") version "1.4.10"
}

group = "io.axoniq"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}
val axonVersion: String by extra { "4.4.3" }
val mockkVersion: String by extra { "1.10.0" }

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("io.projectreactor:reactor-core")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jet brains.kotlin:kotlin-stdlib-jdk8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.axonframework:axon-spring-boot-starter:$axonVersion")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.axonframework:axon-test:$axonVersion")
	testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}
