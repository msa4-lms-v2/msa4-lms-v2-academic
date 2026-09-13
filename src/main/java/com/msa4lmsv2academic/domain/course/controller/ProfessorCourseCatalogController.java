package com.msa4lmsv2academic.domain.course.controller;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.course.repository.CourseCatalogRepository;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @Validated
public class ProfessorCourseCatalogController {
 private final CourseCatalogRepository repository;
 public record Item(Long id,String code,String name,byte credits,String completionType,Byte targetGrade,String departmentName,String collegeName) {
  static Item from(Course c) { return new Item(c.getId(),c.getCode(),c.getName(),c.getCredits(),c.getCompletionType().name(),c.getTargetGrade(),c.getDepartment().getName(),c.getDepartment().getCollege()==null?null:c.getDepartment().getCollege().getName()); }
 }
 @GetMapping("/api/academic/professors/me/course-catalog")
 @PreAuthorize("hasRole('PROFESSOR')") @Transactional(readOnly=true)
 public GlobalResponseDTO<PageResponseDTO<Item>> search(
   @RequestParam(defaultValue="") @Size(max=100) String keyword,
   @RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,
   @RequestParam(defaultValue="10") @Min(1) @Max(100) int size) {
  var result=repository.search(keyword.trim(),PageRequest.of(page-1,size));
  return GlobalResponseDTO.success(new PageResponseDTO<>(result.getContent().stream().map(Item::from).toList(),result.getTotalElements(),page,size,result.hasNext()));
 }
}
