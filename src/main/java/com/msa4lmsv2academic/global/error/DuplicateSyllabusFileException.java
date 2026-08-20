package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateSyllabusFileException extends BusinessException {

    public DuplicateSyllabusFileException() {
        super(CustomResponseCode.DUPLICATE_DATA, "동일한 강의계획서 파일이 이미 등록되어 있습니다.");
    }
}
