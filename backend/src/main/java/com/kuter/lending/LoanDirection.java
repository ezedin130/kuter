package com.kuter.lending;

/**
 * Direction of an inventory lending transaction.
 */
public enum LoanDirection {
    BORROW, // Incoming from an external counterparty
    LOAN    // Outgoing to an external counterparty
}
