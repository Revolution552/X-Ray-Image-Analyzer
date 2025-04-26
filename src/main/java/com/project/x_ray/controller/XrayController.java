package com.project.x_ray.controller;

import com.project.x_ray.model.XrayAnalyzer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class XrayController {

    private final XrayAnalyzer xrayAnalyzer = new XrayAnalyzer();

    // Endpoint to upload X-ray image
    @PostMapping("/upload-xray")
    public ResponseEntity<String> uploadXray(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        // Save the file (you could save it to disk or process it directly)
        try {
            String filePath = "uploads/" + file.getOriginalFilename();
            File dest = new File(filePath);
            file.transferTo(dest);
            return ResponseEntity.ok("File uploaded successfully: " + filePath);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error saving the file");
        }
    }


    @PostMapping("/analyze-xray")
    public ResponseEntity<String> analyzeXray(@RequestParam("file") MultipartFile file) {
        String result = xrayAnalyzer.analyzeXray(file);
        return ResponseEntity.ok(result);
    }
}
