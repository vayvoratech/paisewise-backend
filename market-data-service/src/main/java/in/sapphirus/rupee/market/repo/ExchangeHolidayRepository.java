package in.sapphirus.rupee.market.repo;

import in.sapphirus.rupee.market.domain.ExchangeHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeHolidayRepository extends JpaRepository<ExchangeHoliday, UUID> {
    List<ExchangeHoliday> findByExchange(String exchange);
    Optional<ExchangeHoliday> findByHolidayDateAndExchange(LocalDate holidayDate, String exchange);
}
