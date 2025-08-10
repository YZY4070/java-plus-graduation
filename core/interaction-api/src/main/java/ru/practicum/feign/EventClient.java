package ru.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.event.EventFullDto;

@FeignClient(name = "event-service", path = "/events/feign")
public interface EventClient {
    @GetMapping("/{eventId}")
    EventFullDto getEventByIdFeign(@PathVariable Long eventId);

    @GetMapping("/{userId}/{eventId}")
    EventFullDto getEventByUserFeign(@PathVariable Long userId, @PathVariable Long eventId);

    @PatchMapping("/{eventId}")
    void updateEventForRequests(@PathVariable Long eventId, @Valid @RequestBody EventFullDto eventFullDto);
}
