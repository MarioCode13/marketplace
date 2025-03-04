package dev.marketplace.marketplace.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLScalarConfig {

    @Bean
    public GraphQLScalarType uploadScalar() {
        return ExtendedScalars.Upload;
    }
}