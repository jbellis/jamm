
import org.jetbrains.gradle.ext.*
import org.jetbrains.gradle.ext.Application
import java.time.Duration

// Pull the version from build.xml
val antFileBaseVersionRegex = ".*<property name=\"version\" value=\"([0-9][0-9A-Za-z-.]*)\"/>.*".toRegex()
project.version = providers.fileContents(layout.projectDirectory.file("build.xml"))
        .asText.forUseAtConfigurationTime().get()
        .lines()
        .stream()
        .filter { s -> antFileBaseVersionRegex.matches(s) }
        .map { s -> antFileBaseVersionRegex.replace(s) { mr -> mr.groupValues[1] } }
        .findFirst()
        .get()

plugins {
    `java-library`
    `maven-publish`
    signing
    eclipse
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "0.9"
    // Plugin to allow re-running tests (as Gradle caches test runs) using `-PtestRerun`
    // or to repeat test executions using `-PtestRepetitions=N`.
    id("org.caffinitas.gradle.testrerun") version "0.1"
    // Plugin that allows to skip the jmh-shadow-jar creation and "just" creates an
    // executable `build/microbench` to run the microbenchmark.
    id("org.caffinitas.gradle.microbench") version "0.1.7"
    id("de.marcphilipp.nexus-publish") version "0.4.0"
}

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.outputDir = buildDir.resolve("classes/main")
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("test")
        resources.srcDirs("test-resources")
    }
    named("microbench") {
        java.srcDir("microbench")
        resources.srcDirs("microbench-resources")
    }
}

dependencies {
    testImplementation("junit:junit:4.13")
}

val jar = tasks.named<Jar>("jar")

tasks.named<Test>("test") {
    dependsOn(tasks.named("jar"))
    useJUnit {
    }
    maxParallelForks = 8
    jvmArgumentProviders += CommandLineArgumentProvider {
        mutableListOf("-javaagent:${jar.get().archiveFile.get().asFile}")
    }
}

java {
    @Suppress("UnstableApiUsage")
    withJavadocJar()
    @Suppress("UnstableApiUsage")
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.named<Jar>("sourcesJar") {
    group = "build"
    description = "Creates the sources-jar"

    archiveClassifier.set("sources")
    from({ sourceSets.main.get().allSource })
}
val javadoc = tasks.named<Javadoc>("javadoc") {
    isFailOnError = false
    title = "jamm ${project.version}"

    options {
        showFromPackage()
        encoding = "UTF-8"
        windowTitle = "jamm ${project.version}"
        header = "jamm ${project.version}"
    }
}
tasks.named<Jar>("javadocJar") {
    group = "build"
    description = "Creates the javadoc-jar"

    archiveClassifier.set("javadoc")
    from(javadoc)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                withXml {
                    val elParent = asNode().appendNode("parent")
                    elParent.appendNode("groupId", "org.sonatype.oss")
                    elParent.appendNode("artifactId", "oss-parent")
                    elParent.appendNode("version", "5")
                }

                name.set("Java Agent for Memory Measurements")
                description.set("Jamm provides MemoryMeter, a java agent to measure actual object memory use including JVM overhead.")
                url.set("https://github.com/jbellis/jamm/")
                scm {
                    connection.set("scm:git:git://github.com/jbellis/jamm.git")
                    developerConnection.set("scm:git:git@github.com:jbellis/jamm.git")
                    url.set("http://github.com/jbellis/jamm/tree/master/")
                }
                issueManagement {
                    system.set("github")
                    url.set("http://github.com/jbellis/jamm/issues")
                }
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }
                developers {

                    developer {
                        id.set("stephenc")
                        name.set("Stephen Connolly")
                        roles.set(listOf("publisher"))
                    }
                    developer {
                        id.set("jbellis")
                        name.set("Jonathan Ellis")
                        roles.set(listOf("architect"))
                    }
                    developer {
                        id.set("belliottsmith")
                        name.set("Benedict Elliott Smith")
                        roles.set(listOf("developer"))
                    }
                    developer {
                        id.set("mebigfatguy")
                        name.set("Dave Brosius")
                        roles.set(listOf("developer"))
                    }
                    developer {
                        id.set("mshuler")
                        name.set("Michael Shuler")
                        roles.set(listOf("developer"))
                    }
                    developer {
                        id.set("snazy")
                        name.set("Robert Stupp")
                        roles.set(listOf("developer"))
                    }
                }
            }
        }
    }
}

