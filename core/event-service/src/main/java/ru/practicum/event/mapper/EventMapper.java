package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category.id", source = "category")
    @Mapping(target = "createdOn", expression  = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "confirmedRequests", constant = "0L")
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto newEventDto);

    @Mapping(target = "views", source = "views")
    EventFullDto toEventFullDto(Event event, Long views);

    @Mapping(target = "views", source = "views")
    EventShortDto toEventShortDto(Event event, Long views);

    @Mapping(target = "id", ignore = true)
    Location toLocation(LocationDto locationDto);
}