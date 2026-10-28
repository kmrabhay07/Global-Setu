package com.globalsetu.platform;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static com.globalsetu.platform.Platform.*;

@Entity @Table(name="gs_upload") class Upload {
    @Id public UUID id=UUID.randomUUID();
    public UUID ownerId;
    public UUID recordId;
    public String filename;
    public String mime;
    public long size;
    public Instant createdAt=Instant.now();
}
interface UploadRepository extends JpaRepository<Upload,UUID>{List<Upload> findByRecordId(UUID recordId);}
@RestController @RequestMapping("/api/uploads")
class Uploads {
    final UploadRepository files;final Platform platform;final Path root;
    Uploads(UploadRepository f,Platform p,@Value("${SETU_UPLOAD_DIR:./data/uploads}")String dir){files=f;platform=p;root=Path.of(dir).toAbsolutePath();}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Upload upload(Authentication auth,@RequestParam MultipartFile file,@RequestParam UUID recordId)throws Exception{
        Account a=(Account)auth.getPrincipal();var record=platform.get(recordId);permit(staff(a)||record.ownerId.equals(a.id)||((record.kind.equals("deliveries"))&&text(platform.data(record),"partnerId").equals(a.id.toString())));
        require(file.getSize()>0&&file.getSize()<=5*1024*1024,"File must be between 1 byte and 5 MB");
        byte[] b=file.getBytes();String type="";
        if(b.length>=8&&b[0]==(byte)137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10)type="image/png";
        else if(b.length>=3&&b[0]==(byte)255&&b[1]==(byte)216&&b[2]==(byte)255)type="image/jpeg";
        else if(b.length>=5&&new String(b,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))type="application/pdf";
        require(!type.isEmpty(),"Only PNG, JPEG and PDF files are accepted");
        var f=new Upload();f.ownerId=a.id;f.recordId=recordId;f.size=b.length;f.mime=type;f.filename=Objects.toString(file.getOriginalFilename(),"attachment").replaceAll("[^a-zA-Z0-9._-]","_");
        Files.createDirectories(root);Files.write(root.resolve(f.id.toString()),b,StandardOpenOption.CREATE_NEW);try{files.save(f);}catch(RuntimeException e){Files.deleteIfExists(root.resolve(f.id.toString()));throw e;}return f;
    }
    @GetMapping List<Upload> list(Authentication auth,@RequestParam UUID recordId){var a=(Account)auth.getPrincipal();var r=platform.get(recordId);permit(staff(a)||r.ownerId.equals(a.id)||(r.kind.equals("deliveries")&&text(platform.data(r),"partnerId").equals(a.id.toString())));return files.findByRecordId(recordId);}
    @GetMapping("/{id}") ResponseEntity<byte[]> download(Authentication auth,@PathVariable UUID id)throws Exception{var a=(Account)auth.getPrincipal();var f=files.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));var r=platform.get(f.recordId);permit(staff(a)||f.ownerId.equals(a.id)||r.ownerId.equals(a.id));return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.mime)).header("Content-Disposition",ContentDisposition.attachment().filename(f.filename).build().toString()).header("X-Content-Type-Options","nosniff").body(Files.readAllBytes(root.resolve(f.id.toString())));}
    @DeleteMapping("/{id}") Map<String,Boolean> delete(Authentication auth,@PathVariable UUID id)throws Exception{var a=(Account)auth.getPrincipal();var f=files.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));permit(staff(a)||f.ownerId.equals(a.id));files.delete(f);Files.deleteIfExists(root.resolve(f.id.toString()));return Map.of("deleted",true);}
}
