plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    kotlin("plugin.allopen") version "2.3.21"

    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.22.0"
    id("org.flywaydb.flyway") version "12.5.0"
}

group = "ru.kuzdikenov"
version = "0.0.1-SNAPSHOT"
description = "semester-work-spring"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:12.5.0")
        classpath("org.postgresql:postgresql:42.7.7")
    }
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.springframework.boot:spring-boot-starter-webclient")

    implementation("io.swagger.core.v3:swagger-annotations:2.2.49")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-freemarker-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun loadDotEnv(): Map<String, String> {
    val envFile = rootProject.file(".env")

    if (!envFile.exists()) {
        return emptyMap()
    }

    return envFile.readLines()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate {
            val index = it.indexOf("=")
            it.substring(0, index) to it.substring(index + 1)
        }
}

val dotEnv = loadDotEnv()

fun envOrDotEnv(name: String): String {
    return providers.environmentVariable(name).orNull ?: dotEnv[name].orEmpty()
}

flyway {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5433/${envOrDotEnv("DB_NAME")}"
    user = envOrDotEnv("DB_USER")
    password = envOrDotEnv("DB_PASSWORD")
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    cleanDisabled = true
}

val openApiSpec = "$projectDir/src/main/resources/openapi/openapi.yaml"
val openApiGeneratedDir = layout.buildDirectory
    .dir("generated/openapi")
    .get()
    .asFile
    .absolutePath

openApiGenerate {
    inputSpec.set(openApiSpec)
    outputDir.set(openApiGeneratedDir)

    generatorName.set("kotlin-spring")
    modelPackage.set("ru.kuzdikenov.api.dto")
    apiPackage.set("ru.kuzdikenov.api")

    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "useSpringBoot3" to "true",
            "library" to "spring-boot",
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useBeanValidation" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "openApiNullable" to "false",
            "documentationProvider" to "none",
            "useResponseEntity" to "true"
        )
    )

    additionalProperties.set(
        mapOf(
            "generateApiTests" to "false",
            "generateModelTests" to "false",
            "generateApiDocumentation" to "false",
            "generateModelDocumentation" to "false"
        )
    )
}

sourceSets {
    main {
        kotlin.srcDir("$openApiGeneratedDir/src/main/kotlin")
    }
}

tasks.named("compileKotlin") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("compileTestKotlin") {
    dependsOn(tasks.named("openApiGenerate"))
}
