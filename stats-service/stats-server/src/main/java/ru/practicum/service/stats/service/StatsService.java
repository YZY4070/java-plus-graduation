package ru.practicum.service.stats.service;



import ru.practicum.dto.stats.StatsDtoRequest;
import ru.practicum.dto.stats.StatsDtoResponse;

import java.util.List;

public interface StatsService {

    void saveHit(StatsDtoRequest request);

    List<StatsDtoResponse> findStats(GetStatsRequest request);
}
