package io.github.debeee.travelagency.command.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OutboxEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false)
    private String topic;
    
    @Lob
    private String payload;
    
    private LocalDateTime createdAt;
    
    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    public void incrementRetryCount() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount += 1;
    }

    public boolean hasExceededRetryThreshold(int threshold) {
        return retryCount != null && retryCount >= threshold;
    }
}
