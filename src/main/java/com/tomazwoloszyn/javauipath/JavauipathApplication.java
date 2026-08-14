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

		SpringApplication.run(JavauipathApplication.class, args);

	}

}
