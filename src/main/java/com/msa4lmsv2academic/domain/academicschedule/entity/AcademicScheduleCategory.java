package com.msa4lmsv2academic.domain.academicschedule.entity;

public enum AcademicScheduleCategory {
    ENROLLMENT("수강신청", "수강신청 안내", "수강신청 기간입니다. 기간 내에 수강신청을 완료해 주세요."),
    COURSE_CORRECTION("수강정정", "수강정정 안내", "수강정정 기간입니다. 수강 과목을 확인하고 변경해 주세요."),
    LEAVE("휴학", "휴학 신청 안내", "휴학 신청 기간과 신청 방법을 확인해 주세요."),
    RETURN("복학", "복학 신청 안내", "복학 신청 기간과 신청 방법을 확인해 주세요."),
    GRADE_ENTRY("성적입력", "성적입력 안내", "성적입력 기간과 유의사항을 확인해 주세요."),
    GRADE_CORRECTION("성적정정", "성적정정 안내", "성적정정 기간과 신청 절차를 확인해 주세요."),
    SCHOLARSHIP("장학금", "장학금 안내", "장학금 신청 기간과 지원 요건을 확인해 주세요."),
    TUITION("등록금", "등록금 납부 안내", "등록금 납부 기간과 납부 방법을 확인해 주세요."),
    OTHER("기타", "기타 학사일정", "학사일정 내용을 입력해 주세요.");

    private final String label;
    private final String defaultTitle;
    private final String defaultContent;

    AcademicScheduleCategory(String label, String defaultTitle, String defaultContent) {
        this.label = label;
        this.defaultTitle = defaultTitle;
        this.defaultContent = defaultContent;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public String getDefaultContent() {
        return defaultContent;
    }

}
