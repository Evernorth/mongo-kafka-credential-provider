package com.evernorth.mongo.kafka.auth;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.AwsCredential;
import com.mongodb.MongoCredential;
import org.apache.kafka.common.config.ConfigException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.evernorth.mongo.kafka.auth.Constants.*;

public class AwsAssumeRoleCredentialProviderTests {
    @Test
    public void testAwsAssumeRoleCredentialProvider(){
        Map<String, String> configValues = new HashMap<>();
        configValues.put(ROLE_ARN, "arn:aws:iam::<<account>>:role/<<role>>");
        configValues.put(REGION, "us-east-1");
        configValues.put(SESSION_NAME, "test-session");
        AwsAssumeRoleCredentialProvider awsAssumeRoleCredentialProvider = new AwsAssumeRoleCredentialProvider();
        awsAssumeRoleCredentialProvider.validate(configValues);
        awsAssumeRoleCredentialProvider.init(configValues);
        MongoCredential credential = awsAssumeRoleCredentialProvider.getCustomCredential(configValues);
        AuthenticationMechanism mechanism = credential.getAuthenticationMechanism();
        assert mechanism != null;
        Assert.assertEquals("MONGODB-AWS",mechanism.getMechanismName());
        Assert.assertEquals("$external",credential.getSource());
        Supplier<AwsCredential> awsFreshCredentialSupplier = credential.getMechanismProperty("aws_credential_provider", null);
        Assert.assertNotNull(awsFreshCredentialSupplier);
    }

    @Test
    public void testAwsAssumeRoleCredentialProviderWithDefaultValues(){
        Map<String, String> configValues = new HashMap<>();
        configValues.put(ROLE_ARN, "arn:aws:iam::<<account>>:role/<<role>>");
        /*configValues.put(REGION, "us-east-1");
        configValues.put(SESSION_NAME, "test-session");*/
        AwsAssumeRoleCredentialProvider awsAssumeRoleCredentialProvider = new AwsAssumeRoleCredentialProvider();
        awsAssumeRoleCredentialProvider.validate(configValues);
        awsAssumeRoleCredentialProvider.init(configValues);
        MongoCredential credential = awsAssumeRoleCredentialProvider.getCustomCredential(configValues);
        AuthenticationMechanism mechanism = credential.getAuthenticationMechanism();
        assert mechanism != null;
        Assert.assertEquals("MONGODB-AWS",mechanism.getMechanismName());
        Assert.assertEquals("$external",credential.getSource());
        Supplier<AwsCredential> awsFreshCredentialSupplier = credential.getMechanismProperty("aws_credential_provider", null);
        Assert.assertNotNull(awsFreshCredentialSupplier);
    }

    @Test(expected = ConfigException.class)
    public void testAwsAssumeRoleCredentialProviderWithoutRole(){
        Map<String, String> configValues = new HashMap<>();
        configValues.put(REGION, "us-east-1");
        configValues.put(SESSION_NAME, "test-session");
        AwsAssumeRoleCredentialProvider awsAssumeRoleCredentialProvider = new AwsAssumeRoleCredentialProvider();
        awsAssumeRoleCredentialProvider.validate(configValues);
    }

    @Test(expected = ConfigException.class)
    public void testAwsAssumeRoleCredentialProviderWithEmptyRole(){
        Map<String, String> configValues = new HashMap<>();
        configValues.put(REGION, "us-east-1");
        configValues.put(SESSION_NAME, "test-session");
        configValues.put(ROLE_ARN, "");
        AwsAssumeRoleCredentialProvider awsAssumeRoleCredentialProvider = new AwsAssumeRoleCredentialProvider();
        awsAssumeRoleCredentialProvider.validate(configValues);
    }

    @Test(expected = ConfigException.class)
    public void testAwsAssumeRoleCredentialProviderWithNullRole(){
        Map<String, String> configValues = new HashMap<>();
        configValues.put(REGION, "us-east-1");
        configValues.put(SESSION_NAME, "test-session");
        configValues.put(ROLE_ARN, null);
        AwsAssumeRoleCredentialProvider awsAssumeRoleCredentialProvider = new AwsAssumeRoleCredentialProvider();
        awsAssumeRoleCredentialProvider.validate(configValues);
    }
}
