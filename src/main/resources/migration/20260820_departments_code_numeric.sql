-- 학과코드를 3자리 숫자로 통일(학번/교번 생성 규칙과 맞춤). 기존 영문 코드를 먼저 숫자로 치환한 뒤 컬럼 길이를 줄인다.
UPDATE departments SET code = '001' WHERE code = 'CSE';
UPDATE departments SET code = '002' WHERE code = 'ELEC';
UPDATE departments SET code = '003' WHERE code = 'KOR';
UPDATE departments SET code = '004' WHERE code = 'OPEN';

ALTER TABLE departments MODIFY COLUMN code VARCHAR(3) NOT NULL;
