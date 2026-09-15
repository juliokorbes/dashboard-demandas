package br.com.dashboard.dashboard;

/**
 * Representa uma faixa de atraso da dashboard.
 */
public record DelayRange(
        String range,
        long count
) {
}