package com.mendel.transactions.repository;

import com.mendel.transactions.domain.InvalidTransactionException;
import com.mendel.transactions.domain.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Process-local store. Every public method shares one lock so a write and the
 * parent-link check that guards it cannot interleave.
 */
@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> byId = new HashMap<>();
    private final Map<String, LinkedHashSet<Long>> idsByType = new HashMap<>();
    private final Map<Long, LinkedHashSet<Long>> childIdsByParent = new HashMap<>();

    @Override
    public synchronized void save(Transaction transaction) {
        assertLinkIsValid(transaction);

        Transaction previous = byId.get(transaction.id());
        if (previous != null) {
            if (!previous.type().equals(transaction.type())) {
                removeFromType(previous.type(), previous.id());
            }
            if (!Objects.equals(previous.parentId(), transaction.parentId())) {
                unlink(previous);
            }
        }

        byId.put(transaction.id(), transaction);
        idsByType.computeIfAbsent(transaction.type(), ignored -> new LinkedHashSet<>()).add(transaction.id());
        if (transaction.parentId() != null) {
            childIdsByParent.computeIfAbsent(transaction.parentId(), ignored -> new LinkedHashSet<>())
                    .add(transaction.id());
        }
    }

    @Override
    public synchronized Optional<Transaction> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public synchronized List<Long> findIdsByType(String type) {
        Set<Long> ids = idsByType.get(type);
        if (ids == null) {
            return List.of();
        }
        return List.copyOf(ids);
    }

    @Override
    public synchronized List<Transaction> findWithDescendants(long id) {
        if (!byId.containsKey(id)) {
            return List.of();
        }

        List<Transaction> collected = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(id);

        while (!pending.isEmpty()) {
            long currentId = pending.removeFirst();
            if (!visited.add(currentId)) {
                continue;
            }
            Transaction current = byId.get(currentId);
            if (current == null) {
                continue;
            }
            collected.add(current);
            Set<Long> children = childIdsByParent.get(currentId);
            if (children != null) {
                pending.addAll(children);
            }
        }
        return List.copyOf(collected);
    }

    @Override
    public synchronized void clear() {
        byId.clear();
        idsByType.clear();
        childIdsByParent.clear();
    }

    private void assertLinkIsValid(Transaction transaction) {
        Long parentId = transaction.parentId();
        if (parentId == null) {
            return;
        }
        if (parentId == transaction.id()) {
            throw new InvalidTransactionException("A transaction cannot be its own parent");
        }
        if (!byId.containsKey(parentId)) {
            throw new InvalidTransactionException("Parent transaction " + parentId + " does not exist");
        }

        Long current = parentId;
        while (current != null) {
            if (current == transaction.id()) {
                throw new InvalidTransactionException("parent_id would create a cycle");
            }
            Transaction ancestor = byId.get(current);
            current = ancestor == null ? null : ancestor.parentId();
        }
    }

    private void removeFromType(String type, long id) {
        Set<Long> ids = idsByType.get(type);
        if (ids == null) {
            return;
        }
        ids.remove(id);
        if (ids.isEmpty()) {
            idsByType.remove(type);
        }
    }

    private void unlink(Transaction transaction) {
        if (transaction.parentId() == null) {
            return;
        }
        Set<Long> siblings = childIdsByParent.get(transaction.parentId());
        if (siblings == null) {
            return;
        }
        siblings.remove(transaction.id());
        if (siblings.isEmpty()) {
            childIdsByParent.remove(transaction.parentId());
        }
    }
}
