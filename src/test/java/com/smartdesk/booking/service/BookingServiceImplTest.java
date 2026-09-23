package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.BookingRequest;
import com.smartdesk.booking.dto.BookingResponse;
import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.DeskType;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.entity.Floor;
import com.smartdesk.booking.entity.TimeWindow;
import com.smartdesk.booking.entity.Zone;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DeskRepository deskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Desk hotDesk;
    private Desk fixedDesk;
    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "cutoffTimeStr", "10:00");
        employee1 = new Employee();
        employee1.setId(1L);
        employee1.setName("Alice");

        employee2 = new Employee();
        employee2.setId(2L);
        employee2.setName("Bob");
        
        Floor floor = new Floor();
        floor.setId(100L);
        
        Zone zone = new Zone();
        zone.setId(200L);
        zone.setFloor(floor);

        hotDesk = new Desk();
        hotDesk.setId(10L);
        hotDesk.setDeskType(DeskType.HOT);
        hotDesk.setZone(zone);

        fixedDesk = new Desk();
        fixedDesk.setId(20L);
        fixedDesk.setDeskType(DeskType.FIXED);
        fixedDesk.setAssignedEmployee(employee1);
        fixedDesk.setZone(zone);
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
        doNothing().when(quotaService).checkQuotaForBooking(anyLong(), anyLong(), any(), any());

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
        doNothing().when(quotaService).checkQuotaForBooking(anyLong(), anyLong(), any(), any());

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
        doNothing().when(quotaService).checkQuotaForBooking(anyLong(), anyLong(), any(), any());
        
        when(bookingRepository.save(any(Booking.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(request, 1L);
        });
        assertEquals("Booking failed due to a conflict. The desk might have just been booked.", exception.getMessage());
    }

    @Test
    void testCheckIn_Success() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee1);
        booking.setDesk(hotDesk);
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setTimeWindow(TimeWindow.FULL_DAY);
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.checkIn(100L, 1L);

        assertNotNull(response.getCheckInTime());
        assertEquals(BookingStatus.BOOKED, response.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void testCheckIn_WrongEmployee_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee2);
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(BookingStatus.BOOKED);
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.checkIn(100L, 1L);
        });
        
        assertEquals("You can only check in to your own bookings", exception.getMessage());
    }

    @Test
    void testCheckIn_WrongDate_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee1);
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setStatus(BookingStatus.BOOKED);
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.checkIn(100L, 1L);
        });
        
        assertEquals("You can only check in on the day of the booking in your office's local time (UTC)", exception.getMessage());
    }

    @Test
    void testCancelBooking_BeforeCutoff_Success() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee1);
        
        // Use tomorrow to ensure it's before cutoff
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate tomorrow = ZonedDateTime.now(zoneId).toLocalDate().plusDays(1);
        booking.setBookingDate(tomorrow);
        booking.setStatus(BookingStatus.BOOKED);
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking(100L, 1L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void testCancelBooking_AfterCutoff_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee1);
        
        ZoneId zoneId = ZoneId.of("UTC");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        
        // Ensure cutoff time is set to a time before now (e.g. 00:01) to simulate being past the cutoff
        ReflectionTestUtils.setField(bookingService, "cutoffTimeStr", "00:01");
        
        booking.setBookingDate(now.toLocalDate());
        booking.setStatus(BookingStatus.BOOKED);
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.cancelBooking(100L, 1L);
        });
        
        assertEquals("Cannot cancel a booking after the 00:01 cutoff time in UTC", exception.getMessage());
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    void testCancelBooking_PastDate_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee1);
        
        ZoneId zoneId = ZoneId.of("UTC");
        LocalDate yesterday = ZonedDateTime.now(zoneId).toLocalDate().minusDays(1);
        
        booking.setBookingDate(yesterday);
        booking.setStatus(BookingStatus.BOOKED);
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.cancelBooking(100L, 1L);
        });
        
        assertEquals("Cannot cancel a booking from a past date", exception.getMessage());
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    void testCancelBooking_ExactCutoffBoundary_ThrowsException() {
        Booking booking = new Booking();
        booking.setId(100L);
        booking.setEmployee(employee1);
        
        ReflectionTestUtils.setField(bookingService, "cutoffTimeStr", "10:00");
        
        Floor floor = new Floor();
        floor.setTimezone("UTC");
        Zone zone = new Zone();
        zone.setFloor(floor);
        Desk mockDesk = new Desk();
        mockDesk.setZone(zone);
        booking.setDesk(mockDesk);

        ZonedDateTime exactCutoffTime = ZonedDateTime.parse("2026-10-15T10:00:00Z[UTC]");
        booking.setBookingDate(exactCutoffTime.toLocalDate());
        booking.setStatus(BookingStatus.BOOKED);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        try (MockedStatic<ZonedDateTime> mockedZDT = mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)) {
            mockedZDT.when(() -> ZonedDateTime.now(ZoneId.of("UTC"))).thenReturn(exactCutoffTime);
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                bookingService.cancelBooking(100L, 1L);
            });
            
            assertEquals("Cannot cancel a booking after the 10:00 cutoff time in UTC", exception.getMessage());
            verify(bookingRepository, never()).save(booking);
        }
    }
}
