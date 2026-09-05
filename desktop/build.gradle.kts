plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

group = "com.kfaino.collector"
version = "4.3.10"

application {
    mainClass.set("com.kfaino.collector.desktop.MainKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(kotlin("stdlib"))
    implementation("org.json:json:20231013")
    implementation("com.formdev:flatlaf:3.4.1")
    implementation("com.formdev:flatlaf-extras:3.4.1")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
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
    archiveVersion.set(project.version.toString())
}

tasks.register<JavaExec>("familyInteropFixture") {
    group = "verification"
    description = "启动隔离的桌面家庭服务，供 Android 设备联调。"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.kfaino.collector.desktop.server.FamilyInteropFixture")
}
