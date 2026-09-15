package br.com.dashboard.demand;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controla as requisições HTTP das demandas.
 */
@RestController
@RequestMapping("/demands")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    /**
     * Retorna todas as demandas.
     */
    @GetMapping
    public List<Demand> findAll() {
        return demandService.findAll();
    }

    /**
     * Retorna uma demanda pelo ID.
     */
    @GetMapping("/{id}")
    public Demand findById(@PathVariable Long id) {
        return demandService.findById(id);
    }

    /**
     * Busca demandas pelo identificador externo.
     */
    @GetMapping("/search")
    public List<Demand> searchByExternalId(
            @RequestParam String externalId
    ) {
        return demandService.searchByExternalId(externalId);
    }

    /**
     * Retorna a quantidade de dias de atraso.
     */
    @GetMapping("/{id}/days-overdue")
    public long calculateDaysOverdue(
            @PathVariable Long id
    ) {
        return demandService.calculateDaysOverdue(id);
    }

    /**
     * Retorna o total de demandas.
     */
    @GetMapping("/count")
    public long countAll() {
        return demandService.countAll();
    }

    /**
     * Retorna o total de demandas atrasadas.
     */
    @GetMapping("/count-overdue")
    public long countOverdue() {
        return demandService.countOverdue();
    }

    /**
     * Salva uma nova demanda.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Demand save(@RequestBody Demand demand) {
        return demandService.save(demand);
    }

    /**
     * Apaga todas as demandas da dashboard.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        demandService.deleteAll();
    }
}