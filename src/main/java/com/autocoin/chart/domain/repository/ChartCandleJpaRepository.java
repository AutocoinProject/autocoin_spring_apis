package com.autocoin.chart.domain.repository;

import com.autocoin.chart.domain.entity.ChartCandle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 차트 캔들 데이터 JPA Repository
 */
@Repository
public interface ChartCandleJpaRepository extends JpaRepository<ChartCandle, Long> {

    /**
     * 특정 마켓의 최신 캔들 데이터 조회 (시간 순 정렬)
     */
    List<ChartCandle> findByMarketOrderByCandleTimeDesc(String market);

    /**
     * 특정 마켓의 최신 캔들 데이터 조회 (제한된 개수, 시간 순 정렬)
     */
    List<ChartCandle> findByMarketOrderByCandleTimeDesc(String market, Pageable pageable);

    /**
     * 특정 마켓과 시간의 캔들 데이터 존재 여부 확인
     */
    boolean existsByMarketAndCandleTime(String market, Long candleTime);

    /**
     * 특정 마켓과 시간의 캔들 데이터 조회
     */
    Optional<ChartCandle> findByMarketAndCandleTime(String market, Long candleTime);

    /**
     * 특정 마켓의 캔들 데이터 개수 조회
     */
    long countByMarket(String market);

    /**
     * 특정 마켓의 최신 캔들 데이터 하나 조회
     */
    Optional<ChartCandle> findFirstByMarketOrderByCandleTimeDesc(String market);

    /**
     * 특정 마켓과 시간 목록에 해당하는 캔들 데이터 조회 (배치 최적화)
     */
    List<ChartCandle> findByMarketAndCandleTimeIn(String market, Set<Long> candleTimes);
}
