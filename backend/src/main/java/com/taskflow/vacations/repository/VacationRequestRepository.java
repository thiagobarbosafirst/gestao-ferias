package com.taskflow.vacations.repository;

import com.taskflow.vacations.model.VacationRequest;
import com.taskflow.vacations.model.VacationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Acesso a dados de pedidos de férias.
 */
public interface VacationRequestRepository
        extends JpaRepository<VacationRequest, Long>, JpaSpecificationExecutor<VacationRequest> {

    /**
     * Procura pedidos que se sobrepõem ao intervalo [start, end] (datas inclusivas).
     * <p>
     * Dois intervalos inclusivos A e B sobrepõem-se quando: A.start <= B.end E A.end >= B.start.
     * Ex.: 01/08→05/08 e 05/08→10/08 sobrepõem-se porque 01/08 <= 10/08 e 05/08 >= 05/08.
     *
     * @param excludeId id de um pedido a ignorar (o próprio pedido quando estamos a editá-lo);
     *                  usar -1 quando não há nada a excluir.
     */
    @Query("""
            SELECT v FROM VacationRequest v
            WHERE v.status IN :statuses
              AND v.startDate <= :endDate
              AND v.endDate >= :startDate
              AND v.id <> :excludeId
            ORDER BY v.startDate
            """)
    List<VacationRequest> findOverlapping(@Param("startDate") LocalDate start,
                                          @Param("endDate") LocalDate end,
                                          @Param("statuses") Collection<VacationStatus> statuses,
                                          @Param("excludeId") Long excludeId);

    /**
     * Listagem com filtros + paginação. O @EntityGraph carrega o utilizador e o decisor
     * na mesma query (evita o problema N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"user", "decidedBy"})
    Page<VacationRequest> findAll(Specification<VacationRequest> spec, Pageable pageable);

    /** Usado pelo calendário: carrega também o colaborador e o respetivo manager (para verificar permissões). */
    @Override
    @EntityGraph(attributePaths = {"user", "user.manager"})
    List<VacationRequest> findAll(Specification<VacationRequest> spec, Sort sort);

    void deleteByUserId(Long userId);
}
