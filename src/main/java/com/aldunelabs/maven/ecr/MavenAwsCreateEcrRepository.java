package com.aldunelabs.maven.ecr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.CreateRepositoryRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.EncryptionConfiguration;
import com.amazonaws.services.ecr.model.EncryptionType;
import com.amazonaws.services.ecr.model.ImageTagMutability;
import com.amazonaws.services.ecr.model.KmsException;
import com.amazonaws.services.ecr.model.PutLifecyclePolicyRequest;
import com.amazonaws.services.ecr.model.RepositoryNotFoundException;
import com.amazonaws.services.ecr.model.SetRepositoryPolicyRequest;

@Mojo(name = "create-ecr-repository", defaultPhase = LifecyclePhase.DEPLOY)
public class MavenAwsCreateEcrRepository extends AbstractMojo {

	@Parameter(defaultValue = "eu-east-1", name = "region", required = true, property = "AWS_REGION")
	String region;

	@Parameter(required = true, name = "repository", property = "ECR_REPOSITORY")
	String repository;

	@Parameter(name = "profile", property = "AWS_PROFILE")
	String profile;

	@Parameter(defaultValue = "", name = "accesskey", property = "AWS_ACCESSKEY")
	String accesskey;

	@Parameter(defaultValue = "", name = "secretkey", property = "AWS_SECRETKEY")
	String secretkey;

	@Parameter(defaultValue = "", name = "token", property = "AWS_SESSIONTOKEN")
	String token;

	@Parameter(defaultValue = "", name = "registryId", property = "ECR_REGISTRY_ID")
	String registryId;

	@Parameter(defaultValue = "true", name = "mutable", property = "ECR_MUTABLE")
	String mutable;

	@Parameter(defaultValue = "", name = "lifecycleUrl", property = "ECR_LIFECYCLE_URL")
	String lifecycleUrl;

	@Parameter(defaultValue = "", name = "permissionsUrl", property = "ECR_PERMISSIONS_URL")
	String permissionsUrl;

	@Parameter(defaultValue = "", name = "encryptionType", property = "ECR_ENCRYPTION_TYPE")
	String encryptionType;

	@Parameter(defaultValue = "", name = "kmsKey", property = "ECR_KMS_KEY")
	String kmsKey;

	private static boolean isNullOrEmpty(String str) {
		if (str == null)
			return true;
		return str.isEmpty();
	}

	private AWSCredentialsProvider buildCredentialsProvider() {

		if (isNullOrEmpty(profile) == false) {
			return new ProfileCredentialsProvider(profile);
		} else if (isNullOrEmpty(accesskey) == false && isNullOrEmpty(secretkey) == false
				&& isNullOrEmpty(token) == false) {
			return new AWSStaticCredentialsProvider(new BasicSessionCredentials(accesskey, secretkey, token));
		} else if (isNullOrEmpty(accesskey) == false && isNullOrEmpty(secretkey) == false) {
			return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accesskey, secretkey));
		}

		return null;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		AmazonECRClientBuilder builder = AmazonECRClientBuilder.standard().withRegion(region);
		AWSCredentialsProvider credentialsProvider = buildCredentialsProvider();

		if (credentialsProvider != null)
			builder.withCredentials(credentialsProvider);

		AmazonECR ecr = builder.build();

		if (isRepositoryExists(ecr) == false) {
			try {
				CreateRepositoryRequest request = new CreateRepositoryRequest().withRepositoryName(repository);

				if (isNullOrEmpty(mutable) == false)
					request.withImageTagMutability(
							Boolean.valueOf(mutable) ? ImageTagMutability.MUTABLE : ImageTagMutability.IMMUTABLE);

				if (isNullOrEmpty(registryId) == false)
					request.withRegistryId(registryId);

				if (isNullOrEmpty(encryptionType) == false) {
					EncryptionConfiguration encryptionConfiguration = new EncryptionConfiguration();
					EncryptionType encType = EncryptionType.fromValue(encryptionType);

					encryptionConfiguration.setEncryptionType(encryptionType);

					if (encType == EncryptionType.KMS) {
						if (isNullOrEmpty(kmsKey))
							throw new KmsException("Missing key");

						encryptionConfiguration.setKmsKey(kmsKey);
					}

					request.setEncryptionConfiguration(encryptionConfiguration);
				}

				ecr.createRepository(request);
			} catch (AmazonECRException e) {
				getLog().error(String.format("Unable to create ECR repository %s", repository), e);

				throw new MojoFailureException(e, "Create ECR repository", e.getErrorMessage());
			}

			if (isNullOrEmpty(lifecycleUrl) == false) {
				try {
					applyLifecyclePolicy(ecr);
				} catch (IOException e) {
					getLog().warn(String.format("Unable to set lifecycle policy: %s on repository %s", lifecycleUrl,
							repository), e);
				} catch (AmazonECRException e) {
					getLog().error(String.format("Unable to set lifecycle policy: %s on repository %s", lifecycleUrl,
							repository), e);

					throw new MojoFailureException(e, "Create ECR repository", e.getErrorMessage());
				}
			}

			if (isNullOrEmpty(permissionsUrl) == false) {
				try {
					applyPermissionsPolicy(ecr);
				} catch (IOException e) {
					getLog().warn(String.format("Unable to set permissions policy: %s on repository %s", permissionsUrl,
							repository), e);
				} catch (AmazonECRException e) {
					getLog().error(String.format("Unable to set permissions policy: %s on repository %s",
							permissionsUrl, repository), e);

					throw new MojoFailureException(e, "Create ECR repository", e.getErrorMessage());
				}
			}

			getLog().info(String.format("The ECR repository %s is created", repository));
		} else {
			getLog().info(String.format("The ECR repository %s already exists", repository));
		}
	}

	private boolean isRepositoryExists(AmazonECR ecr) {

		DescribeRepositoriesRequest request = new DescribeRepositoriesRequest().withRepositoryNames(this.repository);

		if (isNullOrEmpty(registryId))
			request.withRegistryId(registryId);

		try {
			ecr.describeRepositories(request);
		} catch (RepositoryNotFoundException e) {
			return false;
		}

		return true;
	}

	private void applyLifecyclePolicy(AmazonECR ecr) throws IOException {
		String text = URLReader(new URL(lifecycleUrl));
		PutLifecyclePolicyRequest request = new PutLifecyclePolicyRequest().withRepositoryName(repository)
				.withLifecyclePolicyText(text);

		if (isNullOrEmpty(registryId) == false)
			request.withRegistryId(registryId);

		ecr.putLifecyclePolicy(request);
	}

	private void applyPermissionsPolicy(AmazonECR ecr) throws IOException {
		String text = URLReader(new URL(permissionsUrl));
		SetRepositoryPolicyRequest request = new SetRepositoryPolicyRequest().withRepositoryName(repository)
				.withPolicyText(text);

		if (isNullOrEmpty(registryId) == false)
			request.withRegistryId(registryId);

		ecr.setRepositoryPolicy(request);
	}

	public static String URLReader(URL url) throws IOException {
		try (InputStream in = url.openStream()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			return reader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}
}
