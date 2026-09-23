package com.socialshuffle.repository;

import com.socialshuffle.model.SafetyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyReportRepository extends JpaRepository<SafetyReport, String> {

    List<SafetyReport> findByStatusIgnoreCase(String status);

    List<SafetyReport> findAllByOrderByCreatedAtDesc();
}
