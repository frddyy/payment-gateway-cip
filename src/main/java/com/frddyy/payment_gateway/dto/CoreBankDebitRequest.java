package com.frddyy.payment_gateway.dto;

import java.math.BigDecimal;

public record CoreBankDebitRequest(String account, BigDecimal amount) {}