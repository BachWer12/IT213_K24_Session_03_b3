package org.example.b3.dto;

public record BookingExtraction(
        String guestName,
        String checkInDate,
        int durationNights,
        String roomType
) {}