package br.com.dashboard.dashboard;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final DemandService demandService;

    @Value("${dashboard.due-soon-days:5}")
    private int dueSoonDays;

    @Value("${dashboard.completed-statuses:CONCLUIDA}")
    private String completedStatuses;

    public DashboardService(DemandService demandService) {
        this.demandService = demandService;
    }

    public DashboardSummary getSummary() {

        List<Demand> demands = demandService.findAll();

        LocalDate today = LocalDate.now();
        LocalDate dueSoonLimit = today.plusDays(dueSoonDays);

        long total = demands.size();

        long completed = demands.stream()
                .filter(this::isCompleted)
                .count();

        long overdue = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() != null)
                .filter(demand -> demand.getQualificationDate().isBefore(today))
                .count();

        long todayCount = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() != null)
                .filter(demand -> demand.getQualificationDate().isEqual(today))
                .count();

        long dueSoon = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() != null)
                .filter(demand -> demand.getQualificationDate().isAfter(today))
                .filter(demand -> !demand.getQualificationDate().isAfter(dueSoonLimit))
                .count();

        long onTime = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() != null)
                .filter(demand -> demand.getQualificationDate().isAfter(dueSoonLimit))
                .count();

        long noDeadline = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() == null)
                .count();

        return new DashboardSummary(
                total,
                overdue,
                todayCount,
                dueSoon,
                onTime,
                completed,
                noDeadline
        );
    }

    public List<CriticalDemand> getCriticalDemands() {

        LocalDate today = LocalDate.now();

        return demandService
                .findAll()
                .stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() != null)
                .filter(demand -> !demand.getQualificationDate().isAfter(today))
                .map(demand -> {

                    long daysOverdue = ChronoUnit.DAYS.between(
                            demand.getQualificationDate(),
                            today
                    );

                    return new CriticalDemand(
                            demand.getId(),
                            demand.getExternalId(),
                            demand.getType(),
                            demand.getSector(),
                            demand.getQualificationDate(),
                            daysOverdue,
                            demand.getStatus()
                    );
                })
                .sorted(
                        Comparator
                                .comparingLong(CriticalDemand::daysOverdue)
                                .reversed()
                )
                .toList();
    }

    public List<DelayRange> getDelayRanges() {

        LocalDate today = LocalDate.now();

        List<Demand> overdueDemands = demandService
                .findAll()
                .stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getQualificationDate() != null)
                .filter(demand -> demand.getQualificationDate().isBefore(today))
                .toList();

        long oneToFive = overdueDemands.stream()
                .filter(demand -> {
                    long days = ChronoUnit.DAYS.between(
                            demand.getQualificationDate(),
                            today
                    );

                    return days >= 1 && days <= 5;
                })
                .count();

        long sixToTen = overdueDemands.stream()
                .filter(demand -> {
                    long days = ChronoUnit.DAYS.between(
                            demand.getQualificationDate(),
                            today
                    );

                    return days >= 6 && days <= 10;
                })
                .count();

        long elevenToThirty = overdueDemands.stream()
                .filter(demand -> {
                    long days = ChronoUnit.DAYS.between(
                            demand.getQualificationDate(),
                            today
                    );

                    return days >= 11 && days <= 30;
                })
                .count();

        long moreThanThirty = overdueDemands.stream()
                .filter(demand -> {
                    long days = ChronoUnit.DAYS.between(
                            demand.getQualificationDate(),
                            today
                    );

                    return days > 30;
                })
                .count();

        return List.of(
                new DelayRange("1 a 5 dias", oneToFive),
                new DelayRange("6 a 10 dias", sixToTen),
                new DelayRange("11 a 30 dias", elevenToThirty),
                new DelayRange("Mais de 30 dias", moreThanThirty)
        );
    }

    public List<CategoryCount> getStatusDistribution() {

        return buildDistribution(
                demandService
                        .findAll()
                        .stream()
                        .map(Demand::getStatus)
                        .toList()
        );
    }

    public List<CategoryCount> getSectorDistribution() {

        return buildDistribution(
                demandService
                        .findAll()
                        .stream()
                        .map(Demand::getSector)
                        .toList()
        );
    }

    public List<CategoryCount> getTypeDistribution() {

        return buildDistribution(
                demandService
                        .findAll()
                        .stream()
                        .map(Demand::getType)
                        .toList()
        );
    }

    private List<CategoryCount> buildDistribution(List<String> values) {

        Map<String, Long> grouped = values.stream()
                .map(value ->
                        value == null || value.isBlank()
                                ? "NÃO INFORMADO"
                                : value
                )
                .collect(
                        Collectors.groupingBy(
                                value -> value,
                                Collectors.counting()
                        )
                );

        return grouped
                .entrySet()
                .stream()
                .map(entry ->
                        new CategoryCount(
                                entry.getKey(),
                                entry.getValue()
                        )
                )
                .sorted(
                        Comparator
                                .comparingLong(CategoryCount::count)
                                .reversed()
                                .thenComparing(CategoryCount::category)
                )
                .toList();
    }

    private boolean isCompleted(Demand demand) {

        if (demand == null || demand.getStatus() == null) {
            return false;
        }

        String normalizedStatus = normalizeText(
                demand.getStatus()
        );

        Set<String> completed = Arrays.stream(
                        completedStatuses.split(",")
                )
                .map(this::normalizeText)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        return completed.contains(normalizedStatus);
    }

    private String normalizeText(String value) {

        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value,
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}