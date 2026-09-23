package com.socialshuffle.repository;

import com.socialshuffle.model.SafetyReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyReportRepository extends MongoRepository<SafetyReport, String> {

    List<SafetyReport> findByStatusIgnoreCase(String status);

    List<SafetyReport> findAllByOrderByCreatedAtDesc();
}
