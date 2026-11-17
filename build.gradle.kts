import net.thebugmc.gradle.sonatypepublisher.PublishingType

plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("maven-publish")
    signing
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.4"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    flatDir {
        dirs("lib")
    }
}

dependencies {
    paperweight.paperDevBundle("26.3.build.+")
    compileOnly("com.MT:Minetorio:1.0.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

tasks {
    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    javadoc {
        val standardOptions = options as StandardJavadocDocletOptions
        standardOptions.apply {
            encoding = "UTF-8"
            charSet = "UTF-8"
            memberLevel = JavadocMemberLevel.PUBLIC
            isAuthor = true
            isVersion = true
        }
    }
}

centralPortal {
    name = "MTMultiblocks"
    publishingType = PublishingType.AUTOMATIC
    pom {
        name = "MTMultiblocks"
        description = "Minetorio Multiblocks library"
        url = "https://github.com/darksoulq/MTMultiblocks"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "darksoulq"
                name = "DarksoulQ"
            }
        }
        scm {
            connection = "scm:git:git://github.com/darksoulq/MTMultiblocks.git"
            developerConnection = "scm:git:ssh://github.com/darksoulq/MTMultiblocks.git"
            url = "https://github.com/darksoulq/MTMultiblocks"
        }
    }
}

signing {
    val keyFile = rootProject.file("sonatype.asc")
    if (keyFile.exists()) {
        useInMemoryPgpKeys(
            project.providers.gradleProperty("signing.keyId").orNull,
            keyFile.readText(),
            project.providers.gradleProperty("signing.password").orNull
        )
    }
}
