package com.mendel.transactions.domain;

/**
 * A stored transaction. {@code parentId} is absent when the transaction is a root.
 */
public record Transaction(long id, double amount, String type, Long parentId) {
}
