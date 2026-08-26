package com.msa4lmsv2academic.global.config;

import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentApplicationErrorResponseDTO;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;

@Configuration
public class EnrollmentApplicationOpenApiConfig {
    @Bean
    public OpenApiCustomizer enrollmentApplicationErrorSchema() {
        return openApi -> {
            // 공통 응답 record를 변경하지 않고 이번 POST의 제네릭 오류 data 스키마만 등록합니다.
            var type = new ParameterizedTypeReference<GlobalResponseDTO<EnrollmentApplicationErrorResponseDTO>>() { };
            var resolved = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type.getType()));
            resolved.referencedSchemas.forEach(openApi::schema);
            openApi.schema("EnrollmentApplicationError", resolved.schema);
        };
    }
}
