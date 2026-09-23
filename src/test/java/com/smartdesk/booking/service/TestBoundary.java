package com.smartdesk.booking.service;
import java.time.ZonedDateTime;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
public class TestBoundary {
    public static void main(String[] args) {
        try (MockedStatic<ZonedDateTime> mockedZDT = Mockito.mockStatic(ZonedDateTime.class)) {
            System.out.println("MockedStatic works!");
        }
    }
}
