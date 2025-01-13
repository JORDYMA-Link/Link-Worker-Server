dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")

    // Security
    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

    // Firebase
    implementation("com.google.firebase:firebase-admin:9.1.1")

    // Web Scraping
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.seleniumhq.selenium:selenium-java:4.14.1")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// QueryDSL 설정
kotlin.sourceSets.main {
    kotlin.srcDirs("$projectDir/src/main/kotlin", "$buildDir/generated")
}