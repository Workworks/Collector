plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

group = "com.kfaino.collector"
version = "4.0.0"

application {
    mainClass.set("com.kfaino.collector.desktop.MainKt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.json:json:20231013")
    implementation("com.formdev:flatlaf:3.4.1")
    implementation("com.formdev:flatlaf-extras:3.4.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.kfaino.collector.desktop.MainKt"
        attributes["Implementation-Title"] = "Collecter Desktop"
        attributes["Implementation-Version"] = project.version
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    archiveBaseName.set("Collecter-Desktop")
    archiveClassifier.set("")
    archiveVersion.set("4.0.0")
}
