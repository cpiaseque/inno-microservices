package com.innowise.paymentservice.config;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class LiquibaseConfig {
    @Value("${spring.data.mongodb.uri}")
    private String mongoDbUri;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @Bean
    @DependsOn("mongoTemplate")
    public Liquibase liquibase() throws LiquibaseException {
        ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();

        try (MongoLiquibaseDatabase database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                .openDatabase(mongoDbUri, null, null, null, resourceAccessor)) {

            Liquibase liquibase = new Liquibase(changeLog, resourceAccessor, database);
            liquibase.update("");

            return liquibase;
        }
    }
}