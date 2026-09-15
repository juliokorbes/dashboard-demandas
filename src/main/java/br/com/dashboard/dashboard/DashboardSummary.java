package br.com.dashboard.dashboard;

/**
 * Representa os indicadores principais da dashboard.
 */
public record DashboardSummary(
        long total,
        long overdue,
        long dueSoon,
        long onTime,
        long completed,
        long noDeadline
) {
}