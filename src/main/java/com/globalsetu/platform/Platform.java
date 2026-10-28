package com.globalsetu.platform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service @Transactional
class Platform {
    static final Set<String> KINDS=Set.of("categories","products","profiles","addresses","requests","quotes","bookings","deliveries","payments","refunds","complaints","reviews","notifications","notes","settings");
    static final Set<String> STAFF=Set.of("OPERATIONS","SUPER_ADMIN");
    private final Records records; private final Accounts accounts; private final Audits audits; private final ObjectMapper json; private final UploadRepository uploads;
    Platform(Records r,Accounts a,Audits au,ObjectMapper j,UploadRepository u){records=r;accounts=a;audits=au;json=j;uploads=u;}
    static void require(boolean ok,String message){if(!ok)throw new ResponseStatusException(HttpStatus.CONFLICT,message);}
    static void permit(boolean ok){if(!ok)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");}
    static boolean staff(Account a){return STAFF.contains(a.role);}
    static String text(Map<String,Object> d,String key){return Objects.toString(d.get(key),"").trim();}
    static void required(Map<String,Object> d,String... keys){for(String k:keys)require(!text(d,k).isBlank(),k+" is required");}
    static UUID uuid(Map<String,Object> d,String key){try{return UUID.fromString(text(d,key));}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid "+key);}}
    static BigDecimal money(Map<String,Object> d,String key){try{var n=new BigDecimal(text(d,key));require(n.signum()>=0 && n.precision()<14 && n.scale()<=2,key+" must be a nonnegative amount with at most two decimals");return n;}catch(NumberFormatException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid "+key);}}
    Map<String,Object> data(RecordEntity r){try{return json.readValue(r.payload,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
    void put(RecordEntity r,Map<String,Object> d){try{String p=json.writeValueAsString(d);require(p.length()<=45000,"Record is too large");r.payload=p;r.updatedAt=Instant.now();}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid data");}}
    Map<String,Object> view(RecordEntity r){return Map.of("id",r.id,"kind",r.kind,"ownerId",r.ownerId,"status",r.status,"data",data(r),"createdAt",r.createdAt,"updatedAt",r.updatedAt,"version",r.version);}
    RecordEntity get(UUID id){return records.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found"));}
    RecordEntity lock(UUID id){return records.lock(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found"));}
    RecordEntity linked(Map<String,Object> d,String key,String kind){RecordEntity r=get(uuid(d,key));require(r.kind.equals(kind),"Invalid "+key);return r;}
    boolean canRead(Account a,RecordEntity r){
        if(staff(a))return true;
        if(r.kind.equals("notes")||r.kind.equals("settings"))return false;
        if(r.ownerId.equals(a.id))return true;
        var d=data(r);
        if(r.kind.equals("categories")||r.kind.equals("products"))return r.status.equals("ACTIVE");
        if(r.kind.equals("profiles"))return r.status.equals("VERIFIED");
        if(r.kind.equals("reviews"))return r.status.equals("APPROVED");
        if(Set.of("requests","bookings","deliveries").contains(r.kind))return text(d,"providerId").equals(a.id.toString())||text(d,"partnerId").equals(a.id.toString());
        if(r.kind.equals("quotes"))return get(uuid(d,"requestId")).ownerId.equals(a.id);
        if(r.kind.equals("payments")||r.kind.equals("refunds")||r.kind.equals("complaints"))return get(uuid(d,"bookingId")).ownerId.equals(a.id);
        return false;
    }
    List<Map<String,Object>> list(Account a,String kind){require(KINDS.contains(kind),"Unknown module");return records.findByKindOrderByCreatedAtDesc(kind).stream().filter(r->canRead(a,r)).map(r->{var v=new LinkedHashMap<>(view(r));if(kind.equals("profiles")&&!staff(a)&&!r.ownerId.equals(a.id)){var d=new LinkedHashMap<>(data(r));d.remove("documents");d.remove("reviewNote");d.remove("phone");d.remove("legalDetails");v.put("data",d);}return (Map<String,Object>)v;}).toList();}
    void audit(Account a,RecordEntity r,String action,String before){var e=new AuditEvent();e.actorId=a.id;e.recordId=r.id;e.kind=r.kind;e.action=action;e.previousStatus=before;e.nextStatus=r.status;audits.save(e);}
    void notify(UUID owner,String title,String message,UUID target){var r=new RecordEntity();r.kind="notifications";r.ownerId=owner;r.status="UNREAD";put(r,Map.of("title",title,"message",message,"recordId",target));records.save(r);}
    boolean verified(UUID owner){return records.findByKindOrderByCreatedAtDesc("profiles").stream().anyMatch(p->p.ownerId.equals(owner)&&p.status.equals("VERIFIED"));}
    void availableProvider(UUID owner){var a=accounts.findById(owner).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Provider not found"));require(a.active&&Set.of("PROFESSIONAL","VENDOR").contains(a.role)&&verified(owner),"A verified active provider is required");}
    RecordEntity create(Account a,String kind,Map<String,Object> raw){
        require(KINDS.contains(kind),"Unknown module");var d=new LinkedHashMap<>(raw);var r=new RecordEntity();r.kind=kind;r.ownerId=a.id;r.status="SUBMITTED";
        switch(kind){
            case "categories" -> {permit(staff(a));required(d,"name","city");r.status="ACTIVE";}
            case "settings" -> {permit(a.role.equals("SUPER_ADMIN"));required(d,"key","value");r.status="ACTIVE";}
            case "addresses" -> {required(d,"label","address","city","postalCode");r.status="ACTIVE";}
            case "profiles" -> {
                permit(a.role.equals("CUSTOMER"));required(d,"name","type","city","skills","experience","availability","phone");
                require(Set.of("PROFESSIONAL","VENDOR","DELIVERY_PARTNER").contains(text(d,"type")),"Invalid profile type");
                require(records.findByKindOrderByCreatedAtDesc(kind).stream().noneMatch(p->p.ownerId.equals(a.id)),"Update your existing profile instead");
                d.remove("reviewNote");r.uniqueKey="profile:"+a.id;r.status="PENDING_VERIFICATION";
            }
            case "products" -> {permit(a.role.equals("VENDOR")||staff(a));require(staff(a)||verified(a.id),"Vendor must be verified");required(d,"name","brand","unit","city","stock","deliveryWindow","warranty","billingDetails");money(d,"price");money(d,"stock");r.status="ACTIVE";}
            case "requests" -> {
                permit(a.role.equals("CUSTOMER")||staff(a));required(d,"title","description","address","city","categoryId","quantity","unit","preferredDate","priority");
                var category=linked(d,"categoryId","categories");require(category.status.equals("ACTIVE"),"Category is unavailable");
                require(text(data(category),"city").equalsIgnoreCase(text(d,"city")),"Category does not serve this city");
                require(Boolean.TRUE.equals(d.get("consent")),"Contact consent is required");money(d,"budget");money(d,"quantity");
                require(!LocalDate.parse(text(d,"preferredDate")).isBefore(LocalDate.now()),"Choose a future date");
                d.remove("providerId");d.remove("approvedQuoteId");d.put("consentAt",Instant.now().toString());d.put("consentVersion","request-contact-v1");
            }
            case "quotes" -> {
                permit(staff(a)||Set.of("PROFESSIONAL","VENDOR").contains(a.role));
                var req=lock(uuid(d,"requestId"));require(req.kind.equals("requests"),"Invalid request");
                require(Set.of("PROVIDER_SEARCH","AWAITING_CUSTOMER_DECISION").contains(req.status),"Request is not open to quotes");
                UUID provider=staff(a)?uuid(d,"providerId"):a.id;availableProvider(provider);
                permit(staff(a)||text(data(req),"providerId").equals(a.id.toString()));
                d.put("providerId",provider.toString());r.ownerId=provider;
                required(d,"scope","timeline","validUntil","termsVersion","lineItems");
                require(!LocalDate.parse(text(d,"validUntil")).isBefore(LocalDate.now()),"Quote has expired");
                quoteTotal(d);d.remove("decidedAt");d.remove("decision");r.status="OFFERED";req.status="AWAITING_CUSTOMER_DECISION";records.save(req);notify(req.ownerId,"Quotation ready","Compare your options and record a decision.",req.id);
            }
            case "bookings" -> {
                permit(staff(a));var req=lock(uuid(d,"requestId"));require(req.kind.equals("requests")&&req.status.equals("APPROVED"),"Customer approval is required");
                require(records.findByKindOrderByCreatedAtDesc(kind).stream().noneMatch(b->text(data(b),"requestId").equals(req.id.toString())),"A booking already exists for this request");
                var q=get(uuid(data(req),"approvedQuoteId"));require(q.status.equals("ACCEPTED"),"Accepted quotation required");var qd=data(q);availableProvider(uuid(qd,"providerId"));
                required(d,"scheduledDate");require(Boolean.TRUE.equals(d.get("operationsChecked")),"Operations checks must be recorded");
                require(!LocalDate.parse(text(d,"scheduledDate")).isBefore(LocalDate.now()),"Schedule must not be in the past");
                d.put("providerId",text(qd,"providerId"));d.put("quoteId",q.id.toString());d.put("approvedScope",qd);d.put("amount",qd.get("total"));d.put("address",data(req).get("address"));r.ownerId=req.ownerId;r.status="CONFIRMED";
            }
            case "deliveries" -> {permit(staff(a));required(d,"pickup","dropoff","packageDetails");money(d,"charge");d.remove("partnerId");d.remove("proofId");r.status="AWAITING_ASSIGNMENT";if(!text(d,"bookingId").isEmpty())r.ownerId=linked(d,"bookingId","bookings").ownerId;}
            case "payments" -> {
                permit(staff(a));var b=lock(uuid(d,"bookingId"));require(b.kind.equals("bookings"),"Invalid booking");
                required(d,"method","reference","receiptReference");require(Set.of("CASH","BANK_TRANSFER","UPI_OFFLINE").contains(text(d,"method")),"Only manually verified offline records are enabled");
                require(money(d,"amount").signum()>0,"Amount must be positive");
                require(records.findByKindOrderByCreatedAtDesc(kind).stream().noneMatch(p->text(data(p),"reference").equals(text(d,"reference"))),"Duplicate payment reference");
                r.uniqueKey="payment:"+text(d,"reference");r.ownerId=b.ownerId;r.status="PENDING";
            }
            case "refunds" -> {
                permit(staff(a));var p=lock(uuid(d,"paymentId"));require(p.kind.equals("payments")&&p.status.equals("SUCCEEDED"),"Refund requires a successful payment");
                required(d,"reason","reference");var amount=money(d,"amount");require(amount.signum()>0,"Amount must be positive");
                BigDecimal refunded=records.findByKindOrderByCreatedAtDesc(kind).stream().filter(x->text(data(x),"paymentId").equals(p.id.toString())&&!x.status.equals("REJECTED")).map(x->money(data(x),"amount")).reduce(BigDecimal.ZERO,BigDecimal::add);
                require(refunded.add(amount).compareTo(money(data(p),"amount"))<=0,"Refund exceeds unrefunded payment");
                d.put("bookingId",data(p).get("bookingId"));r.uniqueKey="refund:"+text(d,"reference");r.ownerId=p.ownerId;r.status="PENDING";
            }
            case "complaints" -> {var b=linked(d,"bookingId","bookings");permit(staff(a)||b.ownerId.equals(a.id));required(d,"category","description");r.ownerId=b.ownerId;r.status="OPEN";}
            case "reviews" -> {var b=lock(uuid(d,"bookingId"));permit(b.ownerId.equals(a.id));require(b.kind.equals("bookings")&&Set.of("COMPLETED","CLOSED").contains(b.status),"Reviews are available after completion");required(d,"comment");var score=money(d,"rating");require(score.compareTo(BigDecimal.ONE)>=0&&score.compareTo(new BigDecimal("5"))<=0&&score.stripTrailingZeros().scale()<=0,"Rating must be 1 to 5");require(records.findByKindOrderByCreatedAtDesc(kind).stream().noneMatch(x->text(data(x),"bookingId").equals(b.id.toString())),"Review already exists");d.put("providerId",data(b).get("providerId"));r.status="PENDING_MODERATION";}
            case "notes" -> {permit(staff(a));required(d,"recordId","body","type");get(uuid(d,"recordId"));r.status="INTERNAL";}
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This record is created by the system");
        }
        put(r,d);records.save(r);audit(a,r,"CREATE",null);if(!r.ownerId.equals(a.id))notify(r.ownerId,"New "+kind,"A new record is available in your workspace.",r.id);return r;
    }
    @SuppressWarnings("unchecked") void quoteTotal(Map<String,Object> d){
        require(d.get("lineItems") instanceof List<?>,"Line items must be a list");var items=(List<?>)d.get("lineItems");require(!items.isEmpty()&&items.size()<=100,"Add 1 to 100 line items");BigDecimal total=BigDecimal.ZERO;
        for(Object item:items){require(item instanceof Map<?,?>,"Invalid line item");var line=(Map<String,Object>)item;required(line,"description","unit","type");require(Set.of("LABOUR","MATERIAL").contains(text(line,"type")),"Line type must be LABOUR or MATERIAL");var qty=money(line,"quantity");require(qty.signum()>0,"Quantity must be positive");total=total.add(qty.multiply(money(line,"unitPrice")));}
        total=total.add(money(d,"tax")).add(money(d,"fees")).add(money(d,"deliveryCharge"));require(total.signum()>0,"Total must be positive");d.put("total",total.setScale(2,RoundingMode.HALF_UP));
    }
    RecordEntity update(Account a,UUID id,Map<String,Object> input){
        var r=lock(id);permit(staff(a)||r.ownerId.equals(a.id));require(Set.of("addresses","categories","products","profiles","settings").contains(r.kind),"Use a workflow action; historical records cannot be edited");
        if(Set.of("categories","settings").contains(r.kind))permit(r.kind.equals("settings")?a.role.equals("SUPER_ADMIN"):staff(a));
        var d=data(r);require(input.containsKey("version")&&Long.parseLong(input.get("version").toString())==r.version,"Record changed; reload before saving");var copy=new LinkedHashMap<>(input);copy.remove("version");copy.remove("reviewNote");copy.remove("type");d.putAll(copy);
        if(r.kind.equals("products")){money(d,"price");money(d,"stock");required(d,"name","unit");}
        if(r.kind.equals("profiles")){r.status="PENDING_VERIFICATION";d.remove("reviewNote");}
        put(r,d);records.save(r);audit(a,r,"UPDATE",null);return r;
    }
    RecordEntity action(Account a,UUID id,String action,Map<String,Object> input){
        // Lock requests before their quotes, consistently with quotation creation.
        var initial=get(id);RecordEntity parent=initial.kind.equals("quotes")?lock(uuid(data(initial),"requestId")):null;
        var r=lock(id);permit(canRead(a,r));var d=data(r);String before=r.status;
        switch(r.kind){
            case "profiles" -> {
                permit(staff(a));require(r.status.equals("PENDING_VERIFICATION"),"Profile is not pending review");required(input,"note");
                require(Set.of("verify","reject","request-changes").contains(action),"Invalid verification decision");
                r.status=switch(action){case "verify"->"VERIFIED";case "reject"->"REJECTED";default->"CHANGES_REQUESTED";};d.put("reviewNote",input.get("note"));
                if(action.equals("verify")){require(!uploads.findByRecordId(r.id).isEmpty(),"Upload verification evidence before approval");var owner=accounts.findById(r.ownerId).orElseThrow();permit(!STAFF.contains(owner.role));owner.role=text(d,"type");accounts.save(owner);}
            }
            case "requests" -> {
                if(action.equals("respond")){permit(r.ownerId.equals(a.id));require(r.status.equals("MORE_INFORMATION_REQUIRED"),"No clarification is pending");required(input,"message");d.put("customerClarification",input.get("message"));r.status="UNDER_REVIEW";}
                else if(action.equals("cancel")){permit(r.ownerId.equals(a.id)||staff(a));require(!Set.of("APPROVED","CANCELLED").contains(r.status),"Approved requests must be handled through booking support");r.status="CANCELLED";}
                else {permit(staff(a));require(!Set.of("APPROVED","CANCELLED").contains(r.status),"Request is no longer open");
                    switch(action){case "review"->r.status="UNDER_REVIEW";case "clarify"->{required(input,"message");d.put("clarification",input.get("message"));r.status="MORE_INFORMATION_REQUIRED";}case "assign"->{UUID provider=uuid(input,"providerId");availableProvider(provider);d.put("providerId",provider.toString());r.status="PROVIDER_SEARCH";notify(provider,"New enquiry","A request has been assigned to you.",r.id);}default->throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported action");}
                }
            }
            case "quotes" -> {
                permit(parent.ownerId.equals(a.id));require(r.status.equals("OFFERED")&&parent.status.equals("AWAITING_CUSTOMER_DECISION"),"Decision is no longer available");
                require(Set.of("accept","reject","clarify").contains(action),"Unsupported decision");
                if(action.equals("clarify")){required(input,"message");d.put("clarification",input.get("message"));notify(r.ownerId,"Quote clarification",text(input,"message"),r.id);}
                else {if(action.equals("accept")){require(!LocalDate.parse(text(d,"validUntil")).isBefore(LocalDate.now()),"Quote expired");availableProvider(uuid(d,"providerId"));parent.status="APPROVED";var pd=data(parent);pd.put("approvedQuoteId",r.id.toString());put(parent,pd);records.save(parent);audit(a,parent,"CUSTOMER_APPROVAL","AWAITING_CUSTOMER_DECISION");}r.status=action.equals("accept")?"ACCEPTED":"REJECTED";d.put("decidedAt",Instant.now().toString());d.put("decidedBy",a.id.toString());}
            }
            case "bookings" -> {
                if(action.equals("close")){permit(r.ownerId.equals(a.id));require(r.status.equals("COMPLETED"),"Work must be completed first");r.status="CLOSED";d.put("customerConfirmedAt",Instant.now().toString());}
                else if(action.equals("dispute")){permit(r.ownerId.equals(a.id)||staff(a));require(!Set.of("CLOSED","CANCELLED").contains(r.status),"Booking is closed");required(input,"reason");d.put("disputeReason",input.get("reason"));r.status="DISPUTED";}
                else {permit(staff(a)||text(d,"providerId").equals(a.id.toString()));if(action.equals("cancel")){permit(staff(a));required(input,"reason");require(!Set.of("CLOSED","CANCELLED").contains(r.status),"Booking is closed");d.put("cancellationReason",input.get("reason"));r.status="CANCELLED";}else transition(r,action,Map.of("CONFIRMED",Map.of("schedule","SCHEDULED"),"SCHEDULED",Map.of("start","IN_PROGRESS"),"IN_PROGRESS",Map.of("hold","ON_HOLD","complete","COMPLETED"),"ON_HOLD",Map.of("resume","IN_PROGRESS")));}
            }
            case "deliveries" -> {
                if(action.equals("assign")){permit(staff(a));require(r.status.equals("AWAITING_ASSIGNMENT"),"Delivery already assigned");var partner=accounts.findById(uuid(input,"partnerId")).orElseThrow();require(partner.active&&partner.role.equals("DELIVERY_PARTNER")&&verified(partner.id),"Verified delivery partner required");d.put("partnerId",partner.id.toString());r.status="ASSIGNED";notify(partner.id,"Delivery assigned","Review your pickup and drop-off details.",r.id);}
                else {permit(staff(a)||text(d,"partnerId").equals(a.id.toString()));if(action.equals("deliver")){required(input,"proofId");var proof=uploads.findById(uuid(input,"proofId")).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Proof not found"));require(r.id.equals(proof.recordId),"Proof must belong to this delivery");d.put("proofId",input.get("proofId"));}transition(r,action,Map.of("ASSIGNED",Map.of("accept","PICKUP_PENDING","reject","AWAITING_ASSIGNMENT"),"PICKUP_PENDING",Map.of("pickup","PICKED_UP"),"PICKED_UP",Map.of("transit","IN_TRANSIT"),"IN_TRANSIT",Map.of("deliver","DELIVERED","fail","FAILED_DELIVERY"),"FAILED_DELIVERY",Map.of("return","RETURNED","retry","PICKUP_PENDING")));if(action.equals("reject"))d.remove("partnerId");}
            }
            case "payments" -> {permit(staff(a));require(r.status.equals("PENDING"),"Payment has already been decided");require(Set.of("confirm","fail").contains(action),"Invalid payment action");if(action.equals("confirm")){var booking=lock(uuid(d,"bookingId"));BigDecimal paid=records.findByKindOrderByCreatedAtDesc("payments").stream().filter(p->p.status.equals("SUCCEEDED")&&text(data(p),"bookingId").equals(booking.id.toString())).map(p->money(data(p),"amount")).reduce(BigDecimal.ZERO,BigDecimal::add);require(paid.add(money(d,"amount")).compareTo(money(data(booking),"amount"))<=0,"Payments exceed the agreed booking amount");required(input,"verificationNote");d.put("verificationNote",input.get("verificationNote"));}r.status=action.equals("confirm")?"SUCCEEDED":"FAILED";d.put("recordedBy",a.id.toString());d.put("recordedAt",Instant.now().toString());}
            case "refunds" -> {permit(staff(a));require(r.status.equals("PENDING"),"Refund has already been decided");require(Set.of("confirm","reject").contains(action),"Invalid refund action");required(input,"note");d.put("note",input.get("note"));r.status=action.equals("confirm")?"RECORDED":"REJECTED";}
            case "complaints" -> {
                if(action.equals("close")){permit(r.ownerId.equals(a.id));require(r.status.equals("RESOLVED"),"Complaint is not resolved");r.status="CLOSED";}
                else if(action.equals("reopen")){permit(r.ownerId.equals(a.id));require(Set.of("RESOLUTION_PROPOSED","RESOLVED").contains(r.status),"Cannot reopen at this stage");r.status="OPEN";}
                else {permit(staff(a));if(action.equals("assign")){var owner=accounts.findById(uuid(input,"staffId")).orElseThrow();require(staff(owner),"Operations owner required");d.put("staffId",owner.id.toString());}if(action.equals("propose")){required(input,"resolution");d.put("resolution",input.get("resolution"));}transition(r,action,Map.of("OPEN",Map.of("assign","ASSIGNED"),"ASSIGNED",Map.of("start","IN_PROGRESS"),"IN_PROGRESS",Map.of("propose","RESOLUTION_PROPOSED"),"RESOLUTION_PROPOSED",Map.of("resolve","RESOLVED")));}
            }
            case "reviews" -> {permit(staff(a));require(Set.of("approve","hide").contains(action),"Invalid moderation action");r.status=action.equals("approve")?"APPROVED":"HIDDEN";}
            case "categories","products" -> {permit(staff(a)||(r.kind.equals("products")&&r.ownerId.equals(a.id)));require(Set.of("activate","hide").contains(action),"Invalid action");r.status=action.equals("activate")?"ACTIVE":"HIDDEN";}
            case "notifications" -> {permit(r.ownerId.equals(a.id));require(action.equals("read"),"Invalid action");r.status="READ";}
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No actions for this module");
        }
        put(r,d);records.save(r);audit(a,r,action,before);if(!a.id.equals(r.ownerId))notify(r.ownerId,"Status updated",r.kind+": "+r.status,r.id);return r;
    }
    void transition(RecordEntity r,String action,Map<String,Map<String,String>> rules){String next=rules.getOrDefault(r.status,Map.of()).get(action);require(next!=null,"Action is not allowed from "+r.status);r.status=next;}
}
