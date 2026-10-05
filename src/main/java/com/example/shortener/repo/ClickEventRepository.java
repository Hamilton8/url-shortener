package com.example.shortener.repo;

import com.example.shortener.model.ClickEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    long countByUrlMappingId(Long urlMappingId);

    @Query("select c.clickedAt from ClickEvent c where c.urlMapping.id = :id")
    List<Instant> findClickTimes(@Param("id") Long id);
}
