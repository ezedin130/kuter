import React, { useState, useEffect } from 'react';
import {
  Building2,
  Users,
  Save,
  RefreshCw,
  ShieldCheck,
  Plus,
  Edit,
  Trash2,
  UserCheck,
  UserX,
  KeyRound,
  Lock,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Alert } from '../components/Alert';
import { Modal } from '../components/Modal';
import { shopApi } from '../api/client';
import {
  ShopTenantDetail,
  TenantMemberDetailResponse,
  ShopPermission,
  UserRole,
} from '../api/types';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';

const getStaffPermissions = (isAm: boolean): { key: ShopPermission; label: string; desc: string }[] => [
  { key: 'PRODUCTS_VIEW', label: isAm ? 'ምርቶችን መመልከት' : 'View Products', desc: isAm ? 'የካታሎግ ምርቶችን እና የመለኪያ አሃዶችን ማሰስ' : 'Browse catalog products and conversion units' },
  { key: 'PRODUCTS_MANAGE', label: isAm ? 'ምርቶችን ማስተዳደር' : 'Manage Products', desc: isAm ? 'የምርት መረጃዎችን መፍጠር እና ማሻሻል (የመሸጫ ዋጋን ሳይጨምር)' : 'Create and update product metadata (excl. selling prices)' },
  { key: 'INVENTORY_RECEIVE', label: isAm ? 'ዕቃዎችን መረከብ' : 'Receive Inventory', desc: isAm ? 'ከአቅራቢ የሚመጡ ዕቃዎችን እና ደረሰኞችን መመዝገብ' : 'Record incoming supplier shipments and delivery notes' },
  { key: 'INVENTORY_ADJUST', label: isAm ? 'ዕቃ ማስተካከል' : 'Adjust Inventory', desc: isAm ? 'ተጠያቂነት ያለው የክምችት ማስተካከያ በምክንያት መመዝገብ' : 'Record accountable stock adjustments with reasons' },
  { key: 'INVENTORY_VIEW', label: isAm ? 'ክምችት መመልከት' : 'View Inventory', desc: isAm ? 'ያለውን ክምችት እና የእንቅስቃሴ መዝገብ መመርመር' : 'Inspect stock on hand and ledger movement logs' },
  { key: 'SALES_CREATE', label: isAm ? 'የሽያጭ ግብይት ማከናወን' : 'Create POS Sales', desc: isAm ? 'የመሸጫ ጠረጴዛ የሽያጭ ግብይቶችን ማካሄድ' : 'Process counter checkout sales transactions' },
  { key: 'SALES_VIEW', label: isAm ? 'የሽያጭ ታሪክ መመልከት' : 'View Sales History', desc: isAm ? 'የሽያጭ መዝገቦችን እና የደንበኛ ደረሰኞችን መመርመር' : 'Inspect sales logs and customer receipts' },
  { key: 'SALES_REPORTS_VIEW', label: isAm ? 'የሽያጭ ሪፖርቶችን መመልከት' : 'View Sales Reports', desc: isAm ? 'የአሠራር ሽያጮችን እና የቀን ማጠቃለያዎችን ማግኘት' : 'Access operational sales and daily summaries' },
  { key: 'LENDING_REQUEST', label: isAm ? 'ውሰት/ብድር መመዝገብ' : 'Record Lending/Borrowing', desc: isAm ? 'የተወሰዱ እና የተሰጡ የዕቃ ውሰቶችን መመዝገብ' : 'Create incoming borrowings and outgoing loans' },
  { key: 'LENDING_MANAGE', label: isAm ? 'የውሰት መልሶ ማስተዳደር' : 'Manage Lending Returns', desc: isAm ? 'ከፊል/ሙሉ መልሶችን መመዝገብ እና ውሰቶችን መሰረዝ' : 'Record partial/complete returns and cancel loans' },
  { key: 'LENDING_VIEW', label: isAm ? 'የውሰት ታሪክ መመልከት' : 'View Lending History', desc: isAm ? 'የውሰት መዝገብን እና ያሉ እዳዎችን መመርመር' : 'Inspect lending ledger and outstanding obligations' },
  { key: 'SUBSCRIPTION_VIEW', label: isAm ? 'የደንበኝነት ምዝገባን መመልከት' : 'View Subscriptions', desc: isAm ? 'የሱቁን ደረጃ እና የኮታ ገደቦችን መመርመር' : 'Inspect shop tier and quota limits' },
];

