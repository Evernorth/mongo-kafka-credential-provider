package com.evernorth.mongo.kafka.auth;

import com.mongodb.AwsCredential;
import com.mongodb.MongoCredential;
import com.mongodb.kafka.connect.util.custom.credentials.CustomCredentialProvider;
import org.apache.kafka.common.config.ConfigException;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.evernorth.mongo.kafka.auth.Constants.*;

public class AwsAssumeRoleCredentialProvider implements CustomCredentialProvider {

    private StsClient stsClient;
    private AssumeRoleRequest roleRequest;

    @Override
    public MongoCredential getCustomCredential(Map<?, ?> configs) {
        Supplier<AwsCredential> awsFreshCredentialSupplier =
                () -> {
                    Credentials awsCredentials = stsClient.assumeRole(roleRequest).credentials();
                    return new AwsCredential(
                            awsCredentials.accessKeyId(),
                            awsCredentials.secretAccessKey(),
                            awsCredentials.sessionToken());
                };
        return MongoCredential.createAwsCredential(null, null)
                .withMechanismProperty(
                        MongoCredential.AWS_CREDENTIAL_PROVIDER_KEY, awsFreshCredentialSupplier);
    }

    @Override
    public void validate(Map<?, ?> configs) {
        if (configs.containsKey(ROLE_ARN)) {
            String roleArn = (String) configs.get(ROLE_ARN);
            if (roleArn == null || roleArn.isEmpty()) {
                throw new ConfigException("AWS Role ARN not provided.");
            }
        }else{
            throw new ConfigException("AWS Role ARN not provided.");
        }
    }

    @Override
    public void init(Map<?, ?> configs) {
        String roleArn = (String) configs.get(ROLE_ARN);
        Region region;
        if (configs.containsKey(REGION)) {
            region = Region.of((String) configs.get(REGION));
        } else {
            region = Region.US_EAST_1;
        }
        String sessionName;
        if (configs.containsKey(SESSION_NAME)) {
            sessionName = (String) configs.get(SESSION_NAME);
        } else {
            sessionName = "MONGO-CONNECTOR-SESSION-" + UUID.randomUUID();
        }
        stsClient =
                StsClient.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(region)
                        .build();
        roleRequest = AssumeRoleRequest.builder().roleArn(roleArn).roleSessionName(sessionName).build();
    }
}
