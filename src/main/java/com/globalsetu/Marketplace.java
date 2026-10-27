package com.globalsetu;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity class ServiceRequest {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @NotBlank @Size(max=100) public String customer;
 @NotBlank @Pattern(regexp="[0-9+ -]{10,16}") public String phone;
 @NotBlank @Size(max=100) public String category;
 @NotBlank @Size(max=200) public String location;
 @NotBlank @Size(max=2000) public String description;
 @Size(max=100) public String budget;
 public LocalDate preferredDate;
 @NotBlank public String priority;
 @AssertTrue public boolean consent;
 public String status = "SUBMITTED";
 public Instant createdAt = Instant.now();
 @Version public long version;
}
@Entity class Quote {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(nullable=false) public UUID requestId;
 @NotBlank @Size(max=100) public String provider;
 @NotNull @DecimalMin("0.01") public BigDecimal amount;
 @NotBlank @Size(max=1000) public String scope;
 @NotBlank @Size(max=100) public String timeline;
 @Size(max=1000) public String exclusions;
 @NotNull public LocalDate validUntil;
 public String status="OFFERED";
 public Instant decidedAt;
 @Version public long version;
}
interface RequestRepository extends JpaRepository<ServiceRequest, UUID> { List<ServiceRequest> findAllByOrderByCreatedAtDesc(); }
interface QuoteRepository extends JpaRepository<Quote, UUID> { List<Quote> findByRequestId(UUID id); }
@RestController @RequestMapping("/api")
class Marketplace {
 private final RequestRepository requests; private final QuoteRepository quotes;
 Marketplace(RequestRepository requests, QuoteRepository quotes) { this.requests=requests; this.quotes=quotes; }
 private ServiceRequest request(UUID id) { return requests.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Request not found")); }
 @GetMapping("/categories") List<String> categories(){return List.of("POP work","PVC wall panels","Ceiling work","Gypsum work","Interior services","Materials & delivery");}
 @GetMapping("/requests") List<ServiceRequest> all(){return requests.findAllByOrderByCreatedAtDesc();}
 @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED) ServiceRequest create(@Valid @RequestBody ServiceRequest r){r.id=null;r.status="SUBMITTED";r.createdAt=Instant.now();r.version=0;return requests.save(r);}
 @GetMapping("/requests/{id}/quotes") List<Quote> listQuotes(@PathVariable UUID id){request(id);return quotes.findByRequestId(id);}
 record StatusInput(@NotBlank String status) {}
 @PatchMapping("/requests/{id}/status") ServiceRequest status(@PathVariable UUID id,@Valid @RequestBody StatusInput input){var r=request(id);if(!List.of("UNDER_REVIEW","MORE_INFORMATION_REQUIRED","PROVIDER_SEARCH").contains(input.status()) || List.of("APPROVED","CANCELLED").contains(r.status)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Status change not allowed");r.status=input.status();return requests.save(r);}
 @PostMapping("/requests/{id}/quotes") @ResponseStatus(HttpStatus.CREATED) @Transactional Quote addQuote(@PathVariable UUID id,@Valid @RequestBody Quote q){var r=request(id);if(List.of("APPROVED","CANCELLED").contains(r.status)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Request is closed for quotations");if(q.validUntil.isBefore(LocalDate.now()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quotation validity must not be in the past");q.id=null;q.requestId=id;q.status="OFFERED";q.decidedAt=null;q.version=0;r.status="AWAITING_CUSTOMER_DECISION";requests.save(r);return quotes.save(q);}
 record Decision(@NotBlank String action) {}
 @PostMapping("/quotes/{id}/decision") @Transactional Quote decide(@PathVariable UUID id,@Valid @RequestBody Decision d){var q=quotes.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Quote not found"));var r=request(q.requestId);if(!q.status.equals("OFFERED") || !r.status.equals("AWAITING_CUSTOMER_DECISION"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Decision is no longer available");if(!List.of("ACCEPT","REJECT").contains(d.action()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported decision");if(d.action().equals("ACCEPT") && q.validUntil.isBefore(LocalDate.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Quotation has expired");q.status=d.action().equals("ACCEPT")?"ACCEPTED":"REJECTED";q.decidedAt=Instant.now();if(q.status.equals("ACCEPTED")){r.status="APPROVED";requests.save(r);}return quotes.save(q);}
}
