@file:Suppress("UnstableApiUsage")

import nl.javadude.gradle.plugins.license.LicensePlugin
import javax.swing.JOptionPane

plugins {
	`java-library`
	signing
	`maven-publish`
	id("com.github.hierynomus.license") version "0.15.0"
}

allprojects {
	plugins.apply(LicensePlugin::class)
	plugins.apply(JavaLibraryPlugin::class)

	group = "com.perihelios.aws"
	version = "1.0.0"

	repositories {
		mavenLocal()
		mavenCentral()
	}

	license {
		header = file("${project.rootDir}/.license-template")

		strictCheck = true
		skipExistingHeaders = true
		useDefaultMappings = false

		mapping("java", "SLASHSTAR_STYLE")

		exclude("**/*.json")
		exclude("**/*.json.txt")
	}

	tasks {
		compileJava {
			sourceCompatibility = versions.java
			targetCompatibility = versions.java
		}

		test {
			useJUnitPlatform()
		}
	}
}

val awsLambdaCoreJavadoc: Configuration by configurations.creating

dependencies {
	awsLambdaCoreJavadoc("com.amazonaws:aws-lambda-java-core:${versions.awsLambdaCore}:javadoc")

	implementation("com.amazonaws:aws-lambda-java-core:${versions.awsLambdaCore}")
	implementation("com.google.code.gson:gson:${versions.gson}")

	testImplementation("org.junit.jupiter:junit-jupiter-api:${versions.junit}")
	testImplementation("org.mockito:mockito-core:${versions.mockito}")
	testImplementation("org.assertj:assertj-core:${versions.assertj}")
	testRuntime("org.junit.jupiter:junit-jupiter-engine:${versions.junit}")
}

tasks {
	clean {
		delete("docs/javadoc")
		delete("docs/aws-lambda-java-core")
	}

	val githubAwsLambdaCoreJavadoc by registering(Copy::class) {
		from(zipTree(awsLambdaCoreJavadoc.resolvedConfiguration.files.iterator().next()))
		exclude("META-INF/**")
		destinationDir = file("docs/aws-lambda-java-core/javadoc")
	}

	val githubJavadoc by registering(Copy::class) {
		from(javadoc) {
			include("**/*.html")
			filter { line ->
				when {
					line.matches(Regex("<!-- Generated by javadoc .*-->")) -> ""
					line.matches(Regex("<meta name=\"dc.created\".*")) -> ""
					else -> line
				}
			}
		}

		from(javadoc) {
			exclude("/*.zip")
			exclude("**/*.html")
		}

		destinationDir = file("docs/javadoc")
	}

	javadoc {
		dependsOn(githubAwsLambdaCoreJavadoc)

		with(options as StandardJavadocDocletOptions) {
			charSet = "UTF-8"
			docEncoding = "UTF-8"
			encoding = "UTF-8"
			source = "1.8"

			addStringOption("sourcetab", "4")

			bottom("Copyright &copy; 2019 Perihelios LLC. All rights reserved.")

			links(
					"https://docs.oracle.com/javase/${versions.java}/docs/api/",
					"https://static.javadoc.io/com.google.code.gson/gson/${versions.gson}"
			)

			linksOffline(
					"../aws-lambda-java-core/javadoc/",
					"docs/aws-lambda-java-core/javadoc/"
			)

			keyWords()

			noTimestamp()
		}
	}

	val sourcesJar by registering(Jar::class) {
		dependsOn(JavaPlugin.CLASSES_TASK_NAME)
		from(sourceSets["main"].allJava)
		archiveClassifier.set("sources")
	}

	val javadocJar by registering(Jar::class) {
		dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
		from(githubJavadoc)
		archiveClassifier.set("javadoc")
	}

	artifacts {
		add("archives", sourcesJar)
		add("archives", javadocJar)
	}

	build {
		dependsOn(githubAwsLambdaCoreJavadoc, githubJavadoc, javadocJar, sourcesJar)
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
			artifact(tasks["sourcesJar"])
			artifact(tasks["javadocJar"])

			pom {
				name.set("AWS CloudWatch Event Dispatcher for AWS Lambda")
				description.set("Easy mapping of CloudWatch events to handlers in AWS Lambda")
				url.set("https://github.com/perihelios/aws-cloudwatch-lambda-event-dispatcher")

				issueManagement {
					system.set("GitHub")
					url.set("https://github.com/perihelios/aws-cloudwatch-lambda-event-dispatcher/issues")
				}

				licenses {
					license {
						name.set("Apache License, Version 2.0")
						url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}

				organization {
					name.set("Perihelios LLC")
					url.set("http://www.perihelios.com/")
				}

				developers {
					developer {
						id.set("jc")
						name.set("Jonathan Cottrill")
						email.set("oss@perihelios.com")
					}
				}

				scm {
					connection.set("scm:git:https://github.com/perihelios/aws-cloudwatch-lambda-event-dispatcher.git")
					developerConnection.set("scm:git:ssh://git@github.com/perihelios/aws-cloudwatch-lambda-event-dispatcher.git")
					url.set("https://github.com/perihelios/aws-cloudwatch-lambda-event-dispatcher")
				}
			}
		}
	}

	repositories {
		maven {
			val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
			val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots/"

			url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
		}
	}
}

tasks.withType(PublishToMavenRepository::class).configureEach {
	doFirst {
		repository.credentials {
			username = JOptionPane.showInputDialog("Central Repository Username")
			password = JOptionPane.showInputDialog("Central Repository Password")
		}
	}
}

signing {
	useGpgCmd()
	sign(publishing.publications["mavenJava"])
	extra["signing.gnupg.keyName"] = "Perihelios LLC <pgp@perihelios.com>"
}
