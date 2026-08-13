package com.tomazwoloszyn.javauipath;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tomazwoloszyn.javauipath.PayslipsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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

//    @GetMapping("/test")
//    public String testExtraction() throws Exception {
//        Path temp_payslip_path = Paths.get("D:/Kodowanie/UIPath/JobBoardsLogger/Dunnes Payslips Reader/Data/Email_Payslip_Processed/5040202_02DEC16.pdf");
//        File payslipFile = temp_payslip_path.toFile();
//
//        Map<String, String> extractionResults = payslipsService.processPayslip(payslipFile);
//
//        for (Map.Entry<String, String> extractionResult : extractionResults.entrySet()) {
//            System.out.println(extractionResult.getKey() + ": " + extractionResult.getValue());
//        }
//        return "index.html";
//    }

//    @PostMapping("/extract")
//    public ExtractionResponse extract(@RequestParam MultipartFile file) throws Exception {
//        Path temp_payslip_path = Paths.get("D:/Kodowanie/UIPath/JobBoardsLogger/Dunnes Payslips Reader/Data/Email_Payslip_Processed/5040202_02DEC16.pdf");
//        File payslipFile = temp_payslip_path.toFile();
//
//        Map<String, String> extractionResults = payslipsService.processPayslip(payslipFile);
//
//        for (Map.Entry<String, String> extractionResult : extractionResults.entrySet()) {
//            System.out.println(extractionResult.getKey() + ": " + extractionResult.getValue());
//        }
//        return null;
//    }

    @PostMapping("/upload")
    public Map<String, String> uploadPayslip(
            @RequestParam("file") MultipartFile file) throws Exception {
        System.out.println("Uploading payslip...");

//        try {
//            Map<String, String> extractionResults =
//                    payslipsService.processPayslip(file);
//
//            model.addAttribute("extractionResults", extractionResults);
//            System.out.println("Display results on the page");
//            return "results";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "index";
//        }

        return payslipsService.processPayslip(file);
//        return null;
    }

}
