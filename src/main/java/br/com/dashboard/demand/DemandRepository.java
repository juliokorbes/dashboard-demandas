package br.com.dashboard.demand;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acessa os dados das demandas no banco.
 */
public interface DemandRepository extends JpaRepository<Demand, Long> {

    List<Demand> findByExternalIdContainingIgnoreCase(String externalId);

    /**
     * Busca uma demanda pelo identificador exato.
     */
    Optional<Demand> findByExternalIdIgnoreCase(String externalId);

    /**
     * Verifica se já existe uma demanda com o identificador.
     */
    boolean existsByExternalIdIgnoreCase(String externalId);
}