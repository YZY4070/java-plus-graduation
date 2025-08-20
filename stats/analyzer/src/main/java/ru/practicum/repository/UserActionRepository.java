package ru.practicum.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByEventIdInAndUserId(Set<Long> viewedEvents, Long userId);

    List<UserAction> findAllByUserId(Long userId, PageRequest pageRequest);

    List<UserAction> findAllByEventIdIn(Set<Long> eventIds);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}
