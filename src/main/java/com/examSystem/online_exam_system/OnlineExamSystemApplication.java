package com.examSystem.online_exam_system;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;

@SpringBootApplication
public class OnlineExamSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineExamSystemApplication.class, args);
	}

	@Bean
	public ApplicationRunner openBrowser() {
		return args -> {
			try {
				String url = "http://localhost:8080/users/login";

				// This works more reliably than Desktop
				if (java.awt.Desktop.isDesktopSupported()) {
					new ProcessBuilder(
							"cmd", "/c", "start", url
					).start();
				}

			} catch (Exception e) {
				System.out.println("Auto browser open failed: " + e.getMessage());
			}
		};
	}
}