package com.example.shortener.repo;

import com.example.shortener.model.UrlMapping;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    List<UrlMapping> findAllByOrderByCreatedAtDesc();
}
