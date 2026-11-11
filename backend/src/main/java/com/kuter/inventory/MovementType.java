package com.kuter.inventory;

/**
 * Type of stock transaction recorded in the immutable movement ledger.
 */
public enum MovementType {
    RECEIPT,
    SALE,
    ADJUSTMENT,
    RETURN,
    BORROW_RECEIPT,
    BORROW_RETURN_DISPATCH,
    LOAN_DISPATCH,
    LOAN_RETURN_RECEIPT
}
