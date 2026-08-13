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

    @PostMapping("/upload")
    public Map<String, String> uploadPayslip(
            @RequestParam("file") MultipartFile file) throws Exception {
        System.out.println("Uploading payslip...");

        return payslipsService.processPayslip(file);
    }
}
