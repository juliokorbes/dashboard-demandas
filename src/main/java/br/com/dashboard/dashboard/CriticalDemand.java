package br.com.dashboard.dashboard;

import java.time.LocalDate;

/**
 * Representa uma demanda crítica da dashboard.
 */
public record CriticalDemand(
        Long id,
        String externalId,
        String type,
        String sector,
        LocalDate deadline,
        long daysOverdue,
        String status
) {
}