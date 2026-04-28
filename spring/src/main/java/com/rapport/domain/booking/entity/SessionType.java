package com.rapport.domain.booking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "session_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // CHAT | CALL | VIDEOCALL | MEETING
}
