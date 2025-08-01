package ru.practicum.compilation;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.Event;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "compilations")
@Getter
@Setter
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 50)
    String title;

    @Column(nullable = false)
    Boolean pinned;

    @ManyToMany
    @JoinTable(
            name = "events_compilations",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private Set<Event> events = new HashSet<>();

}
