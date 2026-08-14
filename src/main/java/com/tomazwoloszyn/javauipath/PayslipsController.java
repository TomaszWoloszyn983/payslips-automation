package com.tomazwoloszyn.javauipath;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tomazwoloszyn.javauipath.PayslipsService;
import com.tomazwoloszyn.javauipath.dto.PayslipResult;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final PayslipsService payslipsService;

    public PayslipsController(PayslipsService payslipsService) {
        this.payslipsService = payslipsService;
    }

    @GetMapping("/")
    public String displayHomePage(){
        return "index";
    }

    @PostMapping("/upload")
    @ResponseBody
    public List<PayslipResult> uploadPayslip(
            @RequestParam("files") MultipartFile[] files) throws Exception {

        System.out.println("Number of files received: " + files.length);
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
                System.err.println(
                        "Error processing " + file.getOriginalFilename()
                );
                e.printStackTrace();
            }
        }
        return results;
    }
}
