package ru.practicum.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;
import ru.practicum.model.ActionType;
import ru.practicum.model.EventSim;
import ru.practicum.model.UserAction;
import ru.practicum.repository.SimRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationHandler {
    final UserActionRepository userActionRepository;
    final SimRepository similarityRepository;

    Double viewAction = 0.4;
    Double registerAction = 0.8;
    Double likeAction = 1.0;

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Long userId = request.getUserId();
        int limit = request.getMaxResults();

        // последние просмотренные события пользователя
        Set<Long> recentlyViewed = fetchRecentViewedEventIds(userId, limit);
        if (recentlyViewed.isEmpty()) {
            return Collections.emptyList();
        }

        // похожие события (A->B и B->A), которые пользователь ещё не видел
        Set<Long> candidates = findCandidateRecommendations(userId, recentlyViewed, limit);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        return scoreAndBuildTopRecommendations(candidates, userId, limit);
    }

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int max = request.getMaxResults();

        PageRequest pr = PageRequest.of(0, max, Sort.by(Sort.Direction.DESC, "score"));
        List<EventSim> aList = similarityRepository.findAllByEventA(eventId, pr);
        List<EventSim> bList = similarityRepository.findAllByEventB(eventId, pr);

        List<RecommendedEventProto> result = new ArrayList<>();
        addIfNotSeen(result, aList, true, userId);
        addIfNotSeen(result, bList, false, userId);

        result.sort(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed());
        return result.size() > max ? result.subList(0, max) : result;
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        Set<Long> eventIds = new HashSet<>(request.getEventIdList());
        Map<Long, Double> scores = new HashMap<>();

        userActionRepository.findAllByEventIdIn(eventIds).forEach(action ->
                scores.merge(action.getEventId(), toWeight(action.getActionType()), Double::sum)
        );

        return scores.entrySet().stream()
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build())
                .toList();
    }

    private Set<Long> fetchRecentViewedEventIds(Long userId, int limit) {
        PageRequest pr = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return userActionRepository.findAllByUserId(userId, pr).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> findCandidateRecommendations(Long userId, Set<Long> viewedEventIds, int limit) {
        PageRequest pr = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score"));

        List<EventSim> simA = similarityRepository.findAllByEventAIn(viewedEventIds, pr);
        List<EventSim> simB = similarityRepository.findAllByEventBIn(viewedEventIds, pr);

        return Stream.concat(
                        simA.stream().map(EventSim::getEventB),
                        simB.stream().map(EventSim::getEventA))
                .filter(Objects::nonNull)
                .filter(candidateId -> !userActionRepository.existsByEventIdAndUserId(candidateId, userId))
                .collect(Collectors.toCollection(LinkedHashSet::new)); // сохранить порядок вставки
    }

    private List<RecommendedEventProto> scoreAndBuildTopRecommendations(Set<Long> candidates,
                                                                        Long userId,
                                                                        int limit) {
        Map<Long, Double> scored = candidates.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        candidate -> calculateRecommendationScore(candidate, userId, limit)
                ));

        return scored.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> buildRecommendation(e.getKey(), e.getValue()))
                .toList();
    }

    private Double calculateRecommendationScore(Long candidateEventId, Long userId, int neighborsLimit) {
        PageRequest pr = PageRequest.of(0, neighborsLimit, Sort.by(Sort.Direction.DESC, "score"));

        List<EventSim> simA = similarityRepository.findAllByEventA(candidateEventId, pr);
        List<EventSim> simB = similarityRepository.findAllByEventB(candidateEventId, pr);

        // соберём map viewedEventId -> similarityScore (только те события, которые пользователь видел)
        Map<Long, Double> viewedSimilarityScores = new HashMap<>();
        collectIfUserSeen(simA, true, userId, viewedSimilarityScores);
        collectIfUserSeen(simB, false, userId, viewedSimilarityScores);

        if (viewedSimilarityScores.isEmpty()) {
            return 0.0;
        }

        Map<Long, Double> userWeights = userActionRepository
                .findAllByEventIdInAndUserId(viewedSimilarityScores.keySet(), userId).stream()
                .collect(Collectors.toMap(UserAction::getEventId, ua -> toWeight(ua.getActionType())));

        double sumWeighted = 0.0;
        double sumSim = 0.0;
        for (Map.Entry<Long, Double> e : viewedSimilarityScores.entrySet()) {
            Double userW = userWeights.get(e.getKey());
            if (userW != null) {
                sumWeighted += userW * e.getValue();
                sumSim += e.getValue();
            }
        }

        return sumSim > 0 ? sumWeighted / sumSim : 0.0;
    }

    private void collectIfUserSeen(List<EventSim> similarities,
                                   boolean isEventB,
                                   Long userId,
                                   Map<Long, Double> out) {
        for (EventSim es : similarities) {
            Long related = isEventB ? es.getEventB() : es.getEventA();
            if (related != null && userActionRepository.existsByEventIdAndUserId(related, userId)) {
                out.put(related, es.getScore());
            }
        }
    }

    private void addIfNotSeen(List<RecommendedEventProto> target,
                              List<EventSim> sims,
                              boolean isEventB,
                              Long userId) {
        for (EventSim es : sims) {
            Long candidate = isEventB ? es.getEventB() : es.getEventA();
            if (candidate != null && !userActionRepository.existsByEventIdAndUserId(candidate, userId)) {
                target.add(RecommendedEventProto.newBuilder()
                        .setEventId(candidate)
                        .setScore(es.getScore())
                        .build());
            }
        }
    }

    private RecommendedEventProto buildRecommendation(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }

    private Double toWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> viewAction;
            case REGISTER -> registerAction;
            case LIKE -> likeAction;
        };
    }
}

