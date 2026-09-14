package io.github.debeee.travelagency.query.application.port.in;

import io.github.debeee.travelagency.query.application.command.UpdateAvailabilityCommand;

public interface UpdateAvailabilityUseCase {
    void update(UpdateAvailabilityCommand command);
}
