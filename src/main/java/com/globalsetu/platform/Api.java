package com.globalsetu.platform;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static com.globalsetu.platform.Platform.*;

@RestController @RequestMapping("/api")
class Api {
    private final Platform service; private final Accounts accounts; private final Audits audits; private final Records records;
    Api(Platform p,Accounts a,Audits au,Records r){service=p;accounts=a;audits=au;records=r;}
    Account actor(Authentication a){return (Account)a.getPrincipal();}
    @GetMapping("/catalog/{kind}") List<Map<String,Object>> catalog(@PathVariable String kind){
        require(Set.of("categories","products","profiles","reviews").contains(kind),"Invalid catalogue");
        return records.findByKindOrderByCreatedAtDesc(kind).stream().filter(r->Set.of("ACTIVE","VERIFIED","APPROVED").contains(r.status)).map(r->{var d=service.data(r);var safe=new LinkedHashMap<String,Object>();for(String key:List.of("name","type","city","skills","experience","availability","rate","description","brand","unit","price","stock","deliveryWindow","warranty","rating","comment","providerId"))if(d.containsKey(key))safe.put(key,d.get(key));return Map.<String,Object>of("id",r.id,"status",r.status,"data",safe,"updatedAt",r.updatedAt);}).toList();
    }
    @GetMapping("/records/{kind}") List<Map<String,Object>> list(Authentication a,@PathVariable String kind,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String city){return service.list(actor(a),kind).stream().filter(r->status.isBlank()||r.get("status").equals(status)).filter(r->q.isBlank()||r.get("data").toString().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))).filter(r->city.isBlank()||text((Map<String,Object>)r.get("data"),"city").equalsIgnoreCase(city)).toList();}
    @PostMapping("/records/{kind}") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> create(Authentication a,@PathVariable String kind,@RequestBody Map<String,Object> d){return service.view(service.create(actor(a),kind,d));}
    @PatchMapping("/records/{id}") Map<String,Object> update(Authentication a,@PathVariable UUID id,@RequestBody Map<String,Object> d){return service.view(service.update(actor(a),id,d));}
    @PostMapping("/records/{id}/actions/{action}") Map<String,Object> action(Authentication a,@PathVariable UUID id,@PathVariable String action,@RequestBody Map<String,Object> d){return service.view(service.action(actor(a),id,action,d));}
    @GetMapping("/dashboard") Map<String,Object> dashboard(Authentication a){var me=actor(a);var counts=new LinkedHashMap<String,Object>();for(String kind:KINDS)counts.put(kind,service.list(me,kind).stream().collect(Collectors.groupingBy(r->r.get("status"),Collectors.counting())));return Map.of("counts",counts,"generatedAt",Instant.now());}
    @GetMapping("/accounts") List<Account> accounts(Authentication a){permit(staff(actor(a)));return accounts.findAll();}
    record RoleInput(String role,boolean active){}
    @PatchMapping("/accounts/{id}") @Transactional Account role(Authentication auth,@PathVariable UUID id,@RequestBody RoleInput input){var actor=actor(auth);permit(actor.role.equals("SUPER_ADMIN"));permit(!actor.id.equals(id));require(Set.of("CUSTOMER","PROFESSIONAL","VENDOR","DELIVERY_PARTNER","OPERATIONS","SUPER_ADMIN").contains(input.role()),"Invalid role");var a=accounts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(Set.of("PROFESSIONAL","VENDOR","DELIVERY_PARTNER").contains(input.role()))require(service.verified(id),"Provider verification required");a.role=input.role();a.active=input.active();accounts.save(a);var e=new AuditEvent();e.actorId=actor.id;e.recordId=id;e.kind="accounts";e.action="ROLE_AND_ACCESS_UPDATE";e.nextStatus=input.role()+":"+input.active();audits.save(e);return a;}
    @GetMapping("/audit") List<AuditEvent> audit(Authentication a){permit(staff(actor(a)));return audits.findTop500ByOrderByOccurredAtDesc();}
    @GetMapping("/history/{id}") List<AuditEvent> history(Authentication a,@PathVariable UUID id){permit(service.canRead(actor(a),service.get(id)));return audits.findByRecordIdOrderByOccurredAtDesc(id);}
    @GetMapping("/reports/{kind}") ResponseEntity<byte[]> report(Authentication a,@PathVariable String kind,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String city,@RequestParam(defaultValue="") String from,@RequestParam(defaultValue="") String to,@RequestParam(defaultValue="csv") String format,@RequestParam(defaultValue="") String categoryId,@RequestParam(defaultValue="") String providerId){
        permit(staff(actor(a)));var rows=list(a,kind,"",status,city).stream().filter(r->categoryId.isBlank()||text((Map<String,Object>)r.get("data"),"categoryId").equals(categoryId)).filter(r->providerId.isBlank()||text((Map<String,Object>)r.get("data"),"providerId").equals(providerId)).filter(r->from.isBlank()||!((Instant)r.get("createdAt")).atZone(ZoneOffset.UTC).toLocalDate().isBefore(LocalDate.parse(from))).filter(r->to.isBlank()||!((Instant)r.get("createdAt")).atZone(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.parse(to))).toList();
        if(format.equals("xlsx"))return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header("Content-Disposition","attachment; filename="+kind+".xlsx").body(Workbook.create(rows));
        require(format.equals("csv"),"Unsupported export format");StringBuilder csv=new StringBuilder("id,status,ownerId,createdAt,details\r\n");for(var r:rows)csv.append(List.of(r.get("id"),r.get("status"),r.get("ownerId"),r.get("createdAt"),r.get("data")).stream().map(Api::csv).collect(Collectors.joining(","))).append("\r\n");return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).header("Content-Disposition","attachment; filename="+kind+".csv").body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    static String csv(Object value){String s=value.toString();if(s.stripLeading().matches("^[=+@-].*")||s.startsWith("\t")||s.startsWith("\r"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
@RestControllerAdvice class Errors {
    @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Invalid field value. Check dates, amounts and identifiers."));}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message","Check required fields and password length (12–72 characters)."));}
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> duplicate(){return ResponseEntity.status(409).body(Map.of("message","A conflicting record already exists."));}
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.toString(e.getReason(),"Request failed")));}
}
