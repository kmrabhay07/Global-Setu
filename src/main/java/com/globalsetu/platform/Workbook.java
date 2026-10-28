package com.globalsetu.platform;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
final class Workbook {
 private Workbook(){}
 static byte[] create(List<Map<String,Object>> records){try{
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(buffer)){
   entry(zip,"[Content_Types].xml","<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
   entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
   entry(zip,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Records\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
   entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
   StringBuilder sheet=new StringBuilder("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
   List<String> columns=List.of("id","status","ownerId","createdAt","updatedAt","data");row(sheet,new ArrayList<>(columns));for(var record:records)row(sheet,columns.stream().map(k->Objects.toString(record.get(k),"")).toList());sheet.append("</sheetData></worksheet>");entry(zip,"xl/worksheets/sheet1.xml",sheet.toString());
  }return buffer.toByteArray();
 }catch(IOException e){throw new UncheckedIOException(e);}}
 static void row(StringBuilder b,List<String> values){b.append("<row>");for(String value:values)b.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")).append("</t></is></c>");b.append("</row>");}
 static void entry(ZipOutputStream zip,String path,String content)throws IOException{zip.putNextEntry(new ZipEntry(path));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
}
