package com.socialshuffle.repository;

import com.socialshuffle.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, String> {

    List<Registration> findByEventId(String eventId);

    List<Registration> findByParticipantId(String participantId);

    Optional<Registration> findByEventIdAndParticipantId(String eventId, String participantId);

    List<Registration> findByEventIdAndAttendanceStatus(String eventId, String attendanceStatus);

    long countByEventId(String eventId);

    long countByEventIdAndAttendanceStatus(String eventId, String attendanceStatus);

    Optional<Registration> findByQrCodeTokenIgnoreCase(String qrCodeToken);

    Optional<Registration> findByIdOrQrCodeTokenIgnoreCase(String id, String qrCodeToken);

    @Transactional
    @Modifying
    @Query("UPDATE Registration r SET r.participantId = :targetId WHERE r.participantId = :sourceId")
    int reassignParticipantRegistrations(@Param("sourceId") String sourceId, @Param("targetId") String targetId);

    @Transactional
    @Modifying
    @Query("UPDATE Registration r SET r.attendanceStatus = :status, r.checkInTime = :checkInTime WHERE r.id = :id")
    int updateAttendanceFast(@Param("id") String id, @Param("status") String status, @Param("checkInTime") String checkInTime);

    @Transactional
    @Modifying
    @Query("UPDATE Registration r SET r.paymentStatus = :status WHERE r.id = :id")
    int updatePaymentFast(@Param("id") String id, @Param("status") String status);

    @Transactional
    @Modifying
    @Query("DELETE FROM Registration r WHERE r.eventId IN ('ev-35', 'evt-35', '35') AND (r.participantId = 'p-priya' OR LOWER(r.participantName) LIKE '%priya nair%' OR LOWER(r.participantEmail) LIKE '%priya.nair%')")
    int deletePriyaEvent35();

    @Transactional
    @Modifying
    @Query("DELETE FROM Registration r WHERE r.participantId = :participantId")
    void deleteByParticipantId(@Param("participantId") String participantId);
}
