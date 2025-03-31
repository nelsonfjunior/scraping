package com.example.demo.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ScrapingService {

    private static final String URL_ANS = "https://www.gov.br/ans/pt-br/acesso-a-informacao/participacao-da-sociedade/atualizacao-do-rol-de-procedimentos";
    private static final String OUTPUT_DIR = "anexos";
    private static final String ZIP_FILE = "anexos.zip";

    public Map<String, Object> scrapeAnexos() throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> downloadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Path outputPath = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputPath)) {
            Files.createDirectory(outputPath);
        }

        try {
            Document document = Jsoup.connect(URL_ANS).get();
            
            Elements links = document.select("a[href$=.pdf]");
            
            boolean foundAnexos = false;
            
            for (Element link : links) {
                String linkText = link.text().toLowerCase();
                
                if (linkText.contains("anexo i") || linkText.contains("anexo ii")) {
                    foundAnexos = true;
                    String pdfUrl = link.attr("abs:href");
                    String fileName = linkText.replace(" ", "_") + ".pdf";
                    
                    try {
                        Path filePath = downloadPdf(pdfUrl, fileName);
                        if (filePath != null) {
                            downloadedFiles.add(fileName);
                        }
                    } catch (Exception e) {
                        errors.add("Erro ao baixar " + fileName + ": " + e.getMessage());
                    }
                }
            }

            if (!foundAnexos) {
                errors.add("Nenhum anexo I ou II encontrado na página");
            }

            if (!downloadedFiles.isEmpty()) {
                createZipFile(downloadedFiles);
                result.put("zipFileCreated", true);
            } else {
                result.put("zipFileCreated", false);
            }

        } catch (IOException e) {
            errors.add("Erro ao acessar o site: " + e.getMessage());
            throw e;
        }

        result.put("downloadedFiles", downloadedFiles);
        result.put("errors", errors);
        
        return result;
    }

    private Path downloadPdf(String pdfUrl, String fileName) throws IOException, URISyntaxException {
        Path filePath = Paths.get(OUTPUT_DIR, fileName);
        
        URI uri = new URI(pdfUrl);
        
        try (InputStream in = uri.toURL().openStream();
             FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            return filePath;
        }
    }

    private void createZipFile(List<String> fileNames) throws IOException {
        Path zipPath = Paths.get(ZIP_FILE);
        
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            for (String fileName : fileNames) {
                Path filePath = Paths.get(OUTPUT_DIR, fileName);
                if (Files.exists(filePath)) {
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    byte[] bytes = Files.readAllBytes(filePath);
                    zipOut.write(bytes, 0, bytes.length);
                    zipOut.closeEntry();
                }
            }
        }
    }

    public Path getZipFilePath() {
        return Paths.get(ZIP_FILE);
    }
}