const DEFAULT_ROLE_PERMISSIONS: Record<UserRole, ShopPermission[]> = {
  OWNER: [
    'PRODUCTS_VIEW', 'PRODUCTS_MANAGE', 'PRICES_MANAGE', 'INVENTORY_RECEIVE', 'INVENTORY_ADJUST',
    'INVENTORY_VIEW', 'SALES_CREATE', 'SALES_VIEW', 'SALES_REPORTS_VIEW', 'USERS_MANAGE',
    'LENDING_REQUEST', 'LENDING_MANAGE', 'LENDING_VIEW', 'SUBSCRIPTION_VIEW',
  ],
  MANAGER: [
    'PRODUCTS_VIEW', 'PRODUCTS_MANAGE', 'INVENTORY_RECEIVE', 'INVENTORY_ADJUST',
    'INVENTORY_VIEW', 'SALES_CREATE', 'SALES_VIEW', 'SALES_REPORTS_VIEW',
    'LENDING_REQUEST', 'LENDING_MANAGE', 'LENDING_VIEW', 'SUBSCRIPTION_VIEW',
  ],
  STOREKEEPER: [
    'PRODUCTS_VIEW', 'INVENTORY_RECEIVE', 'INVENTORY_ADJUST', 'INVENTORY_VIEW',
    'LENDING_REQUEST', 'LENDING_MANAGE', 'LENDING_VIEW',
  ],
  CASHIER: [
    'PRODUCTS_VIEW', 'SALES_CREATE', 'SALES_VIEW',
  ],
};

