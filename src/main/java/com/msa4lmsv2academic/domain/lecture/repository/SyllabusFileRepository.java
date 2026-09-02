package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.SyllabusFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyllabusFileRepository extends JpaRepository<SyllabusFile, Long> {

    boolean existsByLectureIdAndOriginalNameAndSize(Long lectureId, String originalName, long size);

    @Query("""
            select syllabusFile
            from SyllabusFile syllabusFile
            join fetch syllabusFile.uploadedBy
            where syllabusFile.lecture.id = :classId
            order by syllabusFile.createdAt desc, syllabusFile.id desc
            """)
    List<SyllabusFile> findAllByClassId(@Param("classId") Long classId);

    @Query("""
            select syllabusFile
            from SyllabusFile syllabusFile
            join fetch syllabusFile.lecture lecture
            join fetch lecture.professor professor
            join fetch professor.user
            join fetch syllabusFile.uploadedBy
            where syllabusFile.id = :fileId
            """)
    Optional<SyllabusFile> findDetailById(@Param("fileId") Long fileId);
}
