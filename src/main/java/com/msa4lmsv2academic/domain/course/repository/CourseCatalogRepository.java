package com.msa4lmsv2academic.domain.course.repository;
import com.msa4lmsv2academic.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
public interface CourseCatalogRepository extends JpaRepository<Course, Long> {
 @Query(value = """
  select c from Course c join fetch c.department d left join fetch d.college college
  where d.active = true and (college is null or college.active = true)
  and (:keyword = '' or lower(c.name) like lower(concat('%', :keyword, '%'))
       or lower(c.code) like lower(concat('%', :keyword, '%')))
  order by c.code, c.id
  """, countQuery="""
  select count(c) from Course c join c.department d left join d.college college
  where d.active = true and (college is null or college.active = true)
  and (:keyword = '' or lower(c.name) like lower(concat('%', :keyword, '%'))
       or lower(c.code) like lower(concat('%', :keyword, '%')))
  """)
 Page<Course> search(@Param("keyword") String keyword, Pageable pageable);
}
