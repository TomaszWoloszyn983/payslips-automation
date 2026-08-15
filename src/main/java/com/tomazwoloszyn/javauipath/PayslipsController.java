package com.tomazwoloszyn.javauipath;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tomazwoloszyn.javauipath.PayslipsService;
import com.tomazwoloszyn.javauipath.dto.PayslipResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payslip")
public class PayslipsController {

    /*
        Both, front-end and back-end contain a validation which
        only allows input the pdf files. In the future files type
        such as jpg, png should be accepted.
     */

    private final PayslipsService payslipsService;

    public PayslipsController(PayslipsService payslipsService) {
        this.payslipsService = payslipsService;
    }

    private long MAX_FILE_SIZE = 10 * 1024 * 1024;// 10 MB

    @GetMapping("/")
    public String displayHomePage(){
        return "index";
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> uploadPayslip(
            @RequestParam("files") MultipartFile[] files) throws Exception {

        String validationError = validateFiles(files);
        System.out.println("Number of files received: " + files.length);
        if (validateFiles(files) != null) {
            return ResponseEntity
                    .badRequest()
                    .body(validationError);
        }
        List<PayslipResult> results = new ArrayList<>();

        for (MultipartFile file : files) {
            System.out.println("Processing: " + file.getOriginalFilename());
            try {
                Map<String, String> extractionResults =
                        payslipsService.processPayslip(file);

                PayslipResult payslipResult = new PayslipResult(
                        file.getOriginalFilename(),
                        extractionResults
                );
                results.add(payslipResult);
            } catch (Exception e) {
                return ResponseEntity
                        .internalServerError()
                        .body("Error processing payslips.");
            }
        }
        return ResponseEntity.ok(results);
    }

    /**
     * For the Demo version, a file upload limit has been introduced. The limit is 3 files at a time.
     * This method checks whether this condition has been met.
     *
     * @param files
     * @return
     */
    private String validateFiles(MultipartFile[] files) {
        System.out.println("Number of files received: " + files.length
                            +"\nFile not larger than "+MAX_FILE_SIZE);
        if (files == null || files.length == 0) {
            return "Please upload at least one payslip.";
        }

        if (files.length > 3) {
            return "You can upload a maximum of 3 payslips at once.";
        }

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                return "File '" + file.getOriginalFilename()
                        + "' is empty.";
            }

            if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
                return "File '" + file.getOriginalFilename()
                        + "' is not a PDF.";
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return "File '" + file.getOriginalFilename()
                        + "' is larger than 10 MB.";
            }
        }

        return null;
    }
}
