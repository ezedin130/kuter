package com.kuter.identity;

/**
 * Granular permissions assignable to shop staff memberships.
 */
public enum ShopPermission {
    PRODUCTS_VIEW,        // View products and catalog information
    PRODUCTS_MANAGE,      // Create and edit product metadata (excluding selling prices)
    PRICES_MANAGE,        // Change product selling prices (STRICTLY OWNER ONLY)
    INVENTORY_RECEIVE,    // Receive purchased or incoming stock via delivery notes
    INVENTORY_ADJUST,     // Perform accountable inventory adjustments
    INVENTORY_VIEW,       // View stock balances and movement history
    SALES_CREATE,         // Create and finalize POS sales
    SALES_VIEW,           // View sales history and printable receipts
    SALES_REPORTS_VIEW,   // View financial summaries and reports
    USERS_MANAGE,         // Manage shop users and assign permissions (STRICTLY OWNER ONLY)
    LENDING_REQUEST,      // Record incoming borrowing or outgoing lending transactions
    LENDING_MANAGE,       // Confirm inventory movements and process returns
    LENDING_VIEW,         // View lending and borrowing records and obligations
    SUBSCRIPTION_VIEW     // View shop subscription tier and quota meters
}
