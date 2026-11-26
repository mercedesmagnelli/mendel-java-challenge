package com.mendel.transactions.web;

import com.mendel.transactions.service.TransactionService;
import com.mendel.transactions.web.dto.StatusResponse;
import com.mendel.transactions.web.dto.SumResponse;
import com.mendel.transactions.web.dto.TransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @PutMapping(path = "/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public StatusResponse put(@PathVariable long transactionId, @Valid @RequestBody TransactionRequest request) {
        transactions.save(transactionId, request.amount(), request.type(), request.parentId());
        return new StatusResponse("ok");
    }

    @GetMapping("/types/{type}")
    public List<Long> byType(@PathVariable String type) {
        return transactions.findIdsByType(type);
    }

    @GetMapping("/sum/{transactionId}")
    public SumResponse sum(@PathVariable long transactionId) {
        return new SumResponse(transactions.sum(transactionId));
    }
}
