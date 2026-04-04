package com.wasup.car_rental_system.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationOverlapTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2027, 7, 1, 10, 0);

    private Reservation reservationStartingAt(LocalDateTime start, int days) {
        return Reservation.builder()
                .startDateTime(start)
                .numberOfDays(days)
                .build();
    }

    @Test
    void overlaps_whenRequestIsContainedWithinExisting() {
        Reservation existing = reservationStartingAt(BASE, 5);
        assertTrue(existing.overlaps(BASE.plusDays(1), BASE.plusDays(3)));
    }

    @Test
    void overlaps_whenExistingIsContainedWithinRequest() {
        Reservation existing = reservationStartingAt(BASE.plusDays(1), 2);
        assertTrue(existing.overlaps(BASE, BASE.plusDays(10)));
    }

    @Test
    void overlaps_whenRequestStartsBeforeAndEndsInsideExisting() {
        Reservation existing = reservationStartingAt(BASE.plusDays(2), 3);
        assertTrue(existing.overlaps(BASE, BASE.plusDays(4)));
    }

    @Test
    void overlaps_whenRequestStartsInsideAndEndsAfterExisting() {
        Reservation existing = reservationStartingAt(BASE, 3);
        assertTrue(existing.overlaps(BASE.plusDays(2), BASE.plusDays(5)));
    }

    @Test
    void overlaps_whenRequestStartsExactlyWhenExistingEnds_noOverlap() {
        Reservation existing = reservationStartingAt(BASE, 3); // ends at BASE+3
        assertFalse(existing.overlaps(BASE.plusDays(3), BASE.plusDays(6)));
    }

    @Test
    void overlaps_whenRequestEndsExactlyWhenExistingStarts_noOverlap() {
        Reservation existing = reservationStartingAt(BASE.plusDays(3), 2);
        assertFalse(existing.overlaps(BASE, BASE.plusDays(3)));
    }

    @Test
    void overlaps_whenRequestIsCompletelyBeforeExisting() {
        Reservation existing = reservationStartingAt(BASE.plusDays(5), 3);
        assertFalse(existing.overlaps(BASE, BASE.plusDays(3)));
    }

    @Test
    void overlaps_whenRequestIsCompletelyAfterExisting() {
        Reservation existing = reservationStartingAt(BASE, 3);
        assertFalse(existing.overlaps(BASE.plusDays(5), BASE.plusDays(8)));
    }

    @Test
    void overlaps_whenReservationIsSingleDay() {
        Reservation existing = reservationStartingAt(BASE, 1); // occupies BASE to BASE+1
        assertTrue(existing.overlaps(BASE, BASE.plusDays(1)));
        assertFalse(existing.overlaps(BASE.plusDays(1), BASE.plusDays(2)));
        assertFalse(existing.overlaps(BASE.minusDays(1), BASE));
    }
}
