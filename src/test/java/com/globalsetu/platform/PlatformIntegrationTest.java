package com.globalsetu.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:setutests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","SETU_ADMIN_MOBILE=9999999999","SETU_ADMIN_PASSWORD=TestAdminPasswordOnly123!","SETU_UPLOAD_DIR=./target/test-uploads","SETU_OTP_DEV=true"})
@AutoConfigureMockMvc
class PlatformIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 String admin()throws Exception{return token("9999999999","TestAdminPasswordOnly123!");}
 String token(String phone,String password)throws Exception{return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("mobile",phone,"password",password)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();}
 String register()throws Exception{String phone="8"+String.format("%09d",Math.abs(new Random().nextInt(999999999)));return json.readTree(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("mobile",phone,"name","Test Customer","password","CustomerPassword123!","consent",true)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();}
 Map<String,Object> create(String token,String kind,Map<String,Object> data)throws Exception{return json.readValue(mvc.perform(post("/api/records/"+kind).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(data))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),Map.class);}
 @Test void anonymousCannotReadPrivateRecords()throws Exception{mvc.perform(get("/api/records/requests")).andExpect(status().isUnauthorized());mvc.perform(get("/api/catalog/categories")).andExpect(status().isOk());}
 @Test void customersCannotAccessOtherAddressesOrAdminModules()throws Exception{
  String one=register(),two=register();var address=create(one,"addresses",Map.of("label","Home","address","Test street","city","Nashik","postalCode","422001"));
  var response=mvc.perform(get("/api/records/addresses").header("Authorization","Bearer "+two)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(response.contains(address.get("id").toString()));
  mvc.perform(patch("/api/records/"+address.get("id")).header("Authorization","Bearer "+two).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"label\":\"Stolen\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/accounts").header("Authorization","Bearer "+one)).andExpect(status().isForbidden());
  mvc.perform(post("/api/records/categories").header("Authorization","Bearer "+one).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Fake\",\"city\":\"Nashik\"}")).andExpect(status().isForbidden());
 }
 @Test void bookingRequiresCustomerApprovalAndVerificationRequiresEvidence()throws Exception{
  String customer=register(),admin=admin();var catalogue=json.readTree(mvc.perform(get("/api/catalog/categories")).andReturn().getResponse().getContentAsString());
  var data=new LinkedHashMap<String,Object>();data.putAll(Map.of("title","New ceiling","description","Test work","address","Test address","city","Nashik","categoryId",catalogue.get(0).get("id").asText(),"quantity",100,"unit","sq ft","budget",1000,"preferredDate","2099-01-01","priority","Quality"));data.put("consent",true);var req=create(customer,"requests",data);
  mvc.perform(post("/api/records/bookings").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("requestId",req.get("id"),"scheduledDate","2099-01-01","operationsChecked",true)))).andExpect(status().isConflict());
  var profile=create(customer,"profiles",Map.of("name","Test provider","type","PROFESSIONAL","city","Nashik","skills","POP","experience","2 years","availability","Weekdays","phone","8888888888"));
  mvc.perform(post("/api/records/"+profile.get("id")+"/actions/verify").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Reviewed\"}")).andExpect(status().isConflict());
 }
 @Test void uploadsRejectExecutableAndRemainPrivate()throws Exception{
  String one=register(),two=register();var address=create(one,"addresses",Map.of("label","Home","address","Test street","city","Nashik","postalCode","422001"));
  mvc.perform(multipart("/api/uploads").file(new MockMultipartFile("file","bad.html","text/html","<script>alert(1)</script>".getBytes())).param("recordId",address.get("id").toString()).header("Authorization","Bearer "+one)).andExpect(status().isConflict());
  mvc.perform(get("/api/uploads").param("recordId",address.get("id").toString()).header("Authorization","Bearer "+two)).andExpect(status().isForbidden());
 }
 Map<String,Object> action(String token,Object id,String action,Map<String,Object> body)throws Exception{return json.readValue(mvc.perform(post("/api/records/"+id+"/actions/"+action).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),Map.class);}
 String accountId(String token)throws Exception{return json.readTree(mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsString()).get("id").asText();}
 @Test void customerApprovalBookingPaymentAndRefundFlow()throws Exception{
  String customer=register(),provider=register(),admin=admin();String providerId=accountId(provider);
  var profile=create(provider,"profiles",Map.of("name","Verified artisan","type","PROFESSIONAL","city","Nashik","skills","POP","experience","3 years","availability","Weekdays","phone","8888888888"));
  mvc.perform(multipart("/api/uploads").file(new MockMultipartFile("file","review.pdf","application/pdf","%PDF-1.4 test verification evidence".getBytes())).param("recordId",profile.get("id").toString()).header("Authorization","Bearer "+provider)).andExpect(status().isOk());
  action(admin,profile.get("id"),"verify",Map.of("note","Identity and references checked"));
  var catalogue=json.readTree(mvc.perform(get("/api/catalog/categories")).andReturn().getResponse().getContentAsString());
  var d=new LinkedHashMap<String,Object>();d.putAll(Map.of("title","Interior work","description","Ceiling work","address","Test address","city","Nashik","categoryId",catalogue.get(0).get("id").asText(),"quantity",2,"unit","sq ft","budget",1100,"preferredDate","2099-01-01","priority","Quality"));d.put("consent",true);var request=create(customer,"requests",d);
  action(admin,request.get("id"),"assign",Map.of("providerId",providerId));
  var q=new LinkedHashMap<String,Object>();q.putAll(Map.of("requestId",request.get("id"),"scope","Two units","timeline","1 day","validUntil","2099-01-01","termsVersion","terms-v1","lineItems",List.of(Map.of("description","Labour","type","LABOUR","quantity",2,"unit","units","unitPrice",500)),"tax",100,"fees",0,"deliveryCharge",0));
  var quote=create(provider,"quotes",q);assertEquals(1100.0,((Number)((Map<?,?>)quote.get("data")).get("total")).doubleValue());
  action(customer,quote.get("id"),"accept",Map.of());
  mvc.perform(post("/api/records/"+quote.get("id")+"/actions/accept").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isConflict());
  var bookingBody=Map.<String,Object>of("requestId",request.get("id"),"scheduledDate","2099-01-01","operationsChecked",true);var booking=create(admin,"bookings",bookingBody);
  mvc.perform(post("/api/records/bookings").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bookingBody))).andExpect(status().isConflict());
  var payment=create(admin,"payments",Map.of("bookingId",booking.get("id"),"amount",1100,"method","BANK_TRANSFER","reference",UUID.randomUUID().toString(),"receiptReference","Receipt 1"));
  assertEquals("PENDING",payment.get("status"));action(admin,payment.get("id"),"confirm",Map.of("verificationNote","Bank statement checked"));
  mvc.perform(post("/api/records/refunds").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("paymentId",payment.get("id"),"amount",1200,"reference","refund-too-large","reason","Adjustment")))).andExpect(status().isConflict());
  var refund=create(admin,"refunds",Map.of("paymentId",payment.get("id"),"amount",100,"reference",UUID.randomUUID().toString(),"reason","Adjustment"));action(admin,refund.get("id"),"confirm",Map.of("note","Refund transfer confirmed"));
  action(provider,booking.get("id"),"schedule",Map.of());action(provider,booking.get("id"),"start",Map.of());action(provider,booking.get("id"),"complete",Map.of());action(customer,booking.get("id"),"close",Map.of());
  var review=create(customer,"reviews",Map.of("bookingId",booking.get("id"),"rating",5,"comment","Completed as agreed"));action(admin,review.get("id"),"approve",Map.of());
  mvc.perform(get("/api/reports/bookings?format=xlsx").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
 }
 @Test void otpIsSingleUse()throws Exception{
  var request=mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON).content("{\"mobile\":\"8777777777\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();var challenge=json.readTree(request);
  var body=json.writeValueAsString(Map.of("challengeId",challenge.get("challengeId").asText(),"code",challenge.get("developmentCode").asText(),"name","OTP User","consent",true));
  mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
 }
 @Test void csvNeutralizesFormulaInjection(){assertEquals("\"'=1+1\"",Api.csv("=1+1"));assertEquals("\"safe\"",Api.csv("safe"));}
}
