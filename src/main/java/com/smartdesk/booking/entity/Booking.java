package com.smartdesk.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bookings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"desk_id", "booking_date", "time_window"}),
        @UniqueConstraint(columnNames = {"employee_id", "booking_date", "time_window"})
    }
)
@Getter
@Setter
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desk_id", nullable = false)
    private Desk desk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_window", nullable = false)
    private TimeWindow timeWindow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column
    private LocalDateTime checkInTime;
}
