package com.msa4lmsv2academic.graduation.credit.application;

import com.msa4lmsv2academic.graduation.credit.model.CreditDiagnosisSource;

public interface CreditDiagnosisDataProvider {

    CreditDiagnosisSource load(long studentId);
}
