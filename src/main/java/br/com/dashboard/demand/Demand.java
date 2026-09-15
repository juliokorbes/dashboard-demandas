package br.com.dashboard.demand;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "demands")
@Getter
@Setter
@NoArgsConstructor
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String externalId;

    private String type;

    private String sector;

    private String responsible;

    private LocalDate entryDate;

    private LocalDate deadline;

    private String status;

    private String description;

    private String externalUrl;
}