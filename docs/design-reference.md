# Kuter — Design Reference & System Analysis

## 1. Design Archive Inspection

- **Source Archive:** `Multi-Tenant Inventory Management SaaS.zip`
- **Archive Contents:** Interactive React + Vite prototype export produced within Figma Make (`src/App.tsx`, `src/index.css`, `public/favicon.svg`, `vite.config.ts`, `package.json`, `AGENTS.md`).
- **Target Reference Directory:** `docs/design-reference/`

The archive is not merely static wireframes or image screenshots; it provides a comprehensive 6,500-line single-file interactive prototype demonstrating complete user interactions, state transitions, navigation flows, and precise CSS design tokens.

---

## 2. Design Tokens & Visual Hierarchy

### A. Color Palette

| Token | Hex Value | Semantic Role |
| :--- | :--- | :--- |
| `--ink` | `#102a3a` | Primary brand deep navy, dark headers, dark sidebar backgrounds |
| `--ink-2` | `#183e51` | Secondary dark blue, card backgrounds, hover states |
| `--teal` | `#16756c` | Primary brand action color, active tabs, primary buttons |
| `--teal-dark` | `#0f5d56` | Primary hover state, active focus borders |
| `--teal-soft` | `#e4f3f0` | Subtle teal tint for icon containers, selected rows |
| `--cream` | `#f6f4ed` | Warm background for public landing page and marketing hero |
| `--paper` | `#ffffff` | Clean white card surfaces, modals, popovers |
| `--canvas` | `#f4f6f5` | Neutral cool background for authenticated app dashboards |
| `--text` | `#172b35` | High-contrast body typography |
| `--muted` | `#64747c` | Secondary labels, descriptions, table subtitles |
| `--faint` | `#8e9aa0` | Tertiary placeholders, timestamp text |
| `--border` | `#dce3e2` | Default subtle card and table row border |
| `--border-strong` | `#cbd5d3` | Form input borders, active divider lines |
| `--success` / `--success-bg` | `#16724d` / `#e8f5ee` | In-stock badges, completed migrations, active tenants |
| `--warning` / `--warning-bg` | `#a75b09` / `#fff2dc` | Low-stock alerts, pending approvals, trial status |
| `--danger` / `--danger-bg` | `#b83a3a` / `#fcedeb` | Out-of-stock badges, rejected requests, errors |
| `--blue` | `#356a8b` | Informational callouts and reference badges |

### B. Typography

- **Headings & Brand:** `Manrope, sans-serif` (weights 500, 600, 700, 800)
  - Used for application headers, logo, modal titles, KPI numeric values, and hero titles.
  - Distinctive character: tight letter spacing (`letter-spacing: -1px` to `-3px`), geometric numerals.
- **Body & Controls:** `DM Sans, sans-serif` (weights 400, 500, 600, 700)
  - Used for body copy, table contents, form inputs, button labels, and secondary navigation.

### C. Spacing & Elevation

- **Radii:**
  - `--radius-sm: 7px` (buttons, input fields, badges)
  - `--radius: 11px` (cards, containers, dialogs)
  - `--radius-lg: 18px` (hero preview containers, modal surfaces)
- **Shadows:**
  - `--shadow-sm: 0 1px 2px rgba(16, 42, 58, 0.06)`
  - `--shadow-md: 0 14px 36px rgba(16, 42, 58, 0.12)`
  - `--shadow-lg: 0 20px 48px rgba(16, 42, 58, 0.18)`

### D. Brand Logo Mark

The Kuter logo consists of an angled teal container (`transform: skewY(-3deg)`) holding three vertical white bars with ascending heights (55%, 100%, 75%), representing stacked building material slabs or inventory stacks.

---

## 3. Application Areas Identified

### Area 1: Public Website & Onboarding

