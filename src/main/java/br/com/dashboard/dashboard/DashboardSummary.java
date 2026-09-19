package br.com.dashboard.dashboard;

/**
 * Representa os indicadores principais da dashboard.
 */
public record DashboardSummary(
        long total,
        long overdue,
        long today,
        long dueSoon,
        long onTime,
        long exitedQueue,
        long noDeadline
) {
}