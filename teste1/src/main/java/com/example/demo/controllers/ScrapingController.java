package com.example.demo.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.services.ScrapingService;

@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {

    @Autowired
    private ScrapingService scrapingService;

    @GetMapping("/start")
    public ResponseEntity<Map<String, Object>> startScraping() {
        try {
            Map<String, Object> result = scrapingService.scrapeAnexos();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Falha ao executar web scraping: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadZip() {
        try {
            Path zipFilePath = scrapingService.getZipFilePath();
            
            if (!Files.exists(zipFilePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(zipFilePath.toUri());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilePath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
