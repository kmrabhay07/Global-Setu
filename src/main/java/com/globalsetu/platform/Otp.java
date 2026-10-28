package com.globalsetu.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import static com.globalsetu.platform.Platform.*;

@Entity @Table(name="gs_otp") class OtpChallenge {
 @Id public UUID id=UUID.randomUUID(); public String mobile; public String codeHash;
 public Instant expiresAt; public int attempts; public boolean consumed;
}
interface OtpRepository extends JpaRepository<OtpChallenge,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from OtpChallenge o where o.id=:id") Optional<OtpChallenge> lock(@Param("id") UUID id);
}
@RestController @RequestMapping("/api/auth/otp")
class Otp {
 final OtpRepository challenges;final Accounts accounts;final Auth auth;final PasswordEncoder encoder;final ObjectMapper json;final boolean dev;final String endpoint,secret;
 Otp(OtpRepository c,Accounts a,Auth au,PasswordEncoder e,ObjectMapper j,Environment environment,@Value("${SETU_OTP_DEV:false}")boolean development,@Value("${SETU_OTP_WEBHOOK_URL:}")String url,@Value("${SETU_OTP_WEBHOOK_TOKEN:}")String token){challenges=c;accounts=a;auth=au;encoder=e;json=j;dev=development&&!Arrays.asList(environment.getActiveProfiles()).contains("prod");endpoint=url;secret=token;}
 record Request(@NotBlank @Pattern(regexp="[0-9]{10,15}")String mobile){}
 record Verify(@NotNull UUID challengeId,@NotBlank @Pattern(regexp="[0-9]{6}")String code,@NotBlank @Size(max=100)String name,@AssertTrue boolean consent){}
 @PostMapping("/request") Map<String,Object> request(@Valid @RequestBody Request input,HttpServletRequest req)throws Exception{
  auth.limit(req.getRemoteAddr());auth.limit("otp:"+input.mobile());
  if(!dev&&(endpoint.isBlank()||secret.isBlank()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"SMS sign-in is not configured yet. Use password sign-in.");
  String code=String.format("%06d",new SecureRandom().nextInt(1000000));var c=new OtpChallenge();c.mobile=input.mobile();c.codeHash=encoder.encode(code);c.expiresAt=Instant.now().plusSeconds(300);
  if(!dev){URI uri=URI.create(endpoint);require(uri.getScheme().equals("https"),"SMS endpoint must use HTTPS");var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+secret).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("phone",input.mobile(),"message","Your Global Setu sign-in code is "+code+". It expires in 5 minutes.","purpose","OTP")))).build();var response=client.send(request,HttpResponse.BodyHandlers.discarding());if(response.statusCode()<200||response.statusCode()>=300)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"SMS delivery request was not accepted");}
  challenges.save(c);var output=new LinkedHashMap<String,Object>();output.put("challengeId",c.id);output.put("expiresAt",c.expiresAt);if(dev)output.put("developmentCode",code);return output;
 }
 // Preserve failed-attempt increments even when the verification response is an error.
 @PostMapping("/verify") @Transactional(noRollbackFor=ResponseStatusException.class) Map<String,Object> verify(@Valid @RequestBody Verify input,HttpServletRequest req){
  auth.limit(req.getRemoteAddr());var c=challenges.lock(input.challengeId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid code"));
  require(!c.consumed&&c.expiresAt.isAfter(Instant.now())&&c.attempts<5,"Code expired or unavailable; request another");c.attempts++;challenges.save(c);
  if(!encoder.matches(input.code(),c.codeHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid code");c.consumed=true;challenges.save(c);
  var account=accounts.findByMobile(c.mobile).orElseGet(()->{var a=new Account();a.mobile=c.mobile;a.name=input.name();a.passwordHash=encoder.encode(UUID.randomUUID().toString()+UUID.randomUUID());return accounts.save(a);});permit(account.active);return auth.issue(account);
 }
}
