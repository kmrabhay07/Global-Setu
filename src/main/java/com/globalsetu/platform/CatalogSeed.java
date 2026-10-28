package com.globalsetu.platform;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import java.util.*;
@Configuration class CatalogSeed {
 @Bean ApplicationRunner seedCategories(Records records,Platform platform){return args->{if(records.findByKindOrderByCreatedAtDesc("categories").isEmpty()){for(String name:List.of("POP work","PVC wall panels","Ceiling work","Gypsum work","Interior services","Materials & delivery")){var r=new RecordEntity();r.kind="categories";r.ownerId=new UUID(0,0);r.status="ACTIVE";platform.put(r,Map.of("name",name,"city","Nashik","description","Request a quotation for "+name.toLowerCase()));records.save(r);}}};}
}
