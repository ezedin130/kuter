import React from 'react';
import { Printer } from 'lucide-react';
import { Modal } from './Modal';
import { SaleReceiptResponseDto } from '../api/types';

interface SaleReceiptModalProps {
  isOpen: boolean;
  onClose: () => void;
  receipt: SaleReceiptResponseDto | null;
}

export const SaleReceiptModal: React.FC<SaleReceiptModalProps> = ({
  isOpen,
  onClose,
  receipt,
}) => {
  if (!receipt) return null;

  const handlePrint = () => {
    window.print();
  };

  const formatDate = (isoStr?: string) => {
    if (!isoStr) return '';
    try {
      return new Date(isoStr).toLocaleString();
    } catch {
      return isoStr;
    }
  };

  const formatCurrency = (amount: number) => {
    return Number(amount || 0).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Operational Sales Receipt"
      footer={
        <div className="no-print" style={{ display: 'flex', gap: '10px' }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handlePrint}
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            <Printer size={16} />
            <span>Print Receipt</span>
          </button>
        </div>
      }
    >
      <div className="receipt-paper">
        <div className="receipt-header">
          <div className="receipt-shop-name">{receipt.shopName || 'Building Materials Shop'}</div>
          {receipt.shopAddress && <div>{receipt.shopAddress}</div>}
          {receipt.shopPhone && <div>Tel: {receipt.shopPhone}</div>}
          {receipt.shopEmail && <div>Email: {receipt.shopEmail}</div>}
        </div>

        <div className="receipt-meta-row">
          <span>Receipt #: <strong>{receipt.invoiceNumber}</strong></span>
          <span>Date: {formatDate(receipt.soldAt)}</span>
        </div>
        <div className="receipt-meta-row">
          <span>Cashier: {receipt.cashierName || 'Staff'}</span>
          <span>Status: <strong>{receipt.status}</strong></span>
        </div>
        {receipt.customerName && (
          <div className="receipt-meta-row">
            <span>Customer: {receipt.customerName}</span>
            {receipt.customerPhone && <span>{receipt.customerPhone}</span>}
          </div>
        )}

        <table className="receipt-table">
          <thead>
            <tr>
              <th>Item</th>
              <th style={{ textAlign: 'center' }}>Qty</th>
              <th style={{ textAlign: 'right' }}>Price</th>
              <th style={{ textAlign: 'right' }}>Total</th>
            </tr>
          </thead>
          <tbody>
            {receipt.items && receipt.items.map((item, idx) => (
              <tr key={idx}>
                <td>
                  <div style={{ fontWeight: 700 }}>{item.productName}</div>
                  <div style={{ fontSize: '10px', color: '#666' }}>SKU: {item.productSku}</div>
                </td>
                <td style={{ textAlign: 'center' }}>
                  {item.quantity} {item.unitCode}
                </td>
                <td style={{ textAlign: 'right' }}>{formatCurrency(item.unitPrice)}</td>
                <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatCurrency(item.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="receipt-totals">
          <div className="receipt-total-row">
            <span>Subtotal</span>
            <span>{formatCurrency(receipt.subtotal)}</span>
          </div>
          {Number(receipt.discountAmount) > 0 && (
            <div className="receipt-total-row" style={{ color: 'var(--danger)' }}>
              <span>Discount</span>
              <span>-{formatCurrency(receipt.discountAmount)}</span>
            </div>
          )}
          {Number(receipt.taxAmount) > 0 && (
            <div className="receipt-total-row">
              <span>Tax</span>
              <span>{formatCurrency(receipt.taxAmount)}</span>
            </div>
          )}
          <div className="receipt-total-row receipt-grand-total">
            <span>TOTAL</span>
            <span>{formatCurrency(receipt.totalAmount)}</span>
          </div>
        </div>

        {receipt.notes && (
          <div style={{ marginTop: '10px', fontSize: '11px', color: '#555', fontStyle: 'italic' }}>
            Note: {receipt.notes}
          </div>
        )}

        {receipt.status === 'VOIDED' && (
          <div
            style={{
              marginTop: '12px',
              padding: '6px',
              background: '#fcedeb',
              color: '#b83a3a',
              borderRadius: '4px',
              textAlign: 'center',
              fontWeight: 700,
              fontSize: '11px',
            }}
          >
            VOIDED on {formatDate(receipt.voidedAt)}: {receipt.voidReason || 'No reason provided'}
          </div>
        )}

        <div className="receipt-disclaimer">
          {receipt.disclaimer || 'Operational Sales Record - Not a Proof of Payment'}
        </div>
      </div>
    </Modal>
  );
};
