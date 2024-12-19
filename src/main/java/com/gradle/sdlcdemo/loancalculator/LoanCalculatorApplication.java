package com.gradle.sdlcdemo.loancalculator;

import com.gradle.sdlcdemo.loantools.LoanTools;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LoanCalculatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanCalculatorApplication.class, args);
    }

    @Bean
    LoanTools loanTools() {
        return new LoanTools();
    }
}
