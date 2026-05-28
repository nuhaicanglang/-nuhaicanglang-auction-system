package com.auction.business.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BidDTOValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validPricePassesValidation() {
        BidDTO dto = new BidDTO();
        dto.setPrice(new BigDecimal("100.00"));

        Set<ConstraintViolation<BidDTO>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullPriceFailsValidation() {
        BidDTO dto = new BidDTO();

        Set<ConstraintViolation<BidDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("出价金额不能为空");
    }

    @Test
    void zeroPriceFailsValidation() {
        BidDTO dto = new BidDTO();
        dto.setPrice(BigDecimal.ZERO);

        Set<ConstraintViolation<BidDTO>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("出价金额必须大于0");
    }
}
