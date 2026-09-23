package com.smartdesk.booking.repository;

import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.TimeWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    boolean existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
            Long deskId, LocalDate bookingDate, TimeWindow timeWindow, BookingStatus status);
            
    boolean existsByEmployeeIdAndBookingDateAndTimeWindowAndStatusNot(
            Long employeeId, LocalDate bookingDate, TimeWindow timeWindow, BookingStatus status);

    List<Booking> findByEmployeeIdOrderByBookingDateDesc(Long employeeId);

    List<Booking> findByEmployeeTeamIdAndBookingDateAndTimeWindowAndStatusNot(
            Long teamId, LocalDate bookingDate, TimeWindow timeWindow, BookingStatus status);

    long countByDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
            Long floorId, LocalDate bookingDate, TimeWindow timeWindow, BookingStatus status);

    long countByEmployeeTeamIdAndDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
            Long teamId, Long floorId, LocalDate bookingDate, TimeWindow timeWindow, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.bookingDate = :date AND b.status = 'BOOKED' AND b.checkInTime IS NULL AND b.desk.deskType = 'HOT'")
    List<Booking> findNoShowHotDeskBookings(@Param("date") LocalDate date);
}
