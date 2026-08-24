package com.msa4lmsv2academic.domain.provisioning.controller;

import com.msa4lmsv2academic.domain.provisioning.request.ProfessorProvisioningRequestDTO;
import com.msa4lmsv2academic.domain.provisioning.request.StudentProvisioningRequestDTO;
import com.msa4lmsv2academic.domain.provisioning.response.ProfessorProvisioningResponseDTO;
import com.msa4lmsv2academic.domain.provisioning.response.StudentProvisioningResponseDTO;
import com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/academic/account-provisionings")
@Tag(name = "Account Provisioning (Internal)", description = "Auth가 호출하는 학생·교수 학사정보 저장 및 학번·교번 생성 내부 API")
public class AccountProvisioningController {

    private final AccountProvisioningService accountProvisioningService;

    @Operation(
            summary = "학생 학사정보 프로비저닝 및 학번 생성",
            description = "Auth에서 전달받은 계정 ID와 학생 정보를 Academic DB에 저장하고 생성한 학번을 Auth에 반환합니다. 프론트에서 직접 호출하지 않는 내부 API입니다."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학생 정보 저장 및 학번 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청값, 학과 또는 전공 검증 실패", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "이미 프로비저닝된 사용자 또는 중복 이메일", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "학번 생성 또는 저장 실패", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @PostMapping("/students")
    public ResponseEntity<GlobalResponseDTO<StudentProvisioningResponseDTO>> provisionStudent(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = StudentProvisioningRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "학생 프로비저닝",
                                    value = "{\"userId\":1,\"name\":\"홍길동\",\"email\":\"student@example.com\",\"phoneNumber\":\"010-1234-5678\",\"address\":\"서울특별시\",\"departmentId\":5,\"majorId\":1,\"admissionYear\":2026}"
                            )
                    )
            )
            @Valid @RequestBody StudentProvisioningRequestDTO request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(accountProvisioningService.provisionStudent(request)));
    }

    @Operation(
            summary = "교수 인사정보 프로비저닝 및 교번 생성",
            description = "Auth에서 전달받은 계정 ID와 교수 정보를 Academic DB에 저장하고 생성한 교번을 Auth에 반환합니다. 프론트에서 직접 호출하지 않는 내부 API입니다."
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교수 정보 저장 및 교번 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 또는 학과 검증 실패", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "이미 프로비저닝된 사용자 또는 중복 이메일", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "교번 생성 또는 저장 실패", content = @Content(schema = @Schema(implementation = GlobalResponseDTO.class)))
    })
    @PostMapping("/professors")
    public ResponseEntity<GlobalResponseDTO<ProfessorProvisioningResponseDTO>> provisionProfessor(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ProfessorProvisioningRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "교수 프로비저닝",
                                    value = "{\"userId\":2,\"name\":\"김교수\",\"email\":\"professor@example.com\",\"phoneNumber\":\"010-9876-5432\",\"address\":\"서울특별시\",\"departmentId\":5,\"hireYear\":2026}"
                            )
                    )
            )
            @Valid @RequestBody ProfessorProvisioningRequestDTO request
    ) {
        return ResponseEntity.ok(GlobalResponseDTO.success(accountProvisioningService.provisionProfessor(request)));
    }
}
