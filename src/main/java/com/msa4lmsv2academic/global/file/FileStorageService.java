package com.msa4lmsv2academic.global.file;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import io.minio.errors.ErrorResponseException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Duration DOWNLOAD_URL_EXPIRY = Duration.ofDays(1);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    // MinIO에 파일을 업로드하고 버킷 내 objectKey를 반환한다. 실제 파일 내용은 저장하지 않고 objectKey만 DB에 남긴다.
    public String upload(String pathPrefix, MultipartFile file) {
        String objectKey = pathPrefix + "/" + UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        return uploadObject(objectKey, file);
    }

    public String uploadEvidence(String pathPrefix, MultipartFile file) {
        String objectKey = pathPrefix + "/" + UUID.randomUUID() + ".pdf";
        return uploadObject(objectKey, file);
    }

    private String uploadObject(String objectKey, MultipartFile file) {
        // file.getInputStream() 실패(로컬 업로드 데이터 문제)와 minioClient.putObject() 실패(MinIO
        // 연결/저장소 문제)는 서로 다른 원인이지만 둘 다 IOException으로 던져질 수 있어, 같은 try 블록에서
        // 묶어 잡으면 네트워크 장애까지 "파일을 읽을 수 없습니다"로 잘못 안내하게 된다. 스트림 open은
        // 별도로 처리해 원인에 맞는 메시지를 준다.
        try (var inputStream = openInputStream(file)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException(
                    "파일 저장소 업로드에 실패했습니다. 잠시 후 다시 시도해 주세요: " + file.getOriginalFilename(), e);
        }
        return objectKey;
    }

    private InputStream openInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new FileStorageException("파일을 읽을 수 없습니다: " + file.getOriginalFilename(), e);
        }
    }

    // 조회용 임시 서명 URL을 발급한다(만료 시간 존재, 버킷은 비공개 유지).
    public String presignedDownloadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry((int) DOWNLOAD_URL_EXPIRY.toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("다운로드 URL 발급에 실패했습니다: " + objectKey, e);
        }
    }

    public byte[] download(String objectKey) {
        try (var inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build())) {
            return inputStream.readAllBytes();
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code())) {
                throw new StoredFileNotFoundException(exception);
            }
            throw new FileStorageException("파일 저장소에서 다운로드를 처리하지 못했습니다.", exception);
        } catch (Exception exception) {
            throw new FileStorageException("파일 다운로드에 실패했습니다. 잠시 후 다시 시도해 주세요.", exception);
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("파일 삭제에 실패했습니다: " + objectKey, exception);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^a-zA-Z0-9._\\-가-힣]", "_");
    }
}
