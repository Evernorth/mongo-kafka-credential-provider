package com.evernorth.mongo.kafka.auth;

public final class Constants {
    public static final String ROLE_ARN = "mongodbaws.auth.mechanism.roleArn";
    public static final String REGION = "mongodbaws.auth.mechanism.region";
    public static final String SESSION_NAME = "mongodbaws.auth.mechanism.roleSessionName";
    public static final String EXTERNAL_ID_ENABLED = "mongodbaws.auth.mechanism.externalId.enabled";
    public static final String CONNECTOR_CLASS_CONFIG = "connector.class";
    public static final String MONGO_SINK_CONNECTOR_CLASS = "com.mongodb.kafka.connect.MongoSinkConnector";
    public static final String MONGO_SOURCE_CONNECTOR_CLASS = "com.mongodb.kafka.connect.MongoSourceConnector";
    public static final String TOPICS_CONFIG = "topics";
    public static final String DATABASE_CONFIG = "database";
    public static final String COLLECTION_CONFIG = "collection";
}
