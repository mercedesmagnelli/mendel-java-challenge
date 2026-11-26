package com.mendel.transactions.web;

import com.mendel.transactions.repository.TransactionRepository;
import com.mendel.transactions.web.dto.ErrorResponse;
import com.mendel.transactions.web.dto.StatusResponse;
import com.mendel.transactions.web.dto.SumResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionApiTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository.clear();
    }

    @Test
    void storesTransactionsAndAnswersTheSpecificationExample() {
        assertThat(put(10, "{\"amount\":5000,\"type\":\"cars\"}").getBody())
                .isEqualTo(new StatusResponse("ok"));
        put(11, "{\"amount\":10000,\"type\":\"shopping\",\"parent_id\":10}");
        put(12, "{\"amount\":5000,\"type\":\"shopping\",\"parent_id\":11}");

        ResponseEntity<List<Long>> cars = rest.exchange(
                "/transactions/types/cars",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        ResponseEntity<List<Long>> shopping = rest.exchange(
                "/transactions/types/shopping",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        assertThat(cars.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cars.getBody()).containsExactly(10L);
        assertThat(shopping.getBody()).containsExactly(11L, 12L);
        assertThat(sum(10).getBody()).isEqualTo(new SumResponse(20000));
        assertThat(sum(11).getBody()).isEqualTo(new SumResponse(15000));
        assertThat(sum(12).getBody()).isEqualTo(new SumResponse(5000));
    }

    @Test
    void unknownTypeIsAnEmptyListAndUnknownSumIsNotFound() {
        ResponseEntity<List<Long>> types = rest.exchange(
                "/transactions/types/cars",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        ResponseEntity<ErrorResponse> missing = rest.getForEntity("/transactions/sum/10", ErrorResponse.class);

        assertThat(types.getBody()).isEmpty();
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isEqualTo(new ErrorResponse("Transaction 10 does not exist"));
    }

    @Test
    void rejectsAnIncompleteBodyAndAnUnknownParent() {
        ResponseEntity<ErrorResponse> incomplete = putExpectingError(1, "{\"amount\":10}");
        ResponseEntity<ErrorResponse> orphan = putExpectingError(2, "{\"amount\":10,\"type\":\"cars\",\"parent_id\":99}");

        assertThat(incomplete.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(incomplete.getBody().message()).contains("type");
        assertThat(orphan.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orphan.getBody().message()).isEqualTo("Parent transaction 99 does not exist");
    }

    private ResponseEntity<StatusResponse> put(long id, String json) {
        ResponseEntity<StatusResponse> response = rest.exchange(
                "/transactions/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(json, jsonHeaders()),
                StatusResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private ResponseEntity<ErrorResponse> putExpectingError(long id, String json) {
        return rest.exchange(
                "/transactions/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(json, jsonHeaders()),
                ErrorResponse.class);
    }

    private ResponseEntity<SumResponse> sum(long id) {
        ResponseEntity<SumResponse> response = rest.getForEntity("/transactions/sum/" + id, SumResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
