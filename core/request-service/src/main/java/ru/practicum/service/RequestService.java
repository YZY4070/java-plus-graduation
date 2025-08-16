package ru.practicum.service;

import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    // Методы для работы с запросами на участие
    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    // Методы для организатора события
    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);
}