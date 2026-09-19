package br.com.dashboard.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "demand_snapshots_v2",
        indexes = {
                @Index(
                        name = "ux_snapshot_v2_datetime_external_id",
                        columnList = "reference_datetime, external_id",
                        unique = true
                ),
                @Index(
                        name = "idx_snapshot_v2_reference_datetime",
                        columnList = "reference_datetime"
                ),
                @Index(
                        name = "idx_snapshot_v2_external_id",
                        columnList = "external_id"
                ),
                @Index(
                        name = "idx_snapshot_v2_sector",
                        columnList = "sector"
                ),
                @Index(
                        name = "idx_snapshot_v2_qualification_date",
                        columnList = "qualification_date"
                ),
                @Index(
                        name = "idx_snapshot_v2_status",
                        columnList = "status"
                )
        }
)
public class DemandSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "reference_datetime",
            nullable = false
    )
    private LocalDateTime referenceDateTime;

    @Column(
            name = "external_id",
            nullable = false
    )
    private String externalId;

    @Column(name = "protocol_onr")
    private String protocolOnr;

    private String type;

    private String stage;

    private String sector;

    private String responsible;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "qualification_date")
    private LocalDate qualificationDate;

    private LocalDate deadline;

    @Column(name = "reentry_date")
    private LocalDate reentryDate;

    private String status;

    public DemandSnapshot() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getReferenceDateTime() {
        return referenceDateTime;
    }

    public void setReferenceDateTime(
            LocalDateTime referenceDateTime
    ) {
        this.referenceDateTime = referenceDateTime;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(
            String externalId
    ) {
        this.externalId = externalId;
    }

    public String getProtocolOnr() {
        return protocolOnr;
    }

    public void setProtocolOnr(
            String protocolOnr
    ) {
        this.protocolOnr = protocolOnr;
    }

    public String getType() {
        return type;
    }

    public void setType(
            String type
    ) {
        this.type = type;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(
            String stage
    ) {
        this.stage = stage;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(
            String sector
    ) {
        this.sector = sector;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(
            String responsible
    ) {
        this.responsible = responsible;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(
            LocalDate entryDate
    ) {
        this.entryDate = entryDate;
    }

    public LocalDate getQualificationDate() {
        return qualificationDate;
    }

    public void setQualificationDate(
            LocalDate qualificationDate
    ) {
        this.qualificationDate =
                qualificationDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(
            LocalDate deadline
    ) {
        this.deadline = deadline;
    }

    public LocalDate getReentryDate() {
        return reentryDate;
    }

    public void setReentryDate(
            LocalDate reentryDate
    ) {
        this.reentryDate = reentryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }
}