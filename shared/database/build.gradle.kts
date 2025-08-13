// Add PostgreSQL driver to buildscript classpath
buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.7.2")
    }
}

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    id("org.flywaydb.flyway") version "9.22.3"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
    }
}

dependencies {
    // Spring Boot Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Spring Boot Data Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // PostgreSQL Driver
    implementation("org.postgresql:postgresql")
    
    // HikariCP Connection Pool (included in spring-boot-starter-data-jpa)
    implementation("com.zaxxer:HikariCP")
    
    // Lettuce Redis Client (included in spring-boot-starter-data-redis)
    implementation("io.lettuce:lettuce-core")
    
    // Apache Commons Pool2 for Redis connection pooling
    implementation("org.apache.commons:commons-pool2")
    
    // Flyway Migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // Jackson for JSON serialization in Redis
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Shared Domain Models
    implementation(project(":shared:domain"))
    implementation(project(":shared:utils"))
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("it.ozimov:embedded-redis:0.7.3")
}

// Flyway configuration for development
flyway {
    url = "jdbc:postgresql://localhost:5432/learning_platform"
    user = "postgres"
    password = "postgres"
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
    validateOnMigrate = true
}

tasks.test {
    useJUnitPlatform()
}