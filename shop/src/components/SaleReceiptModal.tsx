import React from 'react';
import { Printer } from 'lucide-react';
import { Modal } from './Modal';
import { SaleReceiptResponseDto } from '../api/types';
import { useLanguage } from '../i18n/LanguageContext';

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
  const { t, formatDate, language } = useLanguage();

  if (!receipt) return null;

  const handlePrint = () => {
    window.print();
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
      title={t('receipt.title')}
      footer={
        <div className="no-print" style={{ display: 'flex', gap: '10px' }}>
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            {t('common.close')}
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handlePrint}
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          >
            <Printer size={16} />
            <span>{t('receipt.print')}</span>
          </button>
        </div>
      }
    >
      <div className="receipt-paper">
        <div className="receipt-header">
          <div className="receipt-shop-name">{receipt.shopName || (language === 'am' ? 'የህንጻ እቃዎች ሱቅ' : 'Building Materials Shop')}</div>
          {receipt.shopAddress && <div>{receipt.shopAddress}</div>}
          {receipt.shopPhone && <div>{language === 'am' ? 'ስልክ' : 'Tel'}: {receipt.shopPhone}</div>}
          {receipt.shopEmail && <div>{language === 'am' ? 'ኢሜይል' : 'Email'}: {receipt.shopEmail}</div>}
        </div>

        <div className="receipt-meta-row">
          <span>{t('receipt.receiptNo')}: <strong>{receipt.invoiceNumber}</strong></span>
          <span>{t('receipt.date')}: {formatDate(receipt.soldAt)}</span>
        </div>
        <div className="receipt-meta-row">
          <span>{t('receipt.servedBy')}: {receipt.cashierName || (language === 'am' ? 'ሰራተኛ' : 'Staff')}</span>
          <span>{t('common.status')}: <strong>{receipt.status === 'COMPLETED' ? t('status.COMPLETED') : receipt.status === 'VOIDED' ? t('status.VOIDED') : receipt.status}</strong></span>
        </div>
        {receipt.customerName && (
          <div className="receipt-meta-row">
            <span>{t('receipt.customer')}: {receipt.customerName}</span>
            {receipt.customerPhone && <span>{receipt.customerPhone}</span>}
          </div>
        )}

        <table className="receipt-table">
          <thead>
            <tr>
              <th>{t('receipt.items')}</th>
              <th style={{ textAlign: 'center' }}>{t('receipt.qty')}</th>
              <th style={{ textAlign: 'right' }}>{t('receipt.price')}</th>
              <th style={{ textAlign: 'right' }}>{t('receipt.total')}</th>
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
                <td style={{ textAlign: 'right' }}>{formatCurrency(item.unitPrice)} {t('common.currencySymbol')}</td>
                <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatCurrency(item.lineTotal)} {t('common.currencySymbol')}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="receipt-totals">
          <div className="receipt-total-row">
            <span>{t('pos.subtotal')}</span>
            <span>{formatCurrency(receipt.subtotal)} {t('common.currencySymbol')}</span>
          </div>
          {Number(receipt.discountAmount) > 0 && (
            <div className="receipt-total-row" style={{ color: 'var(--danger)' }}>
              <span>{language === 'am' ? 'ቅናሽ' : 'Discount'}</span>
              <span>-{formatCurrency(receipt.discountAmount)} {t('common.currencySymbol')}</span>
            </div>
          )}
          {Number(receipt.taxAmount) > 0 && (
            <div className="receipt-total-row">
              <span>{language === 'am' ? 'ግብር (ቫት)' : 'Tax'}</span>
              <span>{formatCurrency(receipt.taxAmount)} {t('common.currencySymbol')}</span>
            </div>
          )}
          <div className="receipt-total-row receipt-grand-total">
            <span>{t('common.total')}</span>
            <span>{formatCurrency(receipt.totalAmount)} {t('common.currencySymbol')}</span>
          </div>
        </div>

        {receipt.notes && (
          <div style={{ marginTop: '10px', fontSize: '11px', color: '#555', fontStyle: 'italic' }}>
            {t('common.notes')}: {receipt.notes}
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
            {language === 'am' ? 'የተሰረዘበት ቀን ' : 'VOIDED on '}{formatDate(receipt.voidedAt)}: {receipt.voidReason || (language === 'am' ? 'ምንም ምክንያት አልተጠቀሰም' : 'No reason provided')}
          </div>
        )}

        <div className="receipt-disclaimer">
          {language === 'am' ? 'የስራ ላይ የሽያጭ ማስታወሻ ደረሰኝ — ለክፍያ ማረጋገጫነት ብቻ የሚያገለግል' : (receipt.disclaimer || 'Operational Sales Record - Not a Proof of Payment')}
        </div>
      </div>
    </Modal>
  );
};
