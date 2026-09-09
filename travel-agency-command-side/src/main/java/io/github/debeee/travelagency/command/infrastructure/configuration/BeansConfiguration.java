package io.github.debeee.travelagency.command.infrastructure.configuration;

import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.service.HotelService;
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
    public CreateHotelUseCase transactionalCreateHotelUseCase(HotelService hotelService) {
        return new TransactionalCreateHotelUseCase(hotelService);
    }
}
