package com.msa4lmsv2academic.global.config.openapi;

import com.msa4lmsv2academic.global.response.CustomResponseCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomApiResponse {

    CustomResponseCode[] value();
}
