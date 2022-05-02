package com.aldunelabs.maven.ecr;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;

public class createRepository {

	static class TestMavenAwsCreateEcrRepository extends MavenAwsCreateEcrRepository {
		public TestMavenAwsCreateEcrRepository() {
			this.region = System.getProperty("AWS_REGION", "");
			this.profile = System.getProperty("AWS_PROFILE", "");
			this.accesskey = System.getProperty("AWS_ACCESSKEY", "");
			this.secretkey = System.getProperty("AWS_SECRETKEY", "");
			this.token = System.getProperty("AWS_SESSIONTOKEN", "");
			this.encryptionType = System.getProperty("ECR_ENCRYPTION_TYPE", "");
			this.kmsKey = System.getProperty("ECR_KMS_KEY", "");
			this.mutable = System.getProperty("ECR_MUTABLE", "");
			this.lifecycleUrl = System.getProperty("ECR_LIFECYCLE_URL", "");
			this.permissionsUrl = System.getProperty("ECR_PERMISSIONS_URL", "");
			this.registryId = System.getProperty("ECR_REGISTRY_ID", "");
			this.repository = System.getProperty("ECR_REPOSITORY", "");

			this.setLog(new SystemStreamLog());
		}

	}

	@Test
	public void test() throws MojoExecutionException, MojoFailureException {
		TestMavenAwsCreateEcrRepository plugin = new TestMavenAwsCreateEcrRepository();

		plugin.execute();
	}

}
