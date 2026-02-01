package org.refit.refitbackend.global.swagger;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.refit.refitbackend.global.error.ExceptionType;
import org.refit.refitbackend.global.response.ErrorResponse;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiBadRequestError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiConflictError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiNotFoundError;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiRequestBody;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiUnauthorizedError;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

@Configuration
public class SwaggerApiCustomizerConfig {

    @Bean
    public OperationCustomizer swaggerApiCustomizer() {
        return (operation, handlerMethod) -> {
            SwaggerApiSuccess success = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SwaggerApiSuccess.class);
            if (success != null) {
                if (!success.summary().isBlank()) {
                    operation.setSummary(success.summary());
                }
                if (!success.operationDescription().isBlank()) {
                    operation.setDescription(success.operationDescription());
                }
                if (success.wrapApiResponse() && !ApiResponse.class.equals(success.implementation())) {
                    Schema<?> schema = buildApiResponseSchema(success.responseCode(), success.responseDescription(), success.implementation());
                    addResponseWithSchema(operation, success.responseCode(), success.responseDescription(), schema, null, false);
                } else {
                    addResponse(operation, success.responseCode(), success.responseDescription(), success.implementation(), null);
                }
                if (!"200".equals(success.responseCode())) {
                    ApiResponses responses = operation.getResponses();
                    if (responses != null) {
                        responses.remove("200");
                    }
                }
            }

            SwaggerApiNotFoundError notFound = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SwaggerApiNotFoundError.class);
            if (notFound != null) {
                Map<String, Example> examples = null;
                ExceptionType[] types = notFound.types();
                if (types != null && types.length > 0) {
                    examples = new LinkedHashMap<>();
                    for (ExceptionType type : types) {
                        examples.put(type.getCode(), buildErrorExample(type));
                    }
                }
                addResponse(operation, notFound.responseCode(), notFound.description(), ErrorResponse.class, examples);
            }

