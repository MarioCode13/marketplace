package dev.marketplace.marketplace.config;

import dev.marketplace.marketplace.exceptions.AuthException;
import dev.marketplace.marketplace.exceptions.UserAlreadyExistsException;
import dev.marketplace.marketplace.exceptions.InvalidCredentialsException;
import dev.marketplace.marketplace.exceptions.ValidationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof UserAlreadyExistsException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "USER_ALREADY_EXISTS"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof InvalidCredentialsException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "INVALID_CREDENTIALS"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof ValidationException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "VALIDATION_ERROR"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof AuthException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "AUTH_ERROR"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "VALIDATION_ERROR"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof dev.marketplace.marketplace.exceptions.BusinessNotFoundException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.NOT_FOUND)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "BUSINESS_NOT_FOUND"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        if (ex instanceof dev.marketplace.marketplace.exceptions.PermissionDeniedException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.FORBIDDEN)
                    .message(ex.getMessage())
                    .extensions(java.util.Map.of("code", "PERMISSION_DENIED"))
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }

        // For any other exceptions, return a generic error
        return GraphqlErrorBuilder.newError()
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("An unexpected error occurred. Please try again.")
                .extensions(java.util.Map.of("code", "INTERNAL_ERROR"))
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }
}
