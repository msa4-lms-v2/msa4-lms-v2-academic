package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.global.file.FileStorageException;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class DepartmentTransferTemplateService {
    private static final String BASE_PATH = "templates/department-transfer/";

    public TemplateFile studyPlan() {
        return read("study-plan.hwp", "학업계획서 양식.hwp");
    }

    public TemplateFile selfIntroduction() {
        return read("self-introduction.hwp", "자기소개서 양식.hwp");
    }

    private TemplateFile read(String resourceName, String filename) {
        try (var input = new ClassPathResource(BASE_PATH + resourceName).getInputStream()) {
            return new TemplateFile(filename, input.readAllBytes());
        } catch (IOException exception) {
            throw new FileStorageException("학적 변경 신청 양식을 읽을 수 없습니다.", exception);
        }
    }

    public record TemplateFile(String filename, byte[] content) { }
}