            SwaggerApiConflictError conflict = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SwaggerApiConflictError.class);
            if (conflict != null) {
                Map<String, Example> examples = null;
                ExceptionType[] types = conflict.types();
                if (types != null && types.length > 0) {
                    examples = new LinkedHashMap<>();
                    for (ExceptionType type : types) {
                        examples.put(type.getCode(), buildErrorExample(type));
                    }
                }
                addResponse(operation, conflict.responseCode(), conflict.description(), ErrorResponse.class, examples);
            }

            SwaggerApiBadRequestError badRequest = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SwaggerApiBadRequestError.class);
            if (badRequest != null) {
                String description = badRequest.description();
                Map<String, Example> examples = null;
                ExceptionType[] types = badRequest.types();
                if (types != null && types.length > 0) {
                    StringJoiner joiner = new StringJoiner(", ");
                    examples = new LinkedHashMap<>();
                    for (ExceptionType type : types) {
                        joiner.add(type.getCode());
                        examples.put(type.getCode(), buildErrorExample(type));
                    }
                    description = description + " (" + joiner + ")";
                }
                addResponse(operation, badRequest.responseCode(), description, ErrorResponse.class, examples);
            }

            SwaggerApiUnauthorizedError unauthorized = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SwaggerApiUnauthorizedError.class);
            if (unauthorized != null) {
                Map<String, Example> examples = null;
                ExceptionType[] types = unauthorized.types();
                if (types != null && types.length > 0) {
                    examples = new LinkedHashMap<>();
                    for (ExceptionType type : types) {
                        examples.put(type.getCode(), buildErrorExample(type));
                    }
                }
                addResponse(operation, unauthorized.responseCode(), unauthorized.description(), ErrorResponse.class, examples);
            }

            java.util.Collection<SwaggerApiError> errors = AnnotatedElementUtils
                    .findMergedRepeatableAnnotations(handlerMethod.getMethod(), SwaggerApiError.class);
            if (errors != null && !errors.isEmpty()) {
                for (SwaggerApiError error : errors) {
                    Map<String, Example> examples = null;
                    ExceptionType[] types = error.types();
                    if (types != null && types.length > 0) {
                        examples = new LinkedHashMap<>();
                        for (ExceptionType type : types) {
                            examples.put(type.getCode(), buildErrorExample(type));
                        }
                    }
                    addResponse(operation, error.responseCode(), error.description(), error.implementation(), examples);
                }
            }

            SwaggerApiRequestBody requestBody = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), SwaggerApiRequestBody.class);
            if (requestBody != null) {
                operation.setRequestBody(buildRequestBody(requestBody));
            }

            return operation;
        };
    }

    private void addResponse(io.swagger.v3.oas.models.Operation operation, String code, String description, Class<?> schemaClass, Map<String, Example> examples) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(code)) {
            return;
        }

        boolean isErrorSchema = schemaClass.equals(ErrorResponse.class);
        Schema<?> schema = isErrorSchema
                ? buildErrorResponseSchema()
                : new Schema<>().$ref("#/components/schemas/" + schemaClass.getSimpleName());
        addResponseWithSchema(operation, code, description, schema, examples, isErrorSchema);
    }

    private void addResponseWithSchema(io.swagger.v3.oas.models.Operation operation, String code, String description, Schema<?> schema, Map<String, Example> examples, boolean isErrorSchema) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(code)) {
            return;
        }
        MediaType mediaType = new MediaType().schema(schema);
        Map<String, Example> resolvedExamples = examples;
        if (resolvedExamples == null && isErrorSchema) {
            resolvedExamples = new LinkedHashMap<>();
            resolvedExamples.put("example", buildErrorExample("INVALID_REQUEST", "요청이 올바르지 않습니다."));
        }
        if (resolvedExamples != null) {
            for (Map.Entry<String, Example> entry : resolvedExamples.entrySet()) {
                mediaType.addExamples(entry.getKey(), entry.getValue());
            }
        }
        Content content = new Content().addMediaType("application/json", mediaType);

        ApiResponse response = new ApiResponse()
                .description(description)
                .content(content);

        responses.addApiResponse(code, response);
    }

    private RequestBody buildRequestBody(SwaggerApiRequestBody requestBody) {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/" + requestBody.implementation().getSimpleName());
        MediaType mediaType = new MediaType().schema(schema);

        String[] examples = requestBody.examples();
        String[] exampleNames = requestBody.exampleNames();
        if (examples != null && examples.length > 0) {
            for (int i = 0; i < examples.length; i++) {
                String name = (exampleNames != null && exampleNames.length > i && !exampleNames[i].isBlank())
                        ? exampleNames[i]
                        : "example_" + (i + 1);
                Example example = new Example().value(examples[i]);
                mediaType.addExamples(name, example);
            }
        }

        Content content = new Content().addMediaType("application/json", mediaType);
        RequestBody rb = new RequestBody();
        rb.setRequired(requestBody.required());
        if (!requestBody.description().isBlank()) {
            rb.setDescription(requestBody.description());
        }
        rb.setContent(content);
        return rb;
    }

    private Example buildErrorExample(ExceptionType type) {
        return buildErrorExample(type.getCode(), type.getMessage());
    }

    private Example buildErrorExample(String code, String message) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("code", code);
        value.put("message", message);
        value.put("data", null);
        return new Example().value(value);
    }

    private Schema<?> buildApiResponseSchema(String responseCode, String responseDescription, Class<?> dataClass) {
        Schema<Object> schema = new Schema<>();
        schema.setType("object");

        Schema<String> code = new Schema<>();
        code.setType("string");
        code.setDescription("응답 코드");
        code.setExample("201".equals(responseCode) ? "CREATED" : "OK");

        Schema<String> message = new Schema<>();
        message.setType("string");
        message.setDescription("응답 메시지");
        message.setExample(responseDescription);

        Schema<?> data = new Schema<>();
        data.setDescription("응답 데이터");
        if (!Void.class.equals(dataClass)) {
            data.set$ref("#/components/schemas/" + dataClass.getSimpleName());
        } else {
            data.setNullable(true);
        }

        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("code", code);
        properties.put("message", message);
        properties.put("data", data);
        schema.setProperties(properties);

        return schema;
    }

    private Schema<?> buildErrorResponseSchema() {
        Schema<Object> schema = new Schema<>();
        schema.setType("object");

        Schema<String> code = new Schema<>();
        code.setType("string");
        code.setDescription("응답 코드");
        code.setExample("INVALID_REQUEST");

        Schema<String> message = new Schema<>();
        message.setType("string");
        message.setDescription("응답 메시지");
        message.setExample("요청이 올바르지 않습니다.");

        Schema<Object> data = new Schema<>();
        data.setDescription("응답 데이터");
        data.setNullable(true);

        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put("code", code);
        properties.put("message", message);
        properties.put("data", data);
        schema.setProperties(properties);

        return schema;
    }
}