- **Target Audience:** Hardware store owners, building material distributors, yard managers.
- **Key Views:**
  1. **Landing Hero (`home`):** Problem statements specific to building materials (bulk cement, structural steel rebar, timber, paint), live inventory preview, trust indicators.
  2. **Feature Deep Dive (`features`):** Dual units of measure (e.g. 50kg bags vs metric tonnes), stock receipt validation, ledger audit trail.
  3. **Pricing & Plans (`pricing`):** Starter ($49/mo), Professional ($119/mo, recommended), Enterprise ($279/mo) with product and user quotas.
  4. **Registration Request (`register`):** Multi-step tenant application capturing shop name, trade license, owner contact, and selected tier.
  5. **System Operational Status (`status`):** Public transparency page reporting API readiness, database state, and active Spring Boot profiles.
  6. **Authentication Entry (`login`):** Multi-tenant login requiring tenant shop identifier or user email.

### Area 2: Shop Tenant Portal (e.g., Al-Madina Materials)

- **Target Audience:** Yard owners, shop managers, storekeepers, cashiers.
- **Key Modules:**
  1. **Dashboard:** KPI summary (active SKUs, low-stock alerts, daily deliveries, counter sales volume).
  2. **Products & Materials Catalog:** Dual-unit SKU definitions, brand/manufacturer metadata, reorder thresholds, barcode scanning.
  3. **Receive Stock:** Inbound supplier shipment logging, delivery note attachment, conversion factor verification.
  4. **Stock Adjustments & Damage Audits:** Breakage, spillage, shrinkage logging with mandatory reason codes.
  5. **Stock Movement Ledger:** Append-only chronological transaction audit trail.
  6. **Reports & Exports:** Valuation, aging stock, inventory turnover.
  7. **Shop Settings & Roles:** Role-based access control (`OWNER`, `MANAGER`, `STOREKEEPER`, `VIEWER`).

### Area 3: Platform Administration Portal

- **Target Audience:** Kuter SaaS platform operators and super-administrators.
- **Key Modules:**
  1. **Platform Overview:** Global tenant counts, active MRR, pending registration queue.
  2. **Registration Requests:** Review queue for new shop registrations with actions: *Approve*, *Reject*, *Request More Info*.
  3. **Shops / Tenants Directory:** Tenant status management, domain assignment, suspension/activation switches.
  4. **Plans & Subscription Management:** Tier quotas (product limits, user seats, branch counts, pricing).
  5. **Platform Users:** Internal administrative accounts and security permissions.
  6. **System Audit Logs:** Immutable event stream recording administrative actions and security changes.

---

## 4. Reusable Component Conventions

1. **Button Hierarchy:**
   - `.btn-primary`: Teal background (`--teal`), white text, subtle hover lift.
   - `.btn-secondary`: White surface, border (`--border-strong`), dark ink text.
   - `.btn-ghost`: Borderless, subtle dark hover tint.
   - `.btn-danger`: Red text (`--danger`), light pink border.
2. **Badges:**
   - Pill-shaped (`border-radius: 99px`), 11px uppercase bold, includes a 6px dot indicator (`.badge-dot`).
   - Variants: `.badge-success` (green), `.badge-warning` (amber), `.badge-danger` (red), `.badge-neutral` (gray).
3. **Sidebar Navigation:**
   - Dark navy surface (`--ink`), active state highlighting with primary teal background, icon + text layout.
4. **Cards & Surface Containers:**
   - White background (`--paper`), 1px solid border (`--border`), 11px border radius (`--radius`).

---

## 5. Identified Prototype Limitations & Gaps

1. **Single-File Monolithic Prototype:** The Figma export bundled all screens and state into a single 6,500-line `App.tsx`. In Kuter, this is decomposed into clean modular React applications (`admin/` and `frontend/`).
2. **Mock In-Memory Storage:** The prototype used hardcoded React state rather than REST API persistence. Phase One establishes the typed API client (`api/client.ts`) and Spring Boot endpoints.
3. **Asset References:** Hero material photography in the prototype relied on Unsplash URLs. Production builds should bundle local optimized SVG/WebP assets.
4. **Authentication Flow:** The prototype mocked authentication with a simple state switch without JWT tokens or Spring Security integration.
