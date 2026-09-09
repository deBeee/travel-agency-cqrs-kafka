package io.github.debeee.travelagency.command.infrastructure.configuration;

import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.service.BookingService;
import io.github.debeee.travelagency.command.application.service.HotelService;
import io.github.debeee.travelagency.command.infrastructure.tx.TransactionalCreateBookingUseCase;
import io.github.debeee.travelagency.command.infrastructure.tx.TransactionalCreateHotelUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfiguration {

    @Bean
    public HotelService hotelService(HotelRepository hotelRepository) {
        return new HotelService(hotelRepository);
    }

    @Bean
    BookingService bookingService(HotelRepository hotelRepository, BookingRepository bookingRepository) {
        return new BookingService(hotelRepository, bookingRepository);
    }

    @Bean
    public CreateHotelUseCase transactionalCreateHotelUseCase(HotelService hotelService) {
        return new TransactionalCreateHotelUseCase(hotelService);
    }

    @Bean
    public CreateBookingUseCase transactionalCreateBookingUseCase(BookingService bookingService) {
        return new TransactionalCreateBookingUseCase(bookingService);
    }
}
