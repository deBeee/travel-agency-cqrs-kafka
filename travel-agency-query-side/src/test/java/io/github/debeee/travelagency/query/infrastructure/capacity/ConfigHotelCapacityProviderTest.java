package io.github.debeee.travelagency.query.infrastructure.capacity;

import io.github.debeee.travelagency.query.infrastructure.capacity.properties.HotelCapacityProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHotelCapacityProviderTest {

    private static final long HOTEL_ID = 7L;

    @Test
    void shouldReturnDefaultCapacityWhenNoOverridesAreConfigured() {
        // given
        ConfigHotelCapacityProvider provider = new ConfigHotelCapacityProvider(new HotelCapacityProperties(100, null));
        long expectedCapacity = 100;

        // when
        long capacity = provider.getCapacity(HOTEL_ID);

        // then
        assertThat(capacity).isEqualTo(expectedCapacity);
    }

    @Test
    void shouldReturnOverriddenCapacityWhenHotelHasOverride() {
        // given
        ConfigHotelCapacityProvider provider = new ConfigHotelCapacityProvider(
                new HotelCapacityProperties(100, Map.of(HOTEL_ID, 25L)));
        long expectedCapacity = 25;

        // when
        long capacity = provider.getCapacity(HOTEL_ID);

        // then
        assertThat(capacity).isEqualTo(expectedCapacity);
    }

    @Test
    void shouldReturnDefaultCapacityWhenOverridesExistForOtherHotelsOnly() {
        // given
        ConfigHotelCapacityProvider provider = new ConfigHotelCapacityProvider(
                new HotelCapacityProperties(100, Map.of(8L, 25L)));
        long expectedCapacity = 100;

        // when
        long capacity = provider.getCapacity(HOTEL_ID);

        // then
        assertThat(capacity).isEqualTo(expectedCapacity);
    }
}
