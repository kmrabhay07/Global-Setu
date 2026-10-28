package com.globalsetu.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController @RequestMapping("/api/auth")
class Auth {
    private final Accounts accounts; private final Sessions sessions; private final PasswordEncoder encoder;
    private final Map<String,List<Long>> attempts = new ConcurrentHashMap<>();
    Auth(Accounts a,Sessions s,PasswordEncoder p){accounts=a;sessions=s;encoder=p;}
    record Register(@Pattern(regexp="[0-9]{10,15}") @NotBlank String mobile,@NotBlank @Size(max=100) String name,@Size(min=12,max=72) @NotNull String password,@AssertTrue boolean consent){}
    record Login(@NotBlank String mobile,@NotBlank @Size(max=72) String password){}
    synchronized void limit(String ip) {
        long now=System.currentTimeMillis();
        attempts.entrySet().removeIf(e->e.getValue().stream().allMatch(v->v<now-900000));
        var values=attempts.computeIfAbsent(ip,k->new ArrayList<>());values.removeIf(t->t<now-900000);
        if(values.size()>=20)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Try again in 15 minutes");
        values.add(now);
    }
    @PostMapping("/register") @Transactional Map<String,Object> register(@Valid @RequestBody Register r,HttpServletRequest request) {
        limit(request.getRemoteAddr());
        if(accounts.findByMobile(r.mobile()).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Registration could not be completed");
        var a=new Account();a.mobile=r.mobile();a.name=r.name();a.passwordHash=encoder.encode(r.password());accounts.save(a);
        return issue(a);
    }
    @PostMapping("/login") Map<String,Object> login(@Valid @RequestBody Login r,HttpServletRequest request) {
        limit(request.getRemoteAddr());
        Account a=accounts.findByMobile(r.mobile()).filter(x->x.active).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"));
        if(!encoder.matches(r.password(),a.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        return issue(a);
    }
    Map<String,Object> issue(Account a){byte[] b=new byte[32];new SecureRandom().nextBytes(b);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(b);var s=new LoginSession();s.tokenHash=Security.digest(token);s.accountId=a.id;s.expiresAt=Instant.now().plusSeconds(28800);sessions.save(s);return Map.of("token",token,"account",a,"expiresAt",s.expiresAt);}
    @GetMapping("/me") Account me(Authentication auth){return (Account)auth.getPrincipal();}
    record ProfileUpdate(@NotBlank @Size(max=100) String name){}
    @PatchMapping("/me") Account profile(Authentication auth,@Valid @RequestBody ProfileUpdate input){var a=accounts.findById(((Account)auth.getPrincipal()).id).orElseThrow();a.name=input.name();return accounts.save(a);}
    @PostMapping("/logout") Map<String,Boolean> logout(@RequestHeader("Authorization") String header){sessions.deleteById(Security.digest(header.substring(7)));return Map.of("success",true);}
}
@Configuration class Bootstrap {
    @Bean ApplicationRunner admin(Accounts accounts,PasswordEncoder encoder,@Value("${SETU_ADMIN_MOBILE:}") String mobile,@Value("${SETU_ADMIN_PASSWORD:}") String password){return args->{if(!mobile.isBlank() && accounts.findByMobile(mobile).isEmpty()){if(password.length()<16)throw new IllegalStateException("SETU_ADMIN_PASSWORD must have at least 16 characters");var a=new Account();a.name="Global Setu Administrator";a.mobile=mobile;a.role="SUPER_ADMIN";a.passwordHash=encoder.encode(password);accounts.save(a);}};}
}
