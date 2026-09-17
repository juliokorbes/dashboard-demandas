package br.com.dashboard.history;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/history")
public class HistoryController {

    private final DemandSnapshotService snapshotService;

    public HistoryController(
            DemandSnapshotService snapshotService
    ) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/dates")
    public List<LocalDateTime> getAvailableDateTimes() {
        return snapshotService.findAvailableDateTimes();
    }

    @GetMapping("/{dateTime}")
    public List<DemandSnapshot> getSnapshot(
            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime dateTime
    ) {
        return snapshotService.findByReferenceDateTime(
                dateTime
        );
    }

    @DeleteMapping("/{dateTime}")
    public void deleteSnapshot(
            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime dateTime
    ) {
        snapshotService.deleteByReferenceDateTime(
                dateTime
        );
    }
}