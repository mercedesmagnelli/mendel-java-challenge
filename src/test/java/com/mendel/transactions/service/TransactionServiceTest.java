package com.mendel.transactions.service;

import com.mendel.transactions.domain.InvalidTransactionException;
import com.mendel.transactions.domain.TransactionNotFoundException;
import com.mendel.transactions.repository.InMemoryTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionServiceTest {

    private InMemoryTransactionRepository repository;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
        service = new TransactionService(repository);
    }

    @Test
    void sumsEachNodeWithItsDescendants() {
        service.save(10, 5000, "cars", null);
        service.save(11, 10000, "shopping", 10L);
        service.save(12, 5000, "shopping", 11L);

        assertThat(service.sum(10)).isEqualTo(20000);
        assertThat(service.sum(11)).isEqualTo(15000);
        assertThat(service.sum(12)).isEqualTo(5000);
        assertThat(service.findIdsByType("cars")).containsExactly(10L);
        assertThat(service.findIdsByType("shopping")).containsExactly(11L, 12L);
    }

    @Test
    void includesEveryBranchUnderTheSameParent() {
        service.save(1, 100, "a", null);
        service.save(2, 40, "a", 1L);
        service.save(3, 10, "a", 1L);
        service.save(4, 5, "a", 2L);

        assertThat(service.sum(1)).isEqualTo(155);
        assertThat(service.sum(2)).isEqualTo(45);
        assertThat(service.sum(3)).isEqualTo(10);
    }

    @Test
    void movingAParentRecalculatesBothSums() {
        service.save(10, 1000, "a", null);
        service.save(11, 100, "a", 10L);
        service.save(12, 10, "a", 11L);

        service.save(11, 100, "a", null);

        assertThat(service.sum(10)).isEqualTo(1000);
        assertThat(service.sum(11)).isEqualTo(110);
    }

    @Test
    void changingTheTypeUpdatesBothIndexes() {
        service.save(10, 50, "cars", null);
        service.save(11, 25, "cars", null);

        service.save(11, 25, "food", null);

        assertThat(service.findIdsByType("cars")).containsExactly(10L);
        assertThat(service.findIdsByType("food")).containsExactly(11L);
    }

    @Test
    void replacingTheAmountChangesTheSum() {
        service.save(10, 5000, "cars", null);
        service.save(10, 7000, "cars", null);

        assertThat(service.sum(10)).isEqualTo(7000);
        assertThat(service.findIdsByType("cars")).containsExactly(10L);
    }

    @Test
    void unknownTypeReturnsAnEmptyList() {
        assertThat(service.findIdsByType("missing")).isEmpty();
    }

    @Test
    void rejectsAMissingParent() {
        assertThatThrownBy(() -> service.save(11, 10, "cars", 10L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("Parent transaction 10 does not exist");
    }

    @Test
    void rejectsASelfParent() {
        service.save(10, 10, "cars", null);

        assertThatThrownBy(() -> service.save(10, 10, "cars", 10L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("A transaction cannot be its own parent");
    }

    @Test
    void rejectsACycleThroughDescendants() {
        service.save(10, 10, "cars", null);
        service.save(11, 10, "cars", 10L);
        service.save(12, 10, "cars", 11L);

        assertThatThrownBy(() -> service.save(10, 10, "cars", 12L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("parent_id would create a cycle");
        assertThat(service.sum(10)).isEqualTo(30);
    }

    @Test
    void sumOfAnUnknownIdFails() {
        assertThatThrownBy(() -> service.sum(99))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction 99 does not exist");
    }

    @Test
    void rejectsNonFiniteAmounts() {
        assertThatThrownBy(() -> service.save(1, Double.NaN, "cars", null))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> service.save(1, Double.POSITIVE_INFINITY, "cars", null))
                .isInstanceOf(InvalidTransactionException.class);
    }
}
