package com.mendel.transactions.service;

import com.mendel.transactions.domain.InvalidTransactionException;
import com.mendel.transactions.domain.Transaction;
import com.mendel.transactions.domain.TransactionNotFoundException;
import com.mendel.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactions;

    public TransactionService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    public void save(long id, double amount, String type, Long parentId) {
        if (type == null || type.isBlank()) {
            throw new InvalidTransactionException("type is required");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new InvalidTransactionException("amount must be a finite number");
        }
        transactions.save(new Transaction(id, amount, type, parentId));
    }

    public List<Long> findIdsByType(String type) {
        return transactions.findIdsByType(type);
    }

    /**
     * Amount of the transaction plus every descendant linked through {@code parent_id}.
     * Ancestors are not included: the sum of a child does not roll up into the parent chain.
     */
    public double sum(long id) {
        List<Transaction> linked = transactions.findWithDescendants(id);
        if (linked.isEmpty()) {
            throw new TransactionNotFoundException(id);
        }
        double total = 0d;
        for (Transaction transaction : linked) {
            total += transaction.amount();
        }
        return total;
    }
}
