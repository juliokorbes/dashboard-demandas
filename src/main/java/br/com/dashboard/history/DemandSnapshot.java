package br.com.dashboard.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "demand_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_snapshot_date_external_id",
                        columnNames = {
                                "reference_date",
                                "external_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_snapshot_reference_date",
                        columnList = "reference_date"
                ),
                @Index(
                        name = "idx_snapshot_external_id",
                        columnList = "external_id"
                ),
                @Index(
                        name = "idx_snapshot_sector",
                        columnList = "sector"
                ),
                @Index(
                        name = "idx_snapshot_qualification_date",
                        columnList = "qualification_date"
                ),
                @Index(
                        name = "idx_snapshot_status",
                        columnList = "status"
                )
        }
)
public class DemandSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "reference_date",
            nullable = false
    )
    private LocalDate referenceDate;

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

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getProtocolOnr() {
        return protocolOnr;
    }

    public void setProtocolOnr(String protocolOnr) {
        this.protocolOnr = protocolOnr;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public LocalDate getQualificationDate() {
        return qualificationDate;
    }

    public void setQualificationDate(LocalDate qualificationDate) {
        this.qualificationDate = qualificationDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDate getReentryDate() {
        return reentryDate;
    }

    public void setReentryDate(LocalDate reentryDate) {
        this.reentryDate = reentryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}