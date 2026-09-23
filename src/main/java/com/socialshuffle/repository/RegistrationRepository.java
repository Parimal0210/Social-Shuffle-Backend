package com.socialshuffle.repository;

import com.socialshuffle.model.Registration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends MongoRepository<Registration, String> {

    List<Registration> findByEventId(String eventId);

    List<Registration> findByParticipantId(String participantId);

    Optional<Registration> findByEventIdAndParticipantId(String eventId, String participantId);

    List<Registration> findByEventIdAndAttendanceStatus(String eventId, String attendanceStatus);

    long countByEventId(String eventId);

    long countByEventIdAndAttendanceStatus(String eventId, String attendanceStatus);

    void deleteByParticipantId(String participantId);
}
