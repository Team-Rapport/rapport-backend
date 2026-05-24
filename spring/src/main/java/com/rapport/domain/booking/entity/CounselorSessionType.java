package com.rapport.domain.booking.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "counselor_session_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorSessionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @Column(nullable = false)
    private int price;

    public static CounselorSessionType of(User counselor, SessionType sessionType, int price) {
        CounselorSessionType c = new CounselorSessionType();
        c.counselor = counselor;
        c.sessionType = sessionType;
        c.price = price;
        return c;
    }

    public void updatePrice(int price) { this.price = price; }
}
