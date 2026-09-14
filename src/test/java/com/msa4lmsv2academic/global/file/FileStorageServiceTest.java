package com.msa4lmsv2academic.global.file;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {
    private final MinioClient minio = mock(MinioClient.class);
    private final FileStorageService service = new FileStorageService(minio);

    private void storageFailure(String code) throws Exception {
        ReflectionTestUtils.setField(service, "bucket", "test-only");
        ErrorResponse error = mock(ErrorResponse.class);
        when(error.code()).thenReturn(code);
        var exception = new ErrorResponseException(error, null, "test-only");
        when(minio.getObject(any(GetObjectArgs.class))).thenThrow(exception);
    }

    @Test
    void missingObjectReturnsRecoverableNotFoundWithoutLeakingObjectKey() throws Exception {
        storageFailure("NoSuchKey");
        assertThatThrownBy(() -> service.download("private/key-with-name.hwpx"))
                .isInstanceOf(StoredFileNotFoundException.class)
                .hasMessageContaining("원본 복구").hasMessageNotContaining("private/");
    }

    @Test
    void storagePermissionFailureMustNotBeMisreportedAsMissingFile() throws Exception {
        storageFailure("AccessDenied");
        assertThatThrownBy(() -> service.download("private/key-with-name.hwpx"))
                .isInstanceOf(FileStorageException.class).hasMessageNotContaining("private/");
    }
}
