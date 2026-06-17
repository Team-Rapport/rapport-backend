package com.rapport.domain.intakeform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.chat.entity.ChatMessage;
import com.rapport.domain.chat.entity.ChatMessageRepository;
import com.rapport.domain.chat.entity.ChatRoom;
import com.rapport.domain.chat.entity.ChatRoomRepository;
import com.rapport.domain.intakeform.dto.IntakeFormDto;
import com.rapport.domain.intakeform.entity.IntakeForm;
import com.rapport.domain.intakeform.entity.IntakeFormRepository;
import com.rapport.domain.notification.service.NotificationService;
import com.rapport.domain.user.entity.User;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntakeFormService {

    private final IntakeFormRepository    intakeFormRepository;
    private final BookingRepository       bookingRepository;
    private final ChatRoomRepository      chatRoomRepository;
    private final ChatMessageRepository   chatMessageRepository;
    private final NotificationService     notificationService;
    private final ObjectMapper            objectMapper;
    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    // ── 1. 상담사 → 접수면접지 요청 발송 ──────────────────────────────

    @Transactional
    public IntakeFormDto.RequestResponse requestIntakeForm(Long counselorId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getCounselor().getId().equals(counselorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!booking.isAccepted()) {
            throw new BusinessException(ErrorCode.INTAKE_FORM_BOOKING_NOT_ACCEPTED);
        }

        ChatRoom chatRoom = chatRoomRepository.findByBookingId(bookingId)
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.create(booking.getClient(), booking.getCounselor(), bookingId)));

        String intakeFormUrl = frontendOrigin + "/intake-form/" + bookingId;
        chatMessageRepository.save(ChatMessage.create(
                chatRoom,
                booking.getCounselor(),
                "접수면접지 작성을 요청드립니다. " + intakeFormUrl,
                ChatMessage.MessageType.TEXT));

        notificationService.notifyIntakeFormRequested(
                booking.getClient(), bookingId, booking.getCounselor().getName());

        log.info("IntakeForm requested: bookingId={}, counselorId={}", bookingId, counselorId);
        return IntakeFormDto.RequestResponse.builder()
                .requestedAt(LocalDateTime.now())
                .chatRoomId(chatRoom.getId())
                .build();
    }

    // ── 2. 내담자 → 접수면접지 작성 및 제출 ──────────────────────────

    @Transactional
    public IntakeFormDto.IntakeFormResponse submitIntakeForm(Long clientId, Long bookingId,
                                                              IntakeFormDto.SubmitRequest req) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!booking.isAccepted()) {
            throw new BusinessException(ErrorCode.INTAKE_FORM_BOOKING_NOT_ACCEPTED);
        }
        checkDeadline(booking);

        if (intakeFormRepository.existsByBookingId(bookingId)) {
            throw new BusinessException(ErrorCode.INTAKE_FORM_ALREADY_EXISTS);
        }

        User client = booking.getClient();
        IntakeFormDto.FormData formData = buildFormData(client, booking.getCounselor().getName(), req);

        IntakeForm intakeForm = IntakeForm.create(booking, serialize(formData));
        intakeFormRepository.save(intakeForm);

        notificationService.notifyIntakeFormSubmitted(
                booking.getCounselor(), intakeForm.getId(), client.getName());

        log.info("IntakeForm submitted: bookingId={}, clientId={}", bookingId, clientId);
        return IntakeFormDto.IntakeFormResponse.of(intakeForm, formData);
    }

    // ── 3. 내담자 → 접수면접지 수정 ──────────────────────────────────

    @Transactional
    public IntakeFormDto.IntakeFormResponse updateIntakeForm(Long clientId, Long bookingId,
                                                              IntakeFormDto.UpdateRequest req) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        checkDeadline(booking);

        IntakeForm intakeForm = intakeFormRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTAKE_FORM_NOT_FOUND));

        IntakeFormDto.FormData formData = deserialize(intakeForm.getFormData());
        formData.applyUpdate(req);
        intakeForm.updateFormData(serialize(formData));

        log.info("IntakeForm updated: bookingId={}, clientId={}", bookingId, clientId);
        return IntakeFormDto.IntakeFormResponse.of(intakeForm, formData);
    }

    // ── 4. 내담자 → 내 접수면접지 조회 ──────────────────────────────

    @Transactional(readOnly = true)
    public IntakeFormDto.IntakeFormResponse getMyIntakeForm(Long clientId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        IntakeForm intakeForm = intakeFormRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTAKE_FORM_NOT_FOUND));

        return IntakeFormDto.IntakeFormResponse.of(intakeForm, deserialize(intakeForm.getFormData()));
    }

    // ── 5. 상담사 → 내담자 접수면접지 열람 ──────────────────────────

    @Transactional(readOnly = true)
    public IntakeFormDto.IntakeFormResponse getCounselorIntakeForm(Long counselorId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getCounselor().getId().equals(counselorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        IntakeForm intakeForm = intakeFormRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTAKE_FORM_NOT_FOUND));

        return IntakeFormDto.IntakeFormResponse.of(intakeForm, deserialize(intakeForm.getFormData()));
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void checkDeadline(Booking booking) {
        LocalDateTime deadline = booking.getBookedDate().atTime(23, 59, 59);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(ErrorCode.INTAKE_FORM_DEADLINE_PASSED);
        }
    }

    private IntakeFormDto.FormData buildFormData(User client, String counselorName,
                                                  IntakeFormDto.SubmitRequest req) {
        return IntakeFormDto.FormData.builder()
                .name(client.getName())
                .gender(client.getGender() != null ? client.getGender().name() : "")
                .birthDate(client.getBirthDate() != null ? client.getBirthDate().toString() : "")
                .counselorName(counselorName)
                .counselingRoute(nvl(req.getCounselingRoute()))
                .mainProblem(nvl(req.getMainProblem()))
                .onsetPeriod(nvl(req.getOnsetPeriod()))
                .previousCounseling(nvl(req.getPreviousCounseling()))
                .developmentPregnancy(nvl(req.getDevelopmentPregnancy()))
                .developmentGrowth(nvl(req.getDevelopmentGrowth()))
                .familyRelationship(nvl(req.getFamilyRelationship()))
                .interpersonalRelationship(nvl(req.getInterpersonalRelationship()))
                .socialAdaptation(nvl(req.getSocialAdaptation()))
                .stressCoping(nvl(req.getStressCoping()))
                .socialPsychologicalSupport(nvl(req.getSocialPsychologicalSupport()))
                .medicalHistory(nvl(req.getMedicalHistory()))
                .counselingExpectation(nvl(req.getCounselingExpectation()))
                .testConducted(nvl(req.getTestConducted()))
                .notes(nvl(req.getNotes()))
                .build();
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private String serialize(IntakeFormDto.FormData formData) {
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private IntakeFormDto.FormData deserialize(String json) {
        try {
            return objectMapper.readValue(json, IntakeFormDto.FormData.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
