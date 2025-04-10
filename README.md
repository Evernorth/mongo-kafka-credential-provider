# Mongo Kafka Credential Provider

**Description**:  This repo provides an implementation of the pluggable interface [CustomCredentialProvider](https://github.com/mongodb/mongo-kafka/blob/master/src/main/java/com/mongodb/kafka/connect/util/custom/credentials/CustomCredentialProvider.java) from the [mongo-kafka](https://github.com/mongodb/mongo-kafka/) repo.

The [mongo-kafka connector](https://www.mongodb.com/docs/kafka-connector/current/) enabled a pluggable [interface](https://github.com/mongodb/mongo-kafka/commit/6e43b1d97f52be6d862328e442eea8ece5492d30) in release 1.13.0. 
This enhancement enabled injection of a custom credential provider in the mongo client that gets created for both the sink and the source connectors.
The implementation of this interface is not provided in the mongo-kafka connector repo as it could have different variations (such as different credential providers from AWS)
or any other credential provider that can be injected in the MongoClient.
The kafka connect framework allows us to provide custom jars that can be made available on the class path by providing a plugin location. 
This repo provides the means to build such external jar to provide the AWS authentication provider implementation.

Below is a sequence diagram that explains the initialization sequence that takes place between the connector and the external jar. For more details on how to enable the 
mongo kafka connector to use this, refer to instructions [here](https://github.com/mongodb/mongo-kafka?tab=readme-ov-file#custom-auth-provider-interface)

<img alt="Sequence Diagram" height="500" src="images/custom-credential-provider-sequence-diagram.jpg" width="600"/>

## How to use this repo with mongo kafka connector

1. Use ``mvn clean package`` to generate a jar.
2. Add the compiled JAR to the classpath/plugin path for your Kafka workers. For more information about plugin paths, see the Confluent [documentation](https://docs.confluent.io/platform/current/connect/community.html). Refer to this [guide](https://docs.confluent.io/platform/current/connect/userguide.html#) for more information on kafka connect.

## Technical Details

**AwsAssumeRoleCredentialProvider**

This class can be used with the mongo kafka connector hosted on confluent platform within an AWS container. It uses the ``DefaultCredentialsProvider`` from aws sdk to assume a role provided in configuration, use the session credentials from the assumed role and wrap a supplier lambda in the MongoCredential object that it returns to be wrapped in the MongoClient object.

Example -

<img alt="Design Diagram" height="500" src="images/Design-Ec2.jpg" width="600"/>

- It uses the below properties to accept an AWS roleArn, an AWS region and an AWS session name. 
  ````
  mongodbaws.auth.mechanism.roleArn
  mongodbaws.auth.mechanism.region
  mongodbaws.auth.mechanism.roleSessionName
  ````
- It validates that the roleArn is always passed as an argument. Region will default to us-east-1 and role session name defaults to MONGO-CONNECTOR-SESSION-{random UUID}) if not included as arguments.

**External ID Validation**

With the release of 1.0.1 version of this library, we have added support for external ID validation to address [the confused deputy problem](https://docs.aws.amazon.com/IAM/latest/UserGuide/confused-deputy.html). Below is an overview.

When Kafka and Kafka Connect clusters are deployed as shared resources, and different teams deploy their mongo kafka connectors in that shared space, we run into the confused deputy problem. The below diagram illustrates this scenario.

<img alt="External Id" height="500" src="images/confused-deputy.jpg" width="700"/>

The above diagram shows the case for a sink connector. In this case, the kafka role can assume roles from AWS Accounts A, B and C and consequently has the ability to send traffic from any topic to any Atlas Database/Collection. The same is applicable for the source connector as well.

To solve this, we have made changes to the ``AwsAssumeRoleCredentialProvider`` class to enable addition of an external id if the property ``mongodbaws.auth.mechanism.externalId.enabled`` is set to true. The external id is set to the value derived from ``topics`` property of the sink connector or the ``database`` and ``collection`` (concatenated with a - separator) properties of the source connector based on the type of connector (sink or source) in use.

The Connector sends this external ID as part of the assume role request. Consequently, an external id condition needs to be added to the trust relationships of the role that is assumed. An example is given below.

````
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::<<ACCOUNT_ID>>:role/Enterprise/KafkaConnect"
            },
            "Action": "sts:AssumeRole",
            "Condition": {
                "ForAnyValue:StringLike": {
                    "sts:ExternalId": [
                        "TopicA",
                        "db-collection"
                    ]
                }
            }
        }
    ]
}
````

This policy will work for both a source and sink connector.

## Tests and Coverage

- Unit tests can be located at ``com.evernorth.mongo.kafka.auth.AwsAssumeRoleCredentialProviderTests``
- Jacoco plugin is used for code coverage.

## Dependencies

This repo uses the below jars

| Dependency            | jar                                    | version  |
|-----------------------|----------------------------------------|----------|
| AWS SDK               | software.amazon.awssdk.sts             | 2.25.14  | 
| Mongo Kafka Connector | org.mongodb.kafka.mongo-kafka-connect  | 1.13.0   |

## Limitations

- The external (to the connector) jar that this repo produces has been tested with Confluent platform hosted on AWS EC2 instances. It has not been tested on confluent cloud.
- The mongo kafka connector needs external jars that are compiled using Java 8. Hence,  we have the java version set to 1.8. 

## License
Mongo Kafka Credential Provider is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Changelog
| Version | description|
|---------|------------|
| 1.0.0   | Initial Version|
| 1.0.1 | Changes to support External Id Validation|  

----
