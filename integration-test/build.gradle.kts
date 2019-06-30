plugins {
	id("com.github.johnrengelman.shadow") version "5.0.0"
}

dependencies {
	implementation("com.amazonaws:aws-lambda-java-core:${versions.awsLambdaCore}")
	implementation("com.google.code.gson:gson:${versions.gson}")
	implementation(project(":"))

	testImplementation("org.junit.jupiter:junit-jupiter-api:${versions.junit}")
	testImplementation("org.mockito:mockito-core:${versions.mockito}")
	testImplementation("org.assertj:assertj-core:${versions.assertj}")
	testImplementation("org.awaitility:awaitility:${versions.awaitility}")

	testImplementation("software.amazon.awssdk:cloudwatchevents:${versions.awsSdk}")
	testImplementation("software.amazon.awssdk:cloudwatchlogs:${versions.awsSdk}")
	testImplementation("software.amazon.awssdk:iam:${versions.awsSdk}")
	testImplementation("software.amazon.awssdk:lambda:${versions.awsSdk}")

	testRuntime("org.junit.jupiter:junit-jupiter-engine:${versions.junit}")
}

tasks {
	test {
		dependsOn(shadowJar)
	}

	shadowJar {
		destinationDirectory.set(processTestResources.get().destinationDir)
		archiveBaseName.set("lambda")
		archiveClassifier.set("")
		archiveVersion.set("")
	}
}
