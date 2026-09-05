package com.innowise.orderservice.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TestConstant {
    public static final Integer INTEGER_ID = 1;
    public static final Long LONG_ID = 1L;
    public static final List<Long> LONG_IDS = List.of(LONG_ID);
    public static final LocalDateTime LOCAL_DATE_TIME_NOW = LocalDateTime.now();
    public static final String USER_NAME = "Test";
    public static final String USER_EMAIL = "test@test.test";
    public static final String ITEM_NAME = "Test product";
    public static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(99.99);
    public static final Integer ITEM_QUANTITY = 3;

    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    public static final String PAYMENT_STATUS_FAILED = "FAILED";

    public static final String NEW_ITEM_NAME = "New Product";
    public static final BigDecimal NEW_ITEM_PRICE = BigDecimal.valueOf(33.33);

    public static final String ROLE_USER_WITH_PREFIX = "ROLE_USER";
    public static final String ROLE_USER_WITHOUT_PREFIX = "USER";
    public static final String ROLE_ADMIN_WITH_PREFIX = "ROLE_ADMIN";
    public static final String ROLE_ADMIN_WITHOUT_PREFIX = "ADMIN";

    public static final String JSON_PATH_CUSTOMER = "$.customer";
    public static final String JSON_PATH_CUSTOMER_NAME = "$.customer.name";
    public static final String JSON_PATH_CUSTOMER_SURNAME = "$.customer.surname";
    public static final String JSON_PATH_CUSTOMER_EMAIL = "$.customer.email";
    public static final String JSON_PATH_CUSTOMER_ERROR_MESSAGE = "$.customer.errorMessage";

    public static final String JSON_PATH_ITEMS = "$.items";
    public static final String JSON_PATH_ITEM_NAME = "$.items[0].name";
    public static final String JSON_PATH_ITEM_PRICE = "$.items[0].price";
    public static final String JSON_PATH_ITEM_QUANTITY = "$.items[0].quantity";

    public static final String JSON_PATH_TOTAL_PRICE = "$.totalPrice";
    public static final String JSON_PATH_STATUS = "$.status";

    public static final String JSON_PATH_COMMON_ARRAY = "$";

    public static final String JSON_PATH_EXCEPTION_STATUS = "$.status";
    public static final String JSON_PATH_EXCEPTION_ERROR_MESSAGE = "$.errorMessage";
    public static final String JSON_PATH_EXCEPTION_DETAILS = "$.errorDetails";
    public static final String JSON_PATH_EXCEPTION_TIMESTAMP = "$.timestamp";

    private TestConstant() {
    }
}