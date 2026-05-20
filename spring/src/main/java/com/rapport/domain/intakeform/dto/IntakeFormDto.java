package com.rapport.domain.intakeform.dto;

import com.rapport.domain.intakeform.entity.IntakeForm;
import lombok.*;

import java.time.LocalDateTime;

public class IntakeFormDto {

    /** 내담자가 제출하는 필드 (name/gender/birthDate/counselorName은 서버 자동 조회) */
    @Getter
    @NoArgsConstructor
    public static class SubmitRequest {
        private String counselingRoute;
        private String mainProblem;
        private String onsetPeriod;
        private String previousCounseling;
        private String developmentPregnancy;
        private String developmentGrowth;
        private String familyRelationship;
        private String interpersonalRelationship;
        private String socialAdaptation;
        private String stressCoping;
        private String socialPsychologicalSupport;
        private String medicalHistory;
        private String counselingExpectation;
        private String testConducted;
        private String notes;
    }

    /** null 필드는 기존값 유지 (PATCH) */
    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String counselingRoute;
        private String mainProblem;
        private String onsetPeriod;
        private String previousCounseling;
        private String developmentPregnancy;
        private String developmentGrowth;
        private String familyRelationship;
        private String interpersonalRelationship;
        private String socialAdaptation;
        private String stressCoping;
        private String socialPsychologicalSupport;
        private String medicalHistory;
        private String counselingExpectation;
        private String testConducted;
        private String notes;
    }

    /** intake_forms 조회 응답 */
    @Getter
    @Builder
    public static class IntakeFormResponse {
        private Long id;
        private Long bookingId;
        private FormData formData;
        private LocalDateTime submittedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static IntakeFormResponse of(IntakeForm form, FormData formData) {
            return IntakeFormResponse.builder()
                    .id(form.getId())
                    .bookingId(form.getBooking().getId())
                    .formData(formData)
                    .submittedAt(form.getSubmittedAt())
                    .createdAt(form.getCreatedAt())
                    .updatedAt(form.getUpdatedAt())
                    .build();
        }
    }

    /** 상담사의 접수면접지 요청 응답 */
    @Getter
    @Builder
    public static class RequestResponse {
        private LocalDateTime requestedAt;
        private Long chatRoomId;
    }

    /** form_data JSON 구조 */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FormData {
        private String name;
        private String gender;
        private String birthDate;
        private String counselorName;
        private String counselingRoute;
        private String mainProblem;
        private String onsetPeriod;
        private String previousCounseling;
        private String developmentPregnancy;
        private String developmentGrowth;
        private String familyRelationship;
        private String interpersonalRelationship;
        private String socialAdaptation;
        private String stressCoping;
        private String socialPsychologicalSupport;
        private String medicalHistory;
        private String counselingExpectation;
        private String testConducted;
        private String notes;

        public void applyUpdate(UpdateRequest req) {
            if (req.getCounselingRoute()           != null) this.counselingRoute           = req.getCounselingRoute();
            if (req.getMainProblem()               != null) this.mainProblem               = req.getMainProblem();
            if (req.getOnsetPeriod()               != null) this.onsetPeriod               = req.getOnsetPeriod();
            if (req.getPreviousCounseling()        != null) this.previousCounseling        = req.getPreviousCounseling();
            if (req.getDevelopmentPregnancy()      != null) this.developmentPregnancy      = req.getDevelopmentPregnancy();
            if (req.getDevelopmentGrowth()         != null) this.developmentGrowth         = req.getDevelopmentGrowth();
            if (req.getFamilyRelationship()        != null) this.familyRelationship        = req.getFamilyRelationship();
            if (req.getInterpersonalRelationship() != null) this.interpersonalRelationship = req.getInterpersonalRelationship();
            if (req.getSocialAdaptation()          != null) this.socialAdaptation          = req.getSocialAdaptation();
            if (req.getStressCoping()              != null) this.stressCoping              = req.getStressCoping();
            if (req.getSocialPsychologicalSupport()!= null) this.socialPsychologicalSupport= req.getSocialPsychologicalSupport();
            if (req.getMedicalHistory()            != null) this.medicalHistory            = req.getMedicalHistory();
            if (req.getCounselingExpectation()     != null) this.counselingExpectation     = req.getCounselingExpectation();
            if (req.getTestConducted()             != null) this.testConducted             = req.getTestConducted();
            if (req.getNotes()                     != null) this.notes                     = req.getNotes();
        }
    }
}
