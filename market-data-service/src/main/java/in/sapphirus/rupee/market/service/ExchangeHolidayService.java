package in.sapphirus.rupee.market.service;

import in.sapphirus.rupee.market.domain.ExchangeHoliday;
import in.sapphirus.rupee.market.repo.ExchangeHolidayRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.Optional;

@Service
public class ExchangeHolidayService {
    private final ExchangeHolidayRepository repository;

    public ExchangeHolidayService(ExchangeHolidayRepository repository) {
        this.repository = repository;
    }

    public boolean isMarketOpen(Instant instant) {
        // Convert UTC to IST
        ZonedDateTime ist = instant.atZone(ZoneId.of("Asia/Kolkata"));
        
        // Weekend check
        DayOfWeek day = ist.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        // Holiday calendar check
        LocalDate date = ist.toLocalDate();
        Optional<ExchangeHoliday> holidayOpt = repository.findByHolidayDateAndExchange(date, "NSE");
        
        if (holidayOpt.isPresent()) {
            ExchangeHoliday holiday = holidayOpt.get();
            // If it is a holiday and not a trading day, market is closed
            if (!holiday.getIsTradingDay()) {
                return false;
            }
            // Muhurat Trading Edge Case: If it is a trading day on a holiday, enforce custom start/end times
            if (holiday.getTradingStart() != null && holiday.getTradingEnd() != null) {
                LocalTime time = ist.toLocalTime();
                return !time.isBefore(holiday.getTradingStart()) && !time.isAfter(holiday.getTradingEnd());
            }
        }

        // Standard Active hours check (09:15 to 15:30)
        LocalTime time = ist.toLocalTime();
        return !time.isBefore(LocalTime.of(9, 15)) && !time.isAfter(LocalTime.of(15, 30));
    }
}