microbench {
    jmhVersion.set("1.25.2")
    jvmOptions.set(listOf("-javaagent:${jar.get().archiveFile.get().asFile}"))
}

/**
 * Publishing to oss.sonatype.org:
 *
 * In your ~/.gradle/gradle.properties :
 *
 * {@code
 * # See https://github.com/marcphilipp/nexus-publish-plugin#full-example
 * sonatypeUsername=<YOUR-SONATYPE-USERID>
 * sonatypePassword=<YOUR-SONATYPE-PASSWORD>
 *
 * # See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent
 * signing.gnupg.executable=gpg
 * signing.gnupg.keyName=<YOUR-KEY-ID>
 *
 *
 * Then you can sign the artifacts during publish:
 *      ./gradlew publishToSonatype -PwithSigning
 * or just locally:
 *      ./gradlew publishToMavenLocal -PwithSigning
 */

if (project.hasProperty("withSigning")) {
    signing {
        useGpgCmd()
    }
    tasks.withType<Sign> {
        onlyIf { project.hasProperty("signing.gnupg.keyName") }
    }
}

nexusPublishing {
    clientTimeout.set(Duration.ofMinutes(3))
    repositories {
        sonatype()
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

/**
 * Common settings for all jar tasks.
 */
tasks.withType<Jar>().configureEach {
    @Suppress("UnstableApiUsage")
    manifest {
        attributes["Agent-Class"] = "org.github.jamm.MemoryMeter"
        attributes["Premain-Class"] = "org.github.jamm.MemoryMeter"
        attributes["Implementation-Title"] = "jamm"
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Vendor"] = "Contributors to jamm"
        attributes["Implementation-URL"] = "https://github.com/jbellis/jamm/"
        if (project.hasProperty("release")) {
            attributes["Build-Jdk"] = "${System.getProperty("java.runtime.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vendor.version")} ${System.getProperty("java.version.date")})"
            attributes["X-Build-Timestamp"] = System.currentTimeMillis()
            attributes["X-Build-OS"] = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
        }
    }
}

// Generate a "friendly project name" for the IDE
val ideName = "jamm ${rootProject.version.toString().replace(Regex("^([0-9.]+).*"), "$1")}"
idea {
    module {
        name = ideName
        isDownloadSources = true // this is the default BTW
        inheritOutputDirs = true
    }

    project {
        withGroovyBuilder {
            "settings" {
                val encodings: EncodingConfiguration = getProperty("encodings") as EncodingConfiguration
                val delegateActions: ActionDelegationConfig = getProperty("delegateActions") as ActionDelegationConfig
                val runConfigurations: RunConfigurationContainer = getProperty("runConfigurations") as RunConfigurationContainer

                runConfigurations.defaults(Application::class.java) {
                    moduleName = "jamm.main"
                }

                delegateActions.testRunner = ActionDelegationConfig.TestRunner.CHOOSE_PER_TEST

                encodings.encoding = "UTF-8"
                encodings.properties.encoding = "UTF-8"
            }
        }
    }
}
// There's no proper way to set the name of the IDEA project (when "just importing" or syncing the Gradle project)
val ideaDir = projectDir.resolve(".idea")
if (ideaDir.isDirectory)
    ideaDir.resolve(".name").writeText(ideName)

eclipse {
    project {
        name = ideName
    }
}
