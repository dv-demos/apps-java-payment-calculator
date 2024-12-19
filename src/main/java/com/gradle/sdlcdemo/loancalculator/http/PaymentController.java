package com.gradle.sdlcdemo.loancalculator.http;

import java.math.BigDecimal;

import com.gradle.sdlcdemo.loancalculator.model.CalculatedPayment;
import com.gradle.sdlcdemo.loancalculator.service.HitCounterService;
import com.gradle.sdlcdemo.loancalculator.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Value("${cloud.application.instance_index:local}")
    private String instance;

    @Autowired
    private HitCounterService hitCounterService;

    @Autowired
    private PaymentService paymentService;

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @GetMapping()
    public CalculatedPayment calculatePayment(@RequestParam("amount") double amount, @RequestParam("rate") double rate,
                                              @RequestParam("years") int years) {

        BigDecimal payment = paymentService.calculate(amount, rate, years);

        logger.debug("Calculated payment of {} for input amount: {}, rate: {}, years: {}",
                payment, amount, rate, years);

        CalculatedPayment calculatedPayment = new CalculatedPayment();
        calculatedPayment.setAmount(amount);
        calculatedPayment.setRate(rate);
        calculatedPayment.setYears(years);
        calculatedPayment.setPayment(payment);
        calculatedPayment.setInstance(instance);
        calculatedPayment.setCount(hitCounterService.incrementCounter());

        return calculatedPayment;
    }
}
