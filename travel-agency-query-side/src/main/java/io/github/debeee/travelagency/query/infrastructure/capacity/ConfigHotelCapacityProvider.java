package io.github.debeee.travelagency.query.infrastructure.capacity;

import io.github.debeee.travelagency.query.application.port.out.HotelCapacityProvider;
import io.github.debeee.travelagency.query.infrastructure.capacity.properties.HotelCapacityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigHotelCapacityProvider implements HotelCapacityProvider {
    private final HotelCapacityProperties properties;

    @Override
    public long getCapacity(long hotelId) {
        if (properties.overrides() != null && properties.overrides().containsKey(hotelId)) {
            return properties.overrides().get(hotelId);
        }
        return properties.defaultCapacity();
    }
}
