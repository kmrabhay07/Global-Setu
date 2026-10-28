package com.globalsetu.platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="gs_account")
class Account {
    @Id public UUID id = UUID.randomUUID();
    @Column(unique=true, nullable=false) public String mobile;
    public String name;
    public String role = "CUSTOMER";
    public boolean active = true;
    public String consentVersion = "account-v1";
    public Instant consentAt = Instant.now();
    @JsonIgnore public String passwordHash;
    public Instant createdAt = Instant.now();
}
@Entity @Table(name="gs_session")
class LoginSession {
    @Id public String tokenHash;
    public UUID accountId;
    public Instant expiresAt;
}
@Entity @Table(name="gs_record", indexes={@Index(name="record_kind",columnList="kind"),@Index(name="record_owner",columnList="ownerId")})
class RecordEntity {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable=false) public String kind;
    @Column(nullable=false) public UUID ownerId;
    public String status;
    @Column(unique=true) public String uniqueKey;
    @Column(length=50000) public String payload = "{}";
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();
    @Version public long version;
}
@Entity @Table(name="gs_audit")
class AuditEvent {
    @Id public UUID id = UUID.randomUUID();
    public UUID actorId;
    public UUID recordId;
    public String action;
    public String kind;
    public String previousStatus;
    public String nextStatus;
    public Instant occurredAt = Instant.now();
}
interface Accounts extends JpaRepository<Account,UUID> { Optional<Account> findByMobile(String mobile); }
interface Sessions extends JpaRepository<LoginSession,String> { void deleteByAccountId(UUID accountId); }
interface Records extends JpaRepository<RecordEntity,UUID> {
    List<RecordEntity> findByKindOrderByCreatedAtDesc(String kind);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RecordEntity r where r.id=:id")
    Optional<RecordEntity> lock(@Param("id") UUID id);
}
interface Audits extends JpaRepository<AuditEvent,UUID> { List<AuditEvent> findTop500ByOrderByOccurredAtDesc(); List<AuditEvent> findByRecordIdOrderByOccurredAtDesc(UUID recordId); }
