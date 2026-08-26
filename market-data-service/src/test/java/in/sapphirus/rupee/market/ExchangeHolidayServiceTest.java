package in.sapphirus.rupee.market;

import in.sapphirus.rupee.market.domain.ExchangeHoliday;
import in.sapphirus.rupee.market.repo.ExchangeHolidayRepository;
import in.sapphirus.rupee.market.service.ExchangeHolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeHolidayServiceTest {

    @Mock
    private ExchangeHolidayRepository repository;

    private ExchangeHolidayService service;

    @BeforeEach
    public void setUp() {
        service = new ExchangeHolidayService(repository);
    }

    @Test
    public void testIsMarketOpen_ActiveMarketHours() {
        // Monday Aug 24, 2026, 10:30 AM IST
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 24, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        when(repository.findByHolidayDateAndExchange(LocalDate.of(2026, 8, 24), "NSE"))
                .thenReturn(Optional.empty());

        assertTrue(service.isMarketOpen(instant));
    }

    @Test
    public void testIsMarketOpen_Weekends() {
        // Saturday Aug 22, 2026, 10:30 AM IST
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 22, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        // Should return false immediately without querying repo
        assertFalse(service.isMarketOpen(instant));
        verify(repository, never()).findByHolidayDateAndExchange(any(), any());
    }

    @Test
    public void testIsMarketOpen_PreMarketClosed() {
        // Monday Aug 24, 2026, 9:00 AM IST
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 24, 9, 0, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        when(repository.findByHolidayDateAndExchange(LocalDate.of(2026, 8, 24), "NSE"))
                .thenReturn(Optional.empty());

        assertFalse(service.isMarketOpen(instant));
    }

    @Test
    public void testIsMarketOpen_PostMarketClosed() {
        // Monday Aug 24, 2026, 3:45 PM IST
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 24, 15, 45, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        when(repository.findByHolidayDateAndExchange(LocalDate.of(2026, 8, 24), "NSE"))
                .thenReturn(Optional.empty());

        assertFalse(service.isMarketOpen(instant));
    }

    @Test
    public void testIsMarketOpen_OfficialHoliday() {
        // Monday Aug 24, 2026, 10:30 AM IST (mocked holiday)
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 24, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        ExchangeHoliday holiday = new ExchangeHoliday(
                LocalDate.of(2026, 8, 24),
                "NSE",
                "Mock Holiday",
                "NATIONAL",
                false, // not a trading day
                null,
                null,
                "Description"
        );

        when(repository.findByHolidayDateAndExchange(LocalDate.of(2026, 8, 24), "NSE"))
                .thenReturn(Optional.of(holiday));

        assertFalse(service.isMarketOpen(instant));
    }

    @Test
    public void testIsMarketOpen_MuhuratTrading_Open() {
        // Monday Aug 24, 2026, 6:15 PM IST (custom trading hours on holiday)
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 24, 18, 15, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        ExchangeHoliday holiday = new ExchangeHoliday(
                LocalDate.of(2026, 8, 24),
                "NSE",
                "Diwali Muhurat",
                "SPECIAL",
                true, // it IS a trading day
                LocalTime.of(18, 0), // starts at 6:00 PM
                LocalTime.of(19, 0), // ends at 7:00 PM
                "Diwali Muhurat Trading"
        );

        when(repository.findByHolidayDateAndExchange(LocalDate.of(2026, 8, 24), "NSE"))
                .thenReturn(Optional.of(holiday));

        assertTrue(service.isMarketOpen(instant));
    }

    @Test
    public void testIsMarketOpen_MuhuratTrading_Closed() {
        // Monday Aug 24, 2026, 10:30 AM IST (standard hours, but this is a special day holiday)
        ZonedDateTime zdt = ZonedDateTime.of(2026, 8, 24, 10, 30, 0, 0, ZoneId.of("Asia/Kolkata"));
        Instant instant = zdt.toInstant();

        ExchangeHoliday holiday = new ExchangeHoliday(
                LocalDate.of(2026, 8, 24),
                "NSE",
                "Diwali Muhurat",
                "SPECIAL",
                true, // it IS a trading day
                LocalTime.of(18, 0), // starts at 6:00 PM
                LocalTime.of(19, 0), // ends at 7:00 PM
                "Diwali Muhurat Trading"
        );

        when(repository.findByHolidayDateAndExchange(LocalDate.of(2026, 8, 24), "NSE"))
                .thenReturn(Optional.of(holiday));

        assertFalse(service.isMarketOpen(instant));
    }
}
