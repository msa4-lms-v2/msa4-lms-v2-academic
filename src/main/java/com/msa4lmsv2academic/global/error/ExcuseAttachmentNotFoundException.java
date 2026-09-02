package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ExcuseAttachmentNotFoundException extends BusinessException {

    public ExcuseAttachmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "공결 증빙 파일을 찾을 수 없습니다.");
    }
}
