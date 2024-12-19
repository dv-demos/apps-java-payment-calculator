package com.gradle.sdlcdemo.loancalculator.service;

import java.math.BigDecimal;

import com.gradle.sdlcdemo.loantools.LoanTools;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final LoanTools loanTools;

    public PaymentService(LoanTools loanTools) {
        this.loanTools = loanTools;
    }

    public BigDecimal calculate(double amount, double rate, int years) {
        return loanTools.calculatePayment(amount, rate, years);
    }
}
