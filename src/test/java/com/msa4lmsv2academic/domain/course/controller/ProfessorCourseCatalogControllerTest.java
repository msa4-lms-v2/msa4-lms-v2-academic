package com.msa4lmsv2academic.domain.course.controller;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.course.repository.CourseCatalogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfessorCourseCatalogControllerTest {
    @Test void mapsCourseAndPreservesOneBasedPaging() {
        var repository = mock(CourseCatalogRepository.class);
        var course = mock(Course.class, RETURNS_DEEP_STUBS);
        when(course.getId()).thenReturn(3L);
        when(course.getCode()).thenReturn("CS101");
        when(course.getName()).thenReturn("자료구조");
        when(course.getCredits()).thenReturn((byte) 3);
        when(course.getCompletionType()).thenReturn(CompletionType.MAJOR_REQUIRED);
        when(course.getDepartment().getName()).thenReturn("컴퓨터공학과");
        when(course.getDepartment().getCollege()).thenReturn(null);
        when(repository.search("자료", PageRequest.of(1, 10))).thenReturn(new PageImpl<>(List.of(course), PageRequest.of(1, 10), 21));
        var data = new ProfessorCourseCatalogController(repository).search(" 자료 ", 2, 10).data();
        assertEquals(2, data.page());
        assertTrue(data.hasNext());
        assertEquals(21, data.totalCount());
        assertEquals("CS101", data.items().getFirst().code());
        assertEquals("MAJOR_REQUIRED", data.items().getFirst().completionType());
        assertNull(data.items().getFirst().collegeName());
    }
}
