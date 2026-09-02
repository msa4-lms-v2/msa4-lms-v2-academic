package com.msa4lmsv2academic.global.config.openapi;

import com.msa4lmsv2academic.global.response.CustomResponseCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

@Component
public class ApiResponseCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        CustomApiResponse annotation = handlerMethod.getMethodAnnotation(CustomApiResponse.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(CustomApiResponse.class);
        }
        if (annotation == null) {
            return operation;
        }

        Map<Integer, List<CustomResponseCode>> responseCodesByHttpStatus = new LinkedHashMap<>();
        for (CustomResponseCode responseCode : annotation.value()) {
            responseCodesByHttpStatus
                    .computeIfAbsent(responseCode.getHttpStatus().value(), ignored -> new ArrayList<>())
                    .add(responseCode);
        }

        responseCodesByHttpStatus.forEach((httpStatus, responseCodes) -> {
            MediaType mediaType = new MediaType();

            responseCodes.forEach(responseCode -> {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("code", responseCode.getCode());
                body.put("message", responseCode.getMessage());
                body.put("data", null);

                mediaType.addExamples(
                        responseCode.name(),
                        new Example().summary(responseCode.getMessage()).value(body)
                );
            });

            Content content = new Content().addMediaType("application/json", mediaType);
            operation.getResponses().addApiResponse(
                    String.valueOf(httpStatus),
                    new ApiResponse().description("공통 오류 응답").content(content)
            );
        });

        return operation;
    }
}
