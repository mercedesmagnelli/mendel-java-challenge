package com.mendel.transactions.repository;

import com.mendel.transactions.domain.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    void save(Transaction transaction);

    Optional<Transaction> findById(long id);

    List<Long> findIdsByType(String type);

    /**
     * The transaction and every descendant reached by following child links.
     * Empty when {@code id} is unknown.
     */
    List<Transaction> findWithDescendants(long id);

    void clear();
}
