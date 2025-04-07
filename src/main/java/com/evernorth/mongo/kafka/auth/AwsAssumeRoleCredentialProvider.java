package com.evernorth.mongo.kafka.auth;

import com.mongodb.AwsCredential;
import com.mongodb.MongoCredential;
import com.mongodb.kafka.connect.util.custom.credentials.CustomCredentialProvider;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private String externalId;

    static final Logger LOGGER = LoggerFactory.getLogger(AwsAssumeRoleCredentialProvider.class);

    @Override
    public MongoCredential getCustomCredential(Map<?, ?> configs) {
        Supplier<AwsCredential> awsFreshCredentialSupplier =
                () -> {
                    if(LOGGER.isDebugEnabled()){
                        LOGGER.debug("Getting new AWS credentials");
                    }
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
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Validating Role Arn");
        }
        if (configs.containsKey(ROLE_ARN)) {
            String roleArn = (String) configs.get(ROLE_ARN);
            if (roleArn == null || roleArn.isEmpty()) {
                throw new ConfigException("AWS Role ARN not provided.");
            }
        }else{
            throw new ConfigException("AWS Role ARN not provided.");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating External Id Config");
        }
        if (configs.containsKey(EXTERNAL_ID_ENABLED)) {
            boolean externalIdEnabled = Boolean.parseBoolean((String)configs.get(EXTERNAL_ID_ENABLED));
            String connectorClass = (String)configs.get(CONNECTOR_CLASS_CONFIG);
            if (externalIdEnabled)
                if (connectorClass.equals(MONGO_SINK_CONNECTOR_CLASS)) {
                    String topicsConfig = null;
                    if (configs.containsKey("topics"))
                        topicsConfig = (String)configs.get("topics");
                    if (topicsConfig == null || topicsConfig.isEmpty())
                        throw new ConfigException("topics must to be set when "+ EXTERNAL_ID_ENABLED
                                + " is set to true.");
                } else if (connectorClass.equals(MONGO_SOURCE_CONNECTOR_CLASS)) {
                    String dbNameConfig = (String)configs.get("database");
                    String collectionNameConfig = (String)configs.get("collection");
                    if (dbNameConfig == null || dbNameConfig
                            .isEmpty() || collectionNameConfig == null || collectionNameConfig

                            .isEmpty())
                        throw new ConfigException("database and collection must to be set when "
                                + EXTERNAL_ID_ENABLED + " is set to true.");
                }
        }
    }

    @Override
    public void init(Map<?, ?> configs) {
        String roleArn = (String) configs.get(ROLE_ARN);
        Region region;
        if (configs.containsKey(REGION)) {
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Region Provided in Config.");
            }
            region = Region.of((String) configs.get(REGION));
        } else {
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Defaulting to US EAST 1 Region.");
            }
            region = Region.US_EAST_1;
        }
        String sessionName;
        if (configs.containsKey(SESSION_NAME)) {
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Session Name Provided in Config.");
            }
            sessionName = (String) configs.get(SESSION_NAME);
        } else {
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("Generating Session Name with random GUID with format MONGO-CONNECTOR-SESSION-GUID.");
            }
            sessionName = "MONGO-CONNECTOR-SESSION-" + UUID.randomUUID();
        }
        boolean externalIdEnabled = false;
        if (configs.containsKey(EXTERNAL_ID_ENABLED)) {
            externalIdEnabled = Boolean.parseBoolean((String)configs.get(EXTERNAL_ID_ENABLED));
            if (LOGGER.isDebugEnabled() && externalIdEnabled)
                LOGGER.debug("External Id Enable Flag set to true.");
        }
        String connectorClass = (String)configs.get(CONNECTOR_CLASS_CONFIG);
        if (externalIdEnabled)
            if (connectorClass.equals(MONGO_SINK_CONNECTOR_CLASS)) {
                this.externalId = (String)configs.get("topics");
            } else if (connectorClass.equals(MONGO_SOURCE_CONNECTOR_CLASS)) {
                this
                        .externalId = configs.get("database") + "-" + configs.get("collection");
            }
        stsClient =
                StsClient.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(region)
                        .build();
        AssumeRoleRequest.Builder builder = AssumeRoleRequest.builder().roleArn(roleArn).roleSessionName(sessionName);
        if (externalId != null && !externalId.isEmpty()){
            builder.externalId(this.externalId);
        }
        this.roleRequest = builder.build();
    }
}
