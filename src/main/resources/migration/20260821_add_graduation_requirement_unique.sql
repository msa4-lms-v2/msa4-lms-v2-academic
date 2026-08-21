-- 자동 삭제하지 않는다. 결과가 있으면 적용할 기준 행을 담당자가 확인해 중복을 먼저 정리한다.
SELECT department_id, admission_year, COUNT(*) AS duplicate_count
FROM graduation_requirements
GROUP BY department_id, admission_year
HAVING COUNT(*) > 1;

-- 위 조회 결과가 없을 때 적용한다.
ALTER TABLE graduation_requirements
    DROP INDEX idx_graduation_requirements_department_year,
    ADD CONSTRAINT uk_graduation_requirements_department_year
        UNIQUE (department_id, admission_year);
