package com.msa4lmsv2academic.global.file;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class StoredFileNotFoundException extends BusinessException {
    public StoredFileNotFoundException(Throwable cause) {
        super(CustomResponseCode.NOT_FOUND_DATA,
                "첨부파일 원본이 저장소에 없습니다. 관리자에게 원본 복구를 요청해 주세요.", cause);
    }
}
