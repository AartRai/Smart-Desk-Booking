package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.BookingRequest;
import com.smartdesk.booking.dto.BookingResponse;
import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.DeskType;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.entity.TimeWindow;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.DeskRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DeskRepository deskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Desk hotDesk;
    private Desk fixedDesk;
    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        employee1 = new Employee();
        employee1.setId(1L);
        employee1.setName("Alice");

        employee2 = new Employee();
        employee2.setId(2L);
        employee2.setName("Bob");

        hotDesk = new Desk();
        hotDesk.setId(10L);
        hotDesk.setDeskType(DeskType.HOT);

        fixedDesk = new Desk();
        fixedDesk.setId(20L);
        fixedDesk.setDeskType(DeskType.FIXED);
        fixedDesk.setAssignedEmployee(employee1);
    }

    @Test
    void testCreateBooking_HotDesk_Success() {
        BookingRequest request = new BookingRequest();
        request.setDeskId(10L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setTimeWindow(TimeWindow.FULL_DAY);

        Booking savedBooking = new Booking();
        savedBooking.setId(100L);
        savedBooking.setDesk(hotDesk);
        savedBooking.setEmployee(employee1);
        savedBooking.setBookingDate(request.getBookingDate());
        savedBooking.setTimeWindow(request.getTimeWindow());
        savedBooking.setStatus(BookingStatus.BOOKED);

        when(deskRepository.findById(10L)).thenReturn(Optional.of(hotDesk));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
                10L, request.getBookingDate(), TimeWindow.FULL_DAY, BookingStatus.CANCELLED)).thenReturn(false);
        when(bookingRepository.existsByEmployeeIdAndBookingDateAndTimeWindowAndStatusNot(
                1L, request.getBookingDate(), TimeWindow.FULL_DAY, BookingStatus.CANCELLED)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request, 1L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(BookingStatus.BOOKED, response.getStatus());
    }

    @Test
    void testCreateBooking_FixedDesk_WrongOwner_ThrowsException() {
        BookingRequest request = new BookingRequest();
        request.setDeskId(20L); // Fixed desk assigned to employee1 (id=1)
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setTimeWindow(TimeWindow.FULL_DAY);

        when(deskRepository.findById(20L)).thenReturn(Optional.of(fixedDesk));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee2)); // Booking as employee2

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(request, 2L);
        });
        assertEquals("This fixed desk can only be booked by its assigned owner", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_DeskAlreadyBooked_ThrowsException() {
        BookingRequest request = new BookingRequest();
        request.setDeskId(10L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setTimeWindow(TimeWindow.FULL_DAY);

        when(deskRepository.findById(10L)).thenReturn(Optional.of(hotDesk));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
                10L, request.getBookingDate(), TimeWindow.FULL_DAY, BookingStatus.CANCELLED)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(request, 1L);
        });
        assertEquals("Desk is already booked for this time window", exception.getMessage());
    }

    @Test
    void testCreateBooking_ConcurrencyRaceCondition_ThrowsException() {
        BookingRequest request = new BookingRequest();
        request.setDeskId(10L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setTimeWindow(TimeWindow.FULL_DAY);

        when(deskRepository.findById(10L)).thenReturn(Optional.of(hotDesk));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.existsByEmployeeIdAndBookingDateAndTimeWindowAndStatusNot(any(), any(), any(), any())).thenReturn(false);
        
        when(bookingRepository.save(any(Booking.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(request, 1L);
        });
        assertEquals("Booking failed due to a conflict. The desk might have just been booked.", exception.getMessage());
    }
}
