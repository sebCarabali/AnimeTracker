plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.animetracker"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    // Connect/read timeouts on the RestClient used to call AniList (AniListViewerClient).
    implementation("org.springframework.boot:spring-boot-http-client")
    // Persistencia (Story 1.2): AppUser + WhitelistedUser vía JPA, esquema
    // versionado con Flyway, contra PostgreSQL real (Testcontainers en tests).
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Spring Boot 4 modularizó el autoconfigure de Flyway en su propio starter
    // (spring-boot-flyway ya no viene incluido por tener flyway-core en el
    // classpath a secas, a diferencia de Boot 3.x); sin este starter,
    // FlywayAutoConfiguration ni siquiera se evalúa y las migraciones nunca
    // corren, dejando a Hibernate validate() sin tablas.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Testcontainers 2.x renombró estos módulos (antes junit-jupiter/postgresql
    // a secas); las versiones siguen gestionadas por el testcontainers-bom que
    // importa spring-boot-dependencies.
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