export const ShopSettingsPage: React.FC = () => {
  const { canManageSettings, isOwner, user } = useAuth();
  const { t, language } = useLanguage();
  const isAm = language === 'am';

  const staffPermissionsList = getStaffPermissions(isAm);

  const [tenant, setTenant] = useState<ShopTenantDetail | null>(null);
  const [members, setMembers] = useState<TenantMemberDetailResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Form Fields
  const [name, setName] = useState<string>('');
  const [currency, setCurrency] = useState<string>('ETB');
  const [phone, setPhone] = useState<string>('');
  const [email, setEmail] = useState<string>('');
  const [address, setAddress] = useState<string>('');
  const [vatNumber, setVatNumber] = useState<string>('');

  // Add Member Modal State
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);
  const [newMemberEmail, setNewMemberEmail] = useState<string>('');
  const [newMemberName, setNewMemberName] = useState<string>('');
  const [newMemberPassword, setNewMemberPassword] = useState<string>('');
  const [newMemberPhone, setNewMemberPhone] = useState<string>('');
  const [newMemberRole, setNewMemberRole] = useState<UserRole>('CASHIER');
  const [newMemberPermissions, setNewMemberPermissions] = useState<Set<ShopPermission>>(
    new Set(DEFAULT_ROLE_PERMISSIONS.CASHIER)
  );
  const [isAddingMember, setIsAddingMember] = useState<boolean>(false);

  // Edit Member Modal State
  const [editingMember, setEditingMember] = useState<TenantMemberDetailResponse | null>(null);
  const [editRole, setEditRole] = useState<UserRole>('CASHIER');
  const [editPermissions, setEditPermissions] = useState<Set<ShopPermission>>(new Set());
  const [isSavingMember, setIsSavingMember] = useState<boolean>(false);

  // Transfer Ownership Modal State
  const [isTransferModalOpen, setIsTransferModalOpen] = useState<boolean>(false);
  const [transferTargetUserId, setTransferTargetUserId] = useState<string>('');
  const [currentOwnerPassword, setCurrentOwnerPassword] = useState<string>('');
  const [isTransferring, setIsTransferring] = useState<boolean>(false);

  const fetchShopDetails = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [tenantRes, staffRes] = await Promise.all([
        shopApi.getCurrentTenant(),
        shopApi.getTenantStaffMemberships(),
      ]);

      if (tenantRes.data) {
        const tData = tenantRes.data;
        setTenant(tData);
        setName(tData.name);
        setCurrency(tData.currency || 'ETB');
        setPhone(tData.phoneNumber || '');
        setEmail(tData.email || '');
        setAddress(tData.address || '');
        setVatNumber(tData.vatNumber || '');
      }

      if (staffRes.data) {
        setMembers(staffRes.data);
      }
    } catch (err: any) {
      setError(err.message || (isAm ? 'የሱቅ ዝርዝሮችን መጫን አልተቻለም።' : 'Failed to load shop details.'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchShopDetails();
  }, []);

  const activeSeats = members.filter((m) => m.status === 'ACTIVE').length;
  const isSeatLimitReached = activeSeats >= 4;

  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError(isAm ? 'የሱቅ ስም ያስፈልጋል።' : 'Shop name is required.');
      return;
    }

    setIsSaving(true);
    setError(null);
    setSuccessMsg(null);

    try {
      const res = await shopApi.updateCurrentTenant({
        name: name.trim(),
        currency: currency.trim(),
        timezone: 'UTC',
        phoneNumber: phone.trim() || undefined,
        email: email.trim() || undefined,
        address: address.trim() || undefined,
        vatNumber: vatNumber.trim() || undefined,
      });

      setTenant(res.data);
      setSuccessMsg(isAm ? 'የሱቅ ቅንብሮች በተሳካ ሁኔታ ተሻሽለዋል!' : 'Shop settings updated successfully!');
    } catch (err: any) {
      setError(err.message || (isAm ? 'የሱቅ ቅንብሮችን ማስቀመጥ አልተቻለም።' : 'Failed to save shop settings.'));
    } finally {
      setIsSaving(false);
    }
  };

  // Add Member Handlers
  const handleOpenAddModal = () => {
    setNewMemberEmail('');
    setNewMemberName('');
    setNewMemberPassword('');
    setNewMemberPhone('');
    setNewMemberRole('CASHIER');
    setNewMemberPermissions(new Set(DEFAULT_ROLE_PERMISSIONS.CASHIER));
    setIsAddModalOpen(true);
  };

  const handleRoleChangeForNew = (role: UserRole) => {
    setNewMemberRole(role);
    setNewMemberPermissions(new Set(DEFAULT_ROLE_PERMISSIONS[role].filter((p) => p !== 'PRICES_MANAGE' && p !== 'USERS_MANAGE')));
  };

  const togglePermissionForNew = (perm: ShopPermission) => {
    setNewMemberPermissions((prev) => {
      const next = new Set(prev);
      if (next.has(perm)) {
        next.delete(perm);
      } else {
        next.add(perm);
      }
      return next;
    });
  };

  const handleAddMemberSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMemberEmail.trim() || !newMemberName.trim()) {
      setError(isAm ? 'የሠራተኛ ኢሜይል እና ሙሉ ስም ያስፈልጋል።' : 'Staff email and full name are required.');
      return;
    }
    if (!newMemberPassword || newMemberPassword.length < 6) {
      setError(isAm ? 'የመጀመሪያው የይለፍ ቃል ቢያንስ 6 ፊደላት መሆን አለበት።' : 'Initial password must be at least 6 characters.');
      return;
    }

    setIsAddingMember(true);
    setError(null);
    try {
      await shopApi.addTenantMember({
        email: newMemberEmail.trim(),
        fullName: newMemberName.trim(),
        role: newMemberRole,
        password: newMemberPassword.trim(),
        phoneNumber: newMemberPhone.trim() || undefined,
        permissions: Array.from(newMemberPermissions),
      });
      setIsAddModalOpen(false);
      setSuccessMsg(isAm ? `ሠራተኛ ${newMemberName} በተሳካ ሁኔታ ተጨምሯል።` : `Staff member ${newMemberName} added successfully.`);
      await fetchShopDetails();
    } catch (err: any) {
      setError(err.message || (isAm ? 'ሠራተኛ ማከል አልተቻለም።' : 'Failed to add staff member.'));
    } finally {
      setIsAddingMember(false);
    }
  };

  // Edit Member Handlers
  const handleOpenEditModal = (m: TenantMemberDetailResponse) => {
    setEditingMember(m);
    setEditRole(m.role);
    setEditPermissions(new Set(m.permissions || DEFAULT_ROLE_PERMISSIONS[m.role]));
  };

  const handleRoleChangeForEdit = (role: UserRole) => {
    setEditRole(role);
    setEditPermissions(new Set(DEFAULT_ROLE_PERMISSIONS[role].filter((p) => p !== 'PRICES_MANAGE' && p !== 'USERS_MANAGE')));
  };

  const togglePermissionForEdit = (perm: ShopPermission) => {
    setEditPermissions((prev) => {
      const next = new Set(prev);
      if (next.has(perm)) {
        next.delete(perm);
      } else {
        next.add(perm);
      }
      return next;
    });
  };

  const handleSaveMemberPermissions = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMember) return;

    setIsSavingMember(true);
    setError(null);
    try {
      await shopApi.updateTenantMember(editingMember.id, {
        role: editRole,
        permissions: Array.from(editPermissions),
      });
      setEditingMember(null);
      setSuccessMsg(isAm ? `ለ ${editingMember.fullName} ፈቃዶች ተሻሽለዋል።` : `Permissions updated for ${editingMember.fullName}.`);
      await fetchShopDetails();
    } catch (err: any) {
      setError(err.message || (isAm ? 'የአባል ፈቃዶችን ማሻሻል አልተቻለም።' : 'Failed to update member permissions.'));
    } finally {
      setIsSavingMember(false);
    }
  };

  // Toggle Member Status (Activate / Suspend)
  const handleToggleStatus = async (m: TenantMemberDetailResponse) => {
    if (m.role === 'OWNER') return;
    const newStatus = m.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';

    if (newStatus === 'ACTIVE' && isSeatLimitReached) {
      setError(isAm ? 'ሠራተኛውን ማንቃት አይቻልም: ሱቁ ከፍተኛውን 4 ገቢር መለያዎች ገደብ ደርሷል። እባክዎ መጀመሪያ ሌላ መለያ ያቦዝኑ።' : 'Cannot activate member: Shop has reached the maximum limit of 4 active accounts. Please deactivate another account first.');
      return;
    }

    setError(null);
    try {
      await shopApi.updateTenantMemberStatus(m.id, newStatus);
      setSuccessMsg(isAm ? `የሠራተኛ ሁኔታ ወደ ${newStatus} ተቀይሯል።` : `Staff member status changed to ${newStatus}.`);
      await fetchShopDetails();
    } catch (err: any) {
      setError(err.message || (isAm ? 'የአባል ሁኔታን ማሻሻል አልተቻለም።' : 'Failed to update member status.'));
    }
  };

  // Remove Member
  const handleDeleteMember = async (m: TenantMemberDetailResponse) => {
    if (m.role === 'OWNER') {
      alert(isAm ? 'የሱቅ ባለቤት መለያ ሊሰረዝ አይችልም።' : 'The shop owner account cannot be deleted.');
      return;
    }
    const confirmPrompt = isAm
      ? `እርግጠኛ ነዎት ሠራተኛ ${m.fullName || m.email} ከዚህ ሱቅ ማስወገድ ይፈልጋሉ?`
      : `Are you sure you want to remove staff member ${m.fullName || m.email} from this shop?`;
    if (!window.confirm(confirmPrompt)) {
      return;
    }

    setError(null);
    try {
      await shopApi.removeTenantMember(m.id);
      setSuccessMsg(isAm ? `ሠራተኛ ${m.fullName || m.email} ከሱቅ ተወግዷል።` : `Staff member ${m.fullName || m.email} removed from shop.`);
      await fetchShopDetails();
    } catch (err: any) {
      setError(err.message || (isAm ? 'ሠራተኛውን ማስወገድ አልተቻለም።' : 'Failed to remove staff member.'));
    }
  };

  // Transfer Ownership Handler
  const handleTransferOwnershipSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!transferTargetUserId || !currentOwnerPassword) {
      setError(isAm ? 'እባክዎ ተቀባይ ተጠቃሚ ይምረጡ እና አሁን ያለዎትን የባለቤት የይለፍ ቃል ያረጋግጡ።' : 'Please select a target user and confirm your current owner password.');
      return;
    }

    const confirmTransferPrompt = isAm
      ? 'ወሳኝ እርምጃ: የዚህን ሱቅ ባለቤትነት ማስተላለፍ እንደሚፈልጉ እርግጠኛ ነዎት? የእርስዎ መለያ ወደ MANAGER ዝቅ ይላል፣ እና ሙሉ የ OWNER መብቶች ይተላለፋሉ።'
      : 'CRITICAL ACTION: Are you sure you want to transfer ownership of this shop? Your account will be demoted to MANAGER, and full OWNER privileges will be transferred.';

    if (!window.confirm(confirmTransferPrompt)) {
      return;
    }

    setIsTransferring(true);
    setError(null);
    try {
      await shopApi.transferOwnership({
        newOwnerUserId: transferTargetUserId,
        currentOwnerPassword,
      });
      setIsTransferModalOpen(false);
      setCurrentOwnerPassword('');
      setSuccessMsg(isAm ? 'የሱቅ ባለቤትነት በተሳካ ሁኔታ ተላልፏል። ገጹ እንደገና እየተጫነ ነው...' : 'Shop ownership transferred successfully. Reloading workspace...');
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } catch (err: any) {
      setError(err.message || (isAm ? 'ባለቤትነትን ማስተላለፍ አልተቻለም።' : 'Failed to transfer ownership.'));
    } finally {
      setIsTransferring(false);
    }
  };

  const eligibleNewOwners = members.filter((m) => m.status === 'ACTIVE' && m.role !== 'OWNER' && m.userId !== user?.id);

  return (
    <div>
      <PageHead
        eyebrow={isAm ? 'የሱቅ አስተዳደር' : 'Shop Administration'}
        title={t.settings.title}
        subtitle={t.settings.subtitle}
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={fetchShopDetails}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
            <span>{t.common.refresh}</span>
          </button>
        }
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}
      {successMsg && <Alert type="success" message={successMsg} onDismiss={() => setSuccessMsg(null)} />}

      <div className="settings-layout-grid">
        {/* Left Column: Shop Profile Form */}
        <div className="card">
          <h2 className="card-title" style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Building2 size={18} color="var(--teal)" />
            <span>{t.settings.shopProfile}</span>
          </h2>

          <form onSubmit={handleSaveSettings} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">{t.settings.shopCode}</label>
                <input
                  type="text"
                  className="form-input"
                  value={tenant?.code || ''}
                  disabled
                  style={{ background: '#f5f7f8', color: 'var(--muted)' }}
                />
                <span className="form-hint">{isAm ? 'ልዩ የሱቅ መለያ (በስርዓቱ የማይቀየር)' : 'Unique tenant slug (system immutable)'}</span>
              </div>

              <div className="form-group">
                <label className="form-label">{t.settings.shopName} *</label>
                <input
                  type="text"
                  className="form-input"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  disabled={!canManageSettings}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">{t.settings.currency}</label>
              <input
                type="text"
                className="form-input"
                value={isAm ? 'የኢትዮጵያ ብር (ETB — ብር)' : 'Ethiopian Birr (ETB — Br)'}
                disabled
                readOnly
              />
              <span style={{ fontSize: '11.5px', color: 'var(--muted)', marginTop: '4px', display: 'block' }}>
                {isAm
                  ? 'ሁሉም የገንዘብ ሚዛኖች፣ የክምችት ወጪ፣ ሽያጮች እና ሪፖርቶች የሚሠሩት በኢትዮጵያ ብር (ETB) ብቻ ነው።'
                  : 'All monetary balances, inventory costing, sales, and reports operate exclusively in Ethiopian Birr (ETB).'}
              </span>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">{t.settings.phone}</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="+251 98 211 2233"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  disabled={!canManageSettings}
                />
              </div>

              <div className="form-group">
                <label className="form-label">{isAm ? 'የእውቂያ ኢሜይል' : 'Contact Email'}</label>
                <input
                  type="email"
                  className="form-input"
                  placeholder="contact@shop.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={!canManageSettings}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">{isAm ? 'የተጨማሪ እሴት ታክስ (VAT) / የግብር መለያ ቁጥር (TIN)' : 'VAT / Tax Identification Number'}</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. VAT-39281928"
                value={vatNumber}
                onChange={(e) => setVatNumber(e.target.value)}
                disabled={!canManageSettings}
              />
            </div>

            <div className="form-group">
              <label className="form-label">{t.settings.address}</label>
              <textarea
                className="form-textarea"
                placeholder={isAm ? 'የመንገድ አድራሻ፣ ከተማ፣ ክፍለ ከተማ...' : 'Street address, city, district...'}
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                disabled={!canManageSettings}
              />
            </div>

            {canManageSettings && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSaving}
                  style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
                >
                  <Save size={16} />
                  <span>{isSaving ? (isAm ? 'ለውጦችን በማስቀመጥ ላይ...' : 'Saving Changes...') : t.common.save}</span>
                </button>
              </div>
            )}
          </form>
        </div>

        {/* Right Column: Staff Team Memberships & 4-Seat Limit */}
        <div className="card">
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
            <div>
              <h2 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Users size={18} color="var(--teal)" />
                <span>{isAm ? 'የሱቅ ቡድን እና ፈቃዶች' : 'Shop Team & Permissions'}</span>
              </h2>
              <p style={{ fontSize: '12.5px', color: 'var(--muted)', margin: '4px 0 0 0' }}>
                {isAm
                  ? 'በአንድ ሱቅ ቢበዛ 4 መለያዎች (1 ባለቤት + እስከ 3 ሠራተኞች)።'
                  : 'Maximum 4 accounts per shop (1 Owner + up to 3 Staff).'}
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' }}>
              <span
                className={`badge ${isSeatLimitReached ? 'badge-warning' : 'badge-neutral'}`}
                style={{ fontSize: '11px', fontWeight: 700 }}
              >
                {isAm ? `${activeSeats} / 4 መቀመጫዎች ሥራ ላይ` : `${activeSeats} / 4 Seats In Use`}
              </span>
              {isOwner && (
                <div style={{ display: 'flex', gap: '6px' }}>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={handleOpenAddModal}
                    disabled={isSeatLimitReached}
                    title={isSeatLimitReached ? (isAm ? 'ሱቁ ከፍተኛውን 4 ገቢር መለያዎች ገደብ ደርሷል' : 'Shop has reached its maximum 4 active accounts limit') : (isAm ? 'አዲስ ሠራተኛ ይጨምሩ' : 'Add a new staff member')}
                  >
                    <Plus size={14} />
                    <span>{t.settings.addStaff}</span>
                  </button>
                  {eligibleNewOwners.length > 0 && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => setIsTransferModalOpen(true)}
                      title={isAm ? 'የሱቅ ባለቤትነትን ለሌላ ገቢር የቡድን አባል ያስተላልፉ' : 'Transfer shop ownership to another active team member'}
                    >
                      <KeyRound size={14} />
                      <span>{t.settings.transferOwnership}</span>
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="table-wrap" style={{ marginTop: '16px' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>{isAm ? 'የቡድን አባል' : 'Team Member'}</th>
                  <th>{t.settings.role}</th>
                  <th>{t.common.status}</th>
                  {isOwner && <th style={{ textAlign: 'right' }}>{t.common.actions}</th>}
                </tr>
              </thead>
              <tbody>
                {members.length === 0 ? (
                  <tr>
                    <td colSpan={isOwner ? 4 : 3} style={{ textAlign: 'center', padding: '24px', color: 'var(--muted)' }}>
                      {isAm ? 'ምንም ሠራተኞች አልተገኙም።' : 'No staff members found.'}
                    </td>
                  </tr>
                ) : (
                  members.map((m) => {
                    const isMemberOwner = m.role === 'OWNER';
                    return (
                      <tr key={m.id}>
                        <td>
                          <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <ShieldCheck size={14} color={isMemberOwner ? 'var(--primary)' : 'var(--teal)'} />
                            <span>{m.fullName || m.email}</span>
                            {isMemberOwner && (
                              <span className="badge badge-primary" style={{ fontSize: '10px', padding: '1px 6px' }}>
                                {isAm ? 'ባለቤት' : 'Owner'}
                              </span>
                            )}
                          </div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>
                            {m.email} {m.phoneNumber ? `• ${m.phoneNumber}` : ''}
                          </div>
                          {/* Permissions summary */}
                          {!isMemberOwner && m.permissions && m.permissions.length > 0 && (
                            <div style={{ fontSize: '10px', color: 'var(--muted)', marginTop: '4px' }}>
                              {isAm ? `ፈቃዶች: ${m.permissions.length} ተሰጥተዋል` : `Permissions: ${m.permissions.length} granted`}
                            </div>
                          )}
                        </td>
                        <td>
                          <span style={{ fontSize: '12px', fontWeight: 600 }}>{m.role}</span>
                        </td>
                        <td>
                          <StatusBadge status={m.status} />
                        </td>
                        {isOwner && (
                          <td style={{ textAlign: 'right' }}>
                            {!isMemberOwner && (
                              <div style={{ display: 'inline-flex', gap: '4px' }}>
                                <button
                                  type="button"
                                  className="btn-icon"
                                  title={isAm ? 'ሚና እና ፈቃዶችን ያርትዑ' : 'Edit Role & Permissions'}
                                  onClick={() => handleOpenEditModal(m)}
                                >
                                  <Edit size={15} />
                                </button>
                                <button
                                  type="button"
                                  className="btn-icon"
                                  title={isAm ? (m.status === 'ACTIVE' ? 'የሠራተኛ መለያን አግድ' : 'የሠራተኛ መለያን ዳግም አንቃ') : (m.status === 'ACTIVE' ? 'Suspend Staff Account' : 'Reactivate Staff Account')}
                                  onClick={() => handleToggleStatus(m)}
                                >
                                  {m.status === 'ACTIVE' ? (
                                    <UserX size={15} color="var(--amber)" />
                                  ) : (
                                    <UserCheck size={15} color="var(--teal)" />
                                  )}
                                </button>
                                <button
                                  type="button"
                                  className="btn-icon"
                                  title={isAm ? 'የሠራተኛ መለያ ሰርዝ' : 'Delete Staff Account'}
                                  onClick={() => handleDeleteMember(m)}
                                >
                                  <Trash2 size={15} color="var(--danger)" />
                                </button>
                              </div>
                            )}
                          </td>
                        )}
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add Staff Member Modal */}
      {isAddModalOpen && (
        <Modal
          isOpen={true}
          title={isAm ? 'አዲስ ሠራተኛ ጨምር (የ 4 መቀመጫ ገደብ)' : 'Add Staff Member (4-Seat Shop Limit)'}
          onClose={() => setIsAddModalOpen(false)}
          size="large"
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsAddModalOpen(false)}
                disabled={isAddingMember}
              >
                {t.common.cancel}
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleAddMemberSubmit}
                disabled={isAddingMember || !newMemberEmail.trim() || !newMemberName.trim() || !newMemberPassword.trim() || newMemberPassword.length < 6}
              >
                {isAddingMember ? (isAm ? 'በመጨመር ላይ...' : 'Adding...') : (isAm ? 'ሠራተኛ ጨምር' : 'Add Staff Member')}
              </button>
            </div>
          }
        >
          <form onSubmit={handleAddMemberSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">{isAm ? 'ሙሉ ስም *' : 'Full Name *'}</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder={isAm ? 'ለምሳሌ ዳዊት በቀለ' : 'e.g. David Mwangi'}
                  value={newMemberName}
                  onChange={(e) => setNewMemberName(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">{isAm ? 'የኢሜይል አድራሻ *' : 'Email Address *'}</label>
                <input
                  type="email"
                  className="form-input"
                  placeholder="e.g. staff@shop.example"
                  value={newMemberEmail}
                  onChange={(e) => setNewMemberEmail(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">{isAm ? 'ስልክ ቁጥር (አማራጭ)' : 'Phone Number (Optional)'}</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="+251 91 122 3344"
                  value={newMemberPhone}
                  onChange={(e) => setNewMemberPhone(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">{isAm ? 'የሠራተኛ ሚና *' : 'Shop Staff Role *'}</label>
                <select
                  className="form-select"
                  value={newMemberRole}
                  onChange={(e) => handleRoleChangeForNew(e.target.value as UserRole)}
                >
                  <option value="CASHIER">{isAm ? 'CASHIER (የሽያጭ ትኩረት)' : 'CASHIER (Counter Sales Focus)'}</option>
                  <option value="STOREKEEPER">{isAm ? 'STOREKEEPER (ርክክብ እና ውሰት ትኩረት)' : 'STOREKEEPER (Receiving & Lending Focus)'}</option>
                  <option value="MANAGER">{isAm ? 'MANAGER (አሠራር እና ክትትል ትኩረት)' : 'MANAGER (Operations & Supervision Focus)'}</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">{isAm ? 'የመጀመሪያ የይለፍ ቃል *' : 'Initial Password *'}</label>
              <input
                type="password"
                className="form-input"
                placeholder={isAm ? 'ቢያንስ 6 ፊደላት (ለምሳሌ Staff@123456)' : 'At least 6 characters (e.g. Staff@123456)'}
                value={newMemberPassword}
                onChange={(e) => setNewMemberPassword(e.target.value)}
                required
                minLength={6}
              />
              <span style={{ fontSize: '11.5px', color: 'var(--muted)', marginTop: '4px', display: 'block' }}>
                {isAm
                  ? 'ለዚህ ሠራተኛ የመጀመሪያ የይለፍ ቃል ያዘጋጁ። ለመግባት በዚህ ይጠቀማሉ እንዲሁም በማንኛውም ጊዜ ከላይኛው አሞሌ ሊቀይሩት ይችላሉ።'
                  : 'Set an initial password for this staff member. They will use this credential to log in and can change it anytime from the top bar.'}
              </span>
            </div>

            <div style={{ marginTop: '8px' }}>
              <label className="form-label" style={{ marginBottom: '8px', display: 'block' }}>
                {isAm ? 'ለሠራተኛው የተሰጡ ዝርዝር ፈቃዶች' : 'Granular Permissions Assigned to Staff Member'}
              </label>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                  gap: '10px',
                  maxHeight: '220px',
                  overflowY: 'auto',
                  border: '1px solid var(--border)',
                  padding: '12px',
                  borderRadius: 'var(--radius-md, 8px)',
                  background: 'var(--surface-sunken, #f8fafc)',
                }}
              >
                {staffPermissionsList.map((p) => {
                  const checked = newMemberPermissions.has(p.key);
                  return (
                    <label
                      key={p.key}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '8px',
                        cursor: 'pointer',
                        padding: '6px',
                        background: '#ffffff',
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => togglePermissionForNew(p.key)}
                        style={{ marginTop: '3px' }}
                      />
                      <div>
                        <div style={{ fontSize: '12px', fontWeight: 600 }}>{p.label}</div>
                        <div style={{ fontSize: '10.5px', color: 'var(--muted)' }}>{p.desc}</div>
                      </div>
                    </label>
                  );
                })}
              </div>

              {/* Owner Privileges Notice */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  marginTop: '8px',
                  fontSize: '11px',
                  color: 'var(--muted)',
                }}
              >
                <Lock size={12} color="var(--amber)" />
                <span>
                  <strong>{isAm ? 'የባለቤት ብቻ መብት:' : 'Owner Exclusive:'}</strong>{' '}
                  {isAm
                    ? 'የምርት መሸጫ ዋጋዎችን ማሻሻል እና የተጠቃሚ መለያዎችን ማስተዳደር ለሠራተኞች ሊሰጥ አይችልም።'
                    : 'Modifying product selling prices and managing user accounts cannot be assigned to staff members.'}
                </span>
              </div>
            </div>
          </form>
        </Modal>
      )}

      {/* Edit Staff Permissions Modal */}
      {editingMember && (
        <Modal
          isOpen={true}
          title={isAm ? `ፈቃዶችን ያርትዑ — ${editingMember.fullName || editingMember.email}` : `Edit Permissions — ${editingMember.fullName || editingMember.email}`}
          onClose={() => setEditingMember(null)}
          size="large"
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setEditingMember(null)}
                disabled={isSavingMember}
              >
                {t.common.cancel}
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSaveMemberPermissions}
                disabled={isSavingMember}
              >
                {isSavingMember ? (isAm ? 'በማስቀመጥ ላይ...' : 'Saving...') : (isAm ? 'ፈቃዶችን አሻሽል' : 'Update Permissions')}
              </button>
            </div>
          }
        >
          <form onSubmit={handleSaveMemberPermissions} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">{t.settings.role}</label>
              <select
                className="form-select"
                value={editRole}
                onChange={(e) => handleRoleChangeForEdit(e.target.value as UserRole)}
              >
                <option value="CASHIER">{isAm ? 'CASHIER (የሽያጭ ትኩረት)' : 'CASHIER (Counter Sales Focus)'}</option>
                <option value="STOREKEEPER">{isAm ? 'STOREKEEPER (ርክክብ እና ውሰት ትኩረት)' : 'STOREKEEPER (Receiving & Lending Focus)'}</option>
                <option value="MANAGER">{isAm ? 'MANAGER (አሠራር እና ክትትል ትኩረት)' : 'MANAGER (Operations & Supervision Focus)'}</option>
              </select>
            </div>

            <div>
              <label className="form-label" style={{ marginBottom: '8px', display: 'block' }}>
                {isAm ? 'ለሠራተኛው የተሰጡ ዝርዝር ፈቃዶች' : 'Granular Permissions Assigned to Staff Member'}
              </label>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                  gap: '10px',
                  maxHeight: '220px',
                  overflowY: 'auto',
                  border: '1px solid var(--border)',
                  padding: '12px',
                  borderRadius: 'var(--radius-md, 8px)',
                  background: 'var(--surface-sunken, #f8fafc)',
                }}
              >
                {staffPermissionsList.map((p) => {
                  const checked = editPermissions.has(p.key);
                  return (
                    <label
                      key={p.key}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '8px',
                        cursor: 'pointer',
                        padding: '6px',
                        background: '#ffffff',
                        border: '1px solid var(--border)',
                        borderRadius: '6px',
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => togglePermissionForEdit(p.key)}
                        style={{ marginTop: '3px' }}
                      />
                      <div>
                        <div style={{ fontSize: '12px', fontWeight: 600 }}>{p.label}</div>
                        <div style={{ fontSize: '10.5px', color: 'var(--muted)' }}>{p.desc}</div>
                      </div>
                    </label>
                  );
                })}
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  marginTop: '8px',
                  fontSize: '11px',
                  color: 'var(--muted)',
                }}
              >
                <Lock size={12} color="var(--amber)" />
                <span>
                  <strong>{isAm ? 'የባለቤት ብቻ መብት:' : 'Owner Exclusive:'}</strong>{' '}
                  {isAm
                    ? 'የመሸጫ ዋጋ ለውጦች እና የተጠቃሚ አስተዳደር ለሌላ ሊተላለፉ አይችሉም።'
                    : 'Selling price changes and user administration are strictly non-delegable.'}
                </span>
              </div>
            </div>
          </form>
        </Modal>
      )}

      {/* Transfer Shop Ownership Modal */}
      {isTransferModalOpen && (
        <Modal
          isOpen={true}
          title={isAm ? 'የሱቅ ባለቤትነትን ማስተላለፍ' : 'Transfer Shop Ownership'}
          onClose={() => setIsTransferModalOpen(false)}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsTransferModalOpen(false)}
                disabled={isTransferring}
              >
                {t.common.cancel}
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleTransferOwnershipSubmit}
                disabled={isTransferring || !transferTargetUserId || !currentOwnerPassword}
              >
                {isTransferring ? (isAm ? 'በማስተላለፍ ላይ...' : 'Transferring...') : (isAm ? 'ባለቤትነትን አስተላልፍ' : 'Transfer Ownership')}
              </button>
            </div>
          }
        >
          <form onSubmit={handleTransferOwnershipSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div
              style={{
                background: '#fff1f2',
                border: '1px solid #fecdd3',
                padding: '12px',
                borderRadius: '6px',
                color: '#9f1239',
                fontSize: '12.5px',
              }}
            >
              <strong>{isAm ? 'ማስጠንቀቂያ:' : 'Warning:'}</strong>{' '}
              {isAm
                ? 'የዚህን ሱቅ ዋና ባለቤትነት እያስተላለፉ ነው። የእርስዎ መለያ በራስ-ሰር ወደ MANAGER ዝቅ ይላል፣ እንዲሁም የመሸጫ ዋጋዎችን የመወሰን እና የቡድን አባላትን የማስተዳደር ብቸኛ መብትዎን ያስረክባሉ።'
                : 'You are transferring primary ownership of this shop. Your account will automatically be demoted to MANAGER, and you will surrender the exclusive ability to set selling prices and manage team memberships.'}
            </div>

            <div className="form-group">
              <label className="form-label">{isAm ? 'አዲሱን ባለቤት ይምረጡ (ገቢር የቡድን አባል) *' : 'Select New Owner (Active Team Member) *'}</label>
              <select
                className="form-select"
                value={transferTargetUserId}
                onChange={(e) => setTransferTargetUserId(e.target.value)}
                required
              >
                <option value="">{isAm ? '-- ገቢር የቡድን አባል ይምረጡ --' : '-- Choose active team member --'}</option>
                {eligibleNewOwners.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullName || m.email} ({m.email}) — {isAm ? 'የአሁኑ ሚና:' : 'Current Role:'} {m.role}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">{isAm ? 'የአሁኑ ባለቤት የይለፍ ቃል ማረጋገጫ *' : 'Current Owner Password Confirmation *'}</label>
              <input
                type="password"
                className="form-input"
                placeholder={isAm ? 'የአሁኑን የመለያዎን የይለፍ ቃል ያስገቡ' : 'Enter your current account password'}
                value={currentOwnerPassword}
                onChange={(e) => setCurrentOwnerPassword(e.target.value)}
                required
              />
              <span className="form-hint">{isAm ? 'ለባለቤትነት ዝውውር የደህንነት ማረጋገጫ ያስፈልጋል።' : 'Required for security verification of ownership transfers.'}</span>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
