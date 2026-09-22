package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.DeskRequest;
import com.smartdesk.booking.dto.DeskResponse;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.DeskType;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.entity.Zone;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.DeskRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.repository.ZoneRepository;
import com.smartdesk.booking.service.impl.DeskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeskServiceImplTest {

    @Mock
    private DeskRepository deskRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DeskServiceImpl deskService;

    private Zone zone;
    private Employee employee;

    @BeforeEach
    void setUp() {
        zone = new Zone();
        zone.setId(1L);
        zone.setName("Zone A");

        employee = new Employee();
        employee.setId(10L);
        employee.setName("John Doe");
    }

    @Test
    void testCreateHotDesk_Success() {
        // Arrange
        DeskRequest request = new DeskRequest();
        request.setDeskType(DeskType.HOT);
        request.setXCoordinate(10.5);
        request.setYCoordinate(20.5);
        request.setZoneId(1L);
        request.setAssignedEmployeeId(null);

        Desk savedDesk = new Desk();
        savedDesk.setId(100L);
        savedDesk.setDeskType(DeskType.HOT);
        savedDesk.setXCoordinate(10.5);
        savedDesk.setYCoordinate(20.5);
        savedDesk.setZone(zone);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(deskRepository.save(any(Desk.class))).thenReturn(savedDesk);

        // Act
        DeskResponse response = deskService.createDesk(request);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(DeskType.HOT, response.getDeskType());
        assertNull(response.getAssignedEmployeeId());

        verify(zoneRepository, times(1)).findById(1L);
        verify(deskRepository, times(1)).save(any(Desk.class));
    }

    @Test
    void testCreateFixedDesk_Success() {
        // Arrange
        DeskRequest request = new DeskRequest();
        request.setDeskType(DeskType.FIXED);
        request.setXCoordinate(15.0);
        request.setYCoordinate(25.0);
        request.setZoneId(1L);
        request.setAssignedEmployeeId(10L);

        Desk savedDesk = new Desk();
        savedDesk.setId(101L);
        savedDesk.setDeskType(DeskType.FIXED);
        savedDesk.setXCoordinate(15.0);
        savedDesk.setYCoordinate(25.0);
        savedDesk.setZone(zone);
        savedDesk.setAssignedEmployee(employee);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(deskRepository.save(any(Desk.class))).thenReturn(savedDesk);

        // Act
        DeskResponse response = deskService.createDesk(request);

        // Assert
        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(DeskType.FIXED, response.getDeskType());
        assertEquals(10L, response.getAssignedEmployeeId());

        verify(employeeRepository, times(1)).findById(10L);
    }

    @Test
    void testCreateDesk_ZoneNotFound_ThrowsException() {
        // Arrange
        DeskRequest request = new DeskRequest();
        request.setZoneId(99L); // Invalid Zone ID

        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            deskService.createDesk(request);
        });

        assertEquals("Zone not found with id: 99", exception.getMessage());
        verify(zoneRepository, times(1)).findById(99L);
        verify(deskRepository, never()).save(any(Desk.class));
    }

    @Test
    void testCreateFixedDesk_WithoutEmployee_ThrowsException() {
        // Arrange
        DeskRequest request = new DeskRequest();
        request.setDeskType(DeskType.FIXED);
        request.setZoneId(1L);
        request.setAssignedEmployeeId(null); // Missing assigned employee for FIXED desk

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            deskService.createDesk(request);
        });

        assertEquals("Fixed desk must have an assigned employee", exception.getMessage());
        verify(deskRepository, never()).save(any(Desk.class));
    }
}
