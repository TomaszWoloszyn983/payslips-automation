package com.tomazwoloszyn.javauipath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@SpringBootApplication
public class JavauipathApplication {
	private static PayslipsService  payslipsService = new PayslipsService();

	public static void main(String[] args) throws Exception {
		System.out.println("Application Started");

//        Payslip file sample
//        Path temp_payslip_path = Paths.get("D:/Kodowanie/UIPath/JobBoardsLogger/Dunnes Payslips Reader/Data/Email_Payslip_Processed/5040202_02DEC16.pdf");
//        File payslipFile = temp_payslip_path.toFile();
//
//		Map<String, String> extractionResults = payslipsService.processPayslip(payslipFile);
//
//        for (Map.Entry<String, String> extractionResult : extractionResults.entrySet()) {
//            System.out.println(extractionResult.getKey() + ": " + extractionResult.getValue());
//        }
		
		SpringApplication.run(JavauipathApplication.class, args);
	}

}
