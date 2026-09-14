-- 기존 시드 공지에 학생·교수 작성자가 연결된 데이터를 유일한 관리자 계정으로 보정한다.
-- 신규 공지는 NoticeService가 로그인한 ADMIN 사용자를 author_id로 저장하므로 대상이 아니다.
UPDATE notices AS notice
JOIN (
    SELECT id
    FROM users
    WHERE role = 'ADMIN'
    ORDER BY id
    LIMIT 1
) AS admin_user ON TRUE
SET notice.author_id = admin_user.id
WHERE notice.author_id <> admin_user.id;
