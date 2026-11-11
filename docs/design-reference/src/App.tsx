import { createContext, useContext, useEffect, useMemo, useState } from "react"
import {
  AlertCircle,
  ArrowRight,
  BarChart3,
  Bell,
  Boxes,
  Building2,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  CircleDollarSign,
  ClipboardCheck,
  Clock3,
  FileBarChart,
  FileText,
  Grid2X2,
  HelpCircle,
  History,
  ImagePlus,
  LayoutDashboard,
  LockKeyhole,
  Menu,
  Minus,
  PackageOpen,
  Plus,
  ReceiptText,
  Search,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Store,
  Table2,
  TriangleAlert,
  Upload,
  UserRound,
  Users,
  Warehouse,
  X,
} from "lucide-react"

type Experience = "public" | "shop" | "admin"
type PublicPage = "home" | "features" | "pricing" | "register" | "status" | "login"
type ShopPage = "Dashboard" | "Products" | "Product form" | "Product detail" | "Categories" | "Inventory" | "Receive stock" | "Adjustments" | "Stock ledger" | "Reports" | "Users & roles" | "Subscription" | "Shop settings"
type AdminPage = "Overview" | "Registration requests" | "Shops / tenants" | "Plans" | "Subscriptions" | "Platform users" | "Audit logs" | "Settings"

type Product = {
  sku: string
  name: string
  category: string
  unit: string
  qty: number
  reorder: number
  maxStock?: number
  cost: number
  status: string
  description: string
  image?: string
  brand?: string
  barcode?: string
  active: boolean
  conversion?: { unit: string factor: number }
}

type RegistrationRequest = {
  ref: string
  shop: string
  owner: string
  plan: string
  date: string
  status: string
}

type Plan = {
  code: string
  name: string
  price: number | null
  interval: "Monthly" | "Yearly"
  description: string
  productLimit: number | null
  userLimit: number | null
  public: boolean
  active: boolean
  featured?: boolean
  features: string[]
}

type ShopUser = {
  id: string
  name: string
  email: string
  role: "OWNER" | "MANAGER" | "STOREKEEPER" | "VIEWER"
  status: "Active" | "Invitation pending" | "Disabled"
}

type Movement = {
  reference: string
  product: string
  type: string
  quantity: number
  unit: string
  cost: number
  user: string
  date: string
}

const initialProducts: Product[] = [
  {
    sku: "CEM-OPC-50",
    name: "Portland Cement 50 kg",
    category: "Cement",
    unit: "BAG",
    qty: 248,
    reorder: 80,
    cost: 8.4,
    status: "In stock",
    description:
      "General-purpose cement suitable for concrete, mortar, masonry, and common construction applications.",
    image:
      "https://images.unsplash.com/photo-1730627283177-f43b83c3850c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=82&w=800",
    brand: "Northland Materials",
    active: true,
    conversion: { unit: "KG", factor: 50 },
  },
  {
    sku: "STL-RB12",
    name: "Reinforcement Bar 12 mm",
    category: "Steel",
    unit: "PCS",
    qty: 86,
    reorder: 100,
    cost: 12.75,
    status: "Low stock",
    description:
      "Steel reinforcing bar used to strengthen concrete structures. 12 mm diameter, stocked and counted by piece.",
    image:
      "https://images.unsplash.com/photo-1623428454598-1bfe414bac03?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=82&w=800",
    active: true,
    conversion: { unit: "BUNDLE", factor: 10 },
  },
  {
    sku: "ROF-GI-3M",
    name: "Roofing Sheet 3 m",
    category: "Roofing",
    unit: "PCS",
    qty: 0,
    reorder: 24,
    cost: 24.5,
    status: "Out of stock",
    description:
      "Corrugated roofing sheet for general building roofs, stocked in standard three-metre sheets.",
    image:
      "https://images.unsplash.com/photo-1763926025678-95d196d0ab28?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=82&w=800",
    active: true,
  },
  {
    sku: "HDW-NL-3",
    name: "Common Nails 3 inch",
    category: "Hardware",
    unit: "PCS",
    qty: 740,
    reorder: 150,
    cost: 2.35,
    status: "In stock",
    description:
      "General-purpose steel nails for timber framing and common construction work.",
    image:
      "https://images.unsplash.com/photo-1745900435345-4ef41cf2c25e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=82&w=800",
    active: true,
    conversion: { unit: "BOX", factor: 200 },
  },
  {
    sku: "PLB-PVC20",
    name: "PVC Pipe 20 mm",
    category: "Plumbing",
    unit: "PCS",
    qty: 42,
    reorder: 30,
    cost: 18.5,
    status: "In stock",
    description:
      "PVC pipe for suitable plumbing and drainage applications. 20 mm nominal diameter.",
    image:
      "https://images.unsplash.com/photo-1654695376026-a4bec76a1081?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=82&w=800",
    active: true,
    conversion: { unit: "BUNDLE", factor: 12 },
  },
  {
    sku: "PNT-WHT-20",
    name: "Interior Wall Paint 20 L",
    category: "Paint",
    unit: "PAIL",
    qty: 18,
    reorder: 20,
    cost: 32,
    status: "Low stock",
    description:
      "Decorative interior wall coating in white with a smooth finish, supplied in a 20-litre pail.",
    image:
      "https://images.unsplash.com/photo-1763741226847-f5ef0c846506?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=82&w=800",
    active: true,
  },
]

const initialRequests: RegistrationRequest[] = [
  {
    ref: "REQ-2025-0184",
    shop: "Northstar Building Supply",
    owner: "Maya Okafor",
    plan: "Growth",
    date: "Today, 09:42",
    status: "Pending",
  },
  {
    ref: "REQ-2025-0183",
    shop: "Toma Hardware & Timber",
    owner: "David Toma",
    plan: "Starter",
    date: "Yesterday, 16:18",
    status: "More info",
  },
  {
    ref: "REQ-2025-0182",
    shop: "Lakeview Construction Mart",
    owner: "Amara K.",
    plan: "Growth",
    date: "May 18, 2025",
    status: "Pending",
  },
  {
    ref: "REQ-2025-0181",
    shop: "Westline Roofing Depot",
    owner: "S. Mensah",
    plan: "Scale",
    date: "May 17, 2025",
    status: "Approved",
  },
]

const initialPlans: Plan[] = [
  {
    code: "STARTER-M",
    name: "Starter",
    price: 29,
    interval: "Monthly",
    description: "For small shops organizing stock for the first time.",
    productLimit: 250,
    userLimit: 3,
    public: true,
    active: true,
    features: ["Core inventory", "Units & conversions", "Standard reports"],
  },
  {
    code: "GROWTH-M",
    name: "Growth",
    price: 59,
    interval: "Monthly",
    description: "For active materials shops with a growing team.",
    productLimit: 1000,
    userLimit: 10,
    public: true,
    active: true,
    featured: true,
    features: ["Everything in Starter", "Advanced reports", "Priority support"],
  },
  {
    code: "SCALE-M",
    name: "Scale",
    price: null,
    interval: "Monthly",
    description: "For high-volume shops needing more capacity.",
    productLimit: null,
    userLimit: null,
    public: false,
    active: true,
    features: ["Everything in Growth", "Custom limits", "Assisted onboarding"],
  },
]

const initialUsers: ShopUser[] = [
  {
    id: "u1",
    name: "Maya Okafor",
    email: "maya@northstar.example",
    role: "OWNER",
    status: "Active",
  },
  {
    id: "u2",
    name: "James Kato",
    email: "james@northstar.example",
    role: "STOREKEEPER",
    status: "Active",
  },
  {
    id: "u3",
    name: "Priya Desai",
    email: "priya@northstar.example",
    role: "MANAGER",
    status: "Active",
  },
  {
    id: "u4",
    name: "Elena Cruz",
    email: "elena@northstar.example",
    role: "VIEWER",
    status: "Invitation pending",
  },
]

const initialMovements: Movement[] = [
  {
    reference: "REC-2025-0047",
    product: "Common Nails 3 inch",
    type: "Stock receipt",
    quantity: 1000,
    unit: "PCS",
    cost: 2,
    user: "Maya O.",
    date: "May 21, 10:24",
  },
  {
    reference: "ADJ-2025-0091",
    product: "Portland Cement 50 kg",
    type: "Adjustment out",
    quantity: -6,
    unit: "BAG",
    cost: 8.4,
    user: "James K.",
    date: "May 20, 16:42",
  },
  {
    reference: "REC-2025-0046",
    product: "PVC Pipe 20 mm",
    type: "Stock receipt",
    quantity: 32,
    unit: "PCS",
    cost: 18.5,
    user: "Maya O.",
    date: "May 19, 11:08",
  },
]

type PrototypeContextValue = {
  products: Product[]
  saveProduct: (product: Product, previousSku?: string) => void
  removeProduct: (sku: string) => void
  updateStock: (sku: string, quantity: number, cost?: number) => void
  requests: RegistrationRequest[]
  updateRequest: (ref: string, status: string) => void
  plans: Plan[]
  savePlan: (plan: Plan, previousCode?: string) => void
  users: ShopUser[]
  saveUser: (user: ShopUser) => void
  movements: Movement[]
  addMovement: (movement: Movement) => void
  notify: (message: string, tone?: "success" | "error" | "info") => void
}

const PrototypeContext = createContext<PrototypeContextValue | null>(null)

function usePrototype() {
  const value = useContext(PrototypeContext)
  if (!value) throw new Error("Prototype provider is missing")
  return value
}

function getStockStatus(qty: number, reorder: number) {
  if (qty === 0) return "Out of stock"
  if (qty <= reorder) return "Low stock"
  return "In stock"
}

function downloadCsv(filename: string, rows: Array<Array<string | number>>) {
  const content = rows
    .map((row) =>
      row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(","),
    )
    .join("\n")
  const blob = new Blob([content], { type: "text/csv;charset=utf-8" })
  const url = URL.createObjectURL(blob)
  const link = document.createElement("a")
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const featureItems = [
  [
    "Boxes",
    "Mixed-unit inventory",
    "Track each item in its base unit while receiving stock in bags, boxes, kilograms, or pieces.",
  ],
  [
    "Receipt",
    "Controlled stock receiving",
    "Draft, review, and post receipts with visible quantity and cost conversions.",
  ],
  [
    "History",
    "Traceable stock history",
    "Every balance change keeps its source document, actor, time, and historical cost.",
  ],
  [
    "Chart",
    "Reliable valuation",
    "Understand stock value with moving weighted-average costing, not guesswork.",
  ],
  [
    "Alert",
    "Low-stock monitoring",
    "Spot items at or below their reorder level before shelves run empty.",
  ],
  [
    "Users",
    "Role-based access",
    "Give owners, managers, storekeepers, and viewers only the access they need.",
  ],
]

function Logo({ light = false }: { light?: boolean }) {
  return (
    <div className={`logo ${light ? "logo-light" : ""}`}>
      <span className="logo-mark">
        <span />
        <span />
        <span />
      </span>
      <span>StockForge</span>
    </div>
  )
}

function Button({
  children,
  tone = "primary",
  icon,
  onClick,
  disabled = false,
  type = "button",
}: {
  children: React.ReactNode
  tone?: "primary" | "secondary" | "ghost" | "danger"
  icon?: React.ReactNode
  onClick?: () => void
  disabled?: boolean
  type?: "button" | "submit"
}) {
  const { notify } = usePrototype()
  return (
    <button
      className={`btn btn-${tone}`}
      onClick={
        onClick ||
        (() =>
          notify(
            `${String(children)} is available in this interactive prototype.`,
          ))
      }
      disabled={disabled}
      type={type}
    >
      {icon}
      {children}
    </button>
  )
}

function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon: React.ReactNode
  title: string
  description: string
  action?: React.ReactNode
}) {
  return (
    <div className="empty-state">
      <span>{icon}</span>
      <h2>{title}</h2>
      <p>{description}</p>
      {action}
    </div>
  )
}

function Badge({
  children,
  tone,
}: {
  children: React.ReactNode
  tone?: string
}) {
  const inferred = tone || String(children).toLowerCase().replaceAll(" ", "-")
  return (
    <span className={`badge badge-${inferred}`}>
      <span className="badge-dot" />
      {children}
    </span>
  )
}

function ExperienceSwitch({
  experience,
  setExperience,
}: {
  experience: Experience
  setExperience: (value: Experience) => void
}) {
  return (
    <div
      className="experience-switch"
      aria-label="Prototype experience switcher"
    >
      <span className="switch-label">Prototype</span>
      {(["public", "shop", "admin"] as Experience[]).map((item) => (
        <button
          key={item}
          className={experience === item ? "active" : ""}
          onClick={() => setExperience(item)}
        >
          {item === "public"
            ? "Website"
            : item === "shop"
              ? "Shop portal"
              : "Platform admin"}
        </button>
      ))}
    </div>
  )
}

function PublicSite({
  onEnterShop,
  onEnterAdmin,
}: {
  onEnterShop: () => void
  onEnterAdmin: () => void
}) {
  const { notify } = usePrototype()
  const [page, setPage] = useState<PublicPage>("home")
  const [mobileOpen, setMobileOpen] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [annual, setAnnual] = useState(false)

  const navigate = (next: PublicPage) => {
    setPage(next)
    setMobileOpen(false)
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  if (page === "register")
    return (
      <RegistrationPage
        submitted={submitted}
        setSubmitted={setSubmitted}
        navigate={navigate}
      />
    )
  if (page === "status") return <StatusPage navigate={navigate} />
  if (page === "login")
    return (
      <LoginPage
        navigate={navigate}
        onEnterShop={onEnterShop}
        onEnterAdmin={onEnterAdmin}
      />
    )

  return (
    <div className="public-site">
      <header className="public-header">
        <Logo />
        <nav className={mobileOpen ? "public-nav open" : "public-nav"}>
          <button onClick={() => navigate("features")}>Features</button>
          <button onClick={() => navigate("home")}>How it works</button>
          <button onClick={() => navigate("pricing")}>Pricing</button>
          <button
            onClick={() =>
              notify(
                "About StockForge: focused inventory control for independent materials shops.",
              )
            }
          >
            About
          </button>
        </nav>
        <div className="header-actions">
          <button className="text-link" onClick={() => navigate("login")}>
            Log in
          </button>
          <Button onClick={() => navigate("register")}>Request access</Button>
        </div>
        <button
          className="mobile-menu"
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label="Toggle menu"
        >
          <Menu />
        </button>
      </header>

      {page === "pricing" ? (
        <PricingPage
          annual={annual}
          setAnnual={setAnnual}
          onChoose={() => navigate("register")}
        />
      ) : page === "features" ? (
        <FeaturesPage onStart={() => navigate("register")} />
      ) : (
        <main>
          <section className="hero">
            <div className="hero-copy">
              <div className="eyebrow">
                <Sparkles size={15} /> Inventory clarity, built for materials
                shops
              </div>
              <h1>
                Know what’s in stock.
                <br />
                <em>Trust every number.</em>
              </h1>
              <p>
                Track quantities, manage product units, monitor inventory value,
                and keep a reliable history—from one focused platform.
              </p>
              <div className="hero-actions">
                <Button
                  onClick={() => navigate("register")}
                  icon={<ArrowRight size={17} />}
                >
                  Request access
                </Button>
                <Button tone="secondary" onClick={() => navigate("features")}>
                  Explore features
                </Button>
              </div>
              <div className="hero-proof">
                <span>
                  <CheckCircle2 /> No POS clutter
                </span>
                <span>
                  <CheckCircle2 /> One shop, clearly managed
                </span>
                <span>
                  <CheckCircle2 /> Approval-based access
                </span>
              </div>
            </div>
            <div className="hero-visual">
              <div
                className="material-photo"
                role="img"
                aria-label="Metal building materials stored on warehouse shelving"
              />
              <div className="dashboard-preview">
                <div className="preview-top">
                  <Logo />
                  <span>Northstar Building Supply</span>
                  <span className="avatar">MO</span>
                </div>
                <div className="preview-content">
                  <div className="preview-kicker">INVENTORY OVERVIEW</div>
                  <div className="preview-heading">Good morning, Maya</div>
                  <div className="preview-stats">
                    <div>
                      <small>Inventory value</small>
                      <strong>$6,271.70</strong>
                      <span className="up">+4.8% this month</span>
                    </div>
                    <div>
                      <small>Active products</small>
                      <strong>126</strong>
                      <span>Across 11 categories</span>
                    </div>
                    <div>
                      <small>Needs attention</small>
                      <strong>8</strong>
                      <span className="warning">6 low · 2 out</span>
                    </div>
                  </div>
                  <div className="preview-table">
                    <div className="preview-row head">
                      <span>Product</span>
                      <span>On hand</span>
                      <span>Status</span>
                    </div>
                    <div className="preview-row">
                      <span>Portland Cement</span>
                      <b>248 BAG</b>
                      <Badge>In stock</Badge>
                    </div>
                    <div className="preview-row">
                      <span>Rebar 12 mm</span>
                      <b>86 PCS</b>
                      <Badge>Low stock</Badge>
                    </div>
                    <div className="preview-row">
                      <span>Roofing Sheet 3 m</span>
                      <b>0 PCS</b>
                      <Badge>Out of stock</Badge>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section className="trust-strip">
            <span>Designed for the reality of building materials</span>
            <div>
              <strong>BAG</strong>
              <strong>PCS</strong>
              <strong>KG</strong>
              <strong>BOX</strong>
              <strong>L</strong>
              <strong>M</strong>
            </div>
          </section>

          <section className="section problem-section">
            <div className="section-label">Less uncertainty. More control.</div>
            <div className="section-split">
              <h2>
                Inventory work is hard enough without disconnected records.
              </h2>
              <p>
                StockForge replaces handwritten adjustments and scattered
                spreadsheets with a shared, accountable inventory record your
                team can understand.
              </p>
            </div>
            <div className="problem-grid">
              {[
                [
                  "01",
                  "Unclear quantities",
                  "See current balance and its base unit at a glance.",
                ],
                [
                  "02",
                  "Mixed packaging",
                  "Convert boxes, bags, pieces, and weight per product.",
                ],
                [
                  "03",
                  "Unrecorded changes",
                  "Require a reason, actor, and source for every movement.",
                ],
                [
                  "04",
                  "Unknown value",
                  "Track moving weighted-average cost automatically.",
                ],
              ].map(([n, title, body]) => (
                <article key={n}>
                  <span>{n}</span>
                  <h3>{title}</h3>
                  <p>{body}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="section features-section">
            <div className="section-heading">
              <div>
                <div className="section-label">
                  One focused inventory workspace
                </div>
                <h2>
                  Everything your stock team needs.
                  <br />
                  Nothing they don’t.
                </h2>
              </div>
              <Button tone="secondary" onClick={() => navigate("features")}>
                View all features <ArrowRight size={16} />
              </Button>
            </div>
            <div className="feature-grid">
              {featureItems.map(([icon, title, body], index) => (
                <article
                  key={title}
                  className={
                    index === 0 ? "feature-card featured" : "feature-card"
                  }
                >
                  <FeatureIcon name={icon} />
                  <h3>{title}</h3>
                  <p>{body}</p>
                  <span className="learn">
                    Learn more <ArrowRight size={14} />
                  </span>
                </article>
              ))}
            </div>
          </section>

          <section className="section how-section">
            <div className="how-intro">
              <div className="section-label light">A considered start</div>
              <h2>From application to confident inventory control.</h2>
              <p>
                Every shop is reviewed before access is granted. That keeps the
                platform accountable and your data protected.
              </p>
            </div>
            <div className="steps">
              {[
                [
                  "01",
                  "Request access",
                  "Tell us about your shop and choose a preferred plan.",
                ],
                [
                  "02",
                  "Administrator review",
                  "We review the request and may ask for more information.",
                ],
                [
                  "03",
                  "Set up your shop",
                  "Once approved, create your account and configure units.",
                ],
                [
                  "04",
                  "Build your inventory",
                  "Add products, opening stock, and invite your team.",
                ],
              ].map(([n, title, body]) => (
                <div className="step" key={n}>
                  <span>{n}</span>
                  <h3>{title}</h3>
                  <p>{body}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="section pricing-preview">
            <div className="section-heading">
              <div>
                <div className="section-label">Clear, flexible plans</div>
                <h2>Start with the capacity your shop needs.</h2>
              </div>
              <button className="text-link" onClick={() => navigate("pricing")}>
                Compare every feature <ArrowRight size={15} />
              </button>
            </div>
            <PlanCards onChoose={() => navigate("register")} />
          </section>

          <section className="section faq-section">
            <div>
              <div className="section-label">Common questions</div>
              <h2>Answers before you get started.</h2>
              <p>
                Need another answer? Contact our platform team and we’ll point
                you in the right direction.
              </p>
            </div>
            <div className="faq-list">
              {[
                [
                  "How do I register my shop?",
                  "Choose Request access, complete your shop details, and submit the request for review.",
                ],
                [
                  "Why does my application need approval?",
                  "Approval helps verify the shop, selected plan, and account owner before access is created.",
                ],
                [
                  "Can I manage different product units?",
                  "Yes. Each product has a base unit and can define its own alternate-unit conversions.",
                ],
                [
                  "Can I manage multiple branches?",
                  "V1 supports one shop and one physical inventory location per tenant.",
                ],
              ].map(([q, a], i) => (
                <details key={q} open={i === 0}>
                  <summary>
                    {q}
                    <Plus size={18} />
                  </summary>
                  <p>{a}</p>
                </details>
              ))}
            </div>
          </section>
        </main>
      )}

      <footer className="footer">
        <div>
          <Logo light />
          <p>
            Calm, controlled inventory for
            <br />
            building materials businesses.
          </p>
        </div>
        <div>
          <strong>Product</strong>
          <button onClick={() => navigate("features")}>Features</button>
          <button onClick={() => navigate("pricing")}>Pricing</button>
          <button onClick={() => navigate("status")}>Application status</button>
        </div>
        <div>
          <strong>Company</strong>
          <button
            onClick={() => notify("About StockForge opened in prototype mode.")}
          >
            About
          </button>
          <button onClick={() => notify("Contact: support@stockforge.example")}>
            Contact
          </button>
          <button
            onClick={() =>
              notify("Security overview opened in prototype mode.")
            }
          >
            Security
          </button>
        </div>
        <div>
          <strong>Legal</strong>
          <button
            onClick={() => notify("Privacy policy opened in prototype mode.")}
          >
            Privacy
          </button>
          <button
            onClick={() => notify("Terms of service opened in prototype mode.")}
          >
            Terms
          </button>
        </div>
        <div className="footer-bottom">
          © 2025 StockForge. All rights reserved.{" "}
          <span>Inventory management—not sales or accounting software.</span>
        </div>
      </footer>
    </div>
  )
}

function FeatureIcon({ name }: { name: string }) {
  const icons: Record<string, React.ReactNode> = {
    Boxes: <Boxes />,
    Receipt: <ReceiptText />,
    History: <History />,
    Chart: <BarChart3 />,
    Alert: <TriangleAlert />,
    Users: <Users />,
  }
  return <div className="feature-icon">{icons[name]}</div>
}

function PlanCards({ onChoose }: { onChoose: () => void }) {
  const { plans } = usePrototype()
  const visiblePlans = plans.filter((plan) => plan.public && plan.active)
  if (!visiblePlans.length)
    return (
      <EmptyState
        icon={<CircleDollarSign />}
        title="Public plans are being updated"
        description="Contact the platform team to discuss the right capacity for your shop."
        action={<Button onClick={onChoose}>Contact us about access</Button>}
      />
    )
  return (
    <div className="plan-grid">
      {visiblePlans.map((plan) => (
        <article
          key={plan.code}
          className={`plan-card ${plan.featured ? "recommended" : ""}`}
        >
          {plan.featured && (
            <div className="recommended-label">Most popular</div>
          )}
          <h3>{plan.name}</h3>
          <p>{plan.description}</p>
          <div className="price">
            {plan.price !== null && <small>from</small>}
            <strong>
              {plan.price === null ? "Let’s talk" : `$${plan.price}`}
            </strong>
            {plan.price !== null && (
              <span>/ {plan.interval.toLowerCase()}</span>
            )}
          </div>
          <div className="limits">
            <span>
              <PackageOpen />{" "}
              {plan.productLimit === null
                ? "Custom product limit"
                : `Up to ${plan.productLimit.toLocaleString()} products`}
            </span>
            <span>
              <Users />{" "}
              {plan.userLimit === null
                ? "Custom staff limit"
                : `${plan.userLimit} staff users`}
            </span>
          </div>
          <Button
            tone={plan.featured ? "primary" : "secondary"}
            onClick={onChoose}
          >
            Choose {plan.name} <ArrowRight size={16} />
          </Button>
          <ul>
            {plan.features.map((f) => (
              <li key={f}>
                <Check />
                {f}
              </li>
            ))}
          </ul>
        </article>
      ))}
    </div>
  )
}

function PricingPage({
  annual,
  setAnnual,
  onChoose,
}: {
  annual: boolean
  setAnnual: (v: boolean) => void
  onChoose: () => void
}) {
  return (
    <main className="subpage">
      <div className="subpage-hero">
        <div className="section-label">Plans configured for your stage</div>
        <h1>
          Simple inventory pricing.
          <br />
          No accounting-suite overhead.
        </h1>
        <p>
          Choose a starting plan in your application. Final activation is
          confirmed by a platform administrator.
        </p>
        <div className="billing-toggle">
          <button
            className={!annual ? "active" : ""}
            onClick={() => setAnnual(false)}
          >
            Monthly
          </button>
          <button
            className={annual ? "active" : ""}
            onClick={() => setAnnual(true)}
          >
            Yearly <span>Save 15%</span>
          </button>
        </div>
      </div>
      <section className="section no-top">
        <PlanCards onChoose={onChoose} />
        <p className="sample-note">
          <AlertCircle /> Prices and limits shown are editable sample plan data
          for this prototype.
        </p>
      </section>
      <section className="comparison">
        <h2>Compare plan capacity</h2>
        {[
          "Product catalog",
          "Stock receiving & adjustments",
          "Moving average valuation",
          "Staff roles",
          "Printable reports",
        ].map((f, i) => (
          <div className="compare-row" key={f}>
            <strong>{f}</strong>
            <span>
              <Check /> Included
            </span>
            <span>
              <Check /> Included
            </span>
            <span>
              {i > 2 ? (
                "Custom"
              ) : (
                <>
                  <Check /> Included
                </>
              )}
            </span>
          </div>
        ))}
      </section>
    </main>
  )
}

function FeaturesPage({ onStart }: { onStart: () => void }) {
  return (
    <main className="subpage">
      <div className="subpage-hero feature-hero">
        <div>
          <div className="section-label">Purpose-built for inventory</div>
          <h1>A clearer record from delivery to shelf.</h1>
          <p>
            Product-specific units, controlled posting, and audit-friendly
            history make every stock number explainable.
          </p>
          <Button onClick={onStart}>
            Request access <ArrowRight size={16} />
          </Button>
        </div>
        <div className="feature-illustration">
          <div className="conversion-card">
            <Badge>Live calculation</Badge>
            <h3>5 BOX</h3>
            <span>× 200 PCS per BOX</span>
            <strong>= 1,000 PCS</strong>
            <small>Base quantity added to stock</small>
          </div>
        </div>
      </div>
      <section className="section">
        <div className="feature-detail-grid">
          {featureItems
            .concat([
              [
                "Shield",
                "Tenant-isolated data",
                "Your shop records are scoped by backend authorization—not a browser-provided shop ID.",
              ],
              [
                "Report",
                "Focused inventory reports",
                "Print current stock, valuation, low-stock, receipt, and adjustment reports.",
              ],
            ])
            .map(([icon, title, body], i) => (
              <article key={title}>
                <span className="detail-number">
                  {String(i + 1).padStart(2, "0")}
                </span>
                <FeatureIcon name={icon} />
                <h2>{title}</h2>
                <p>{body}</p>
                <ul>
                  <li>
                    <Check /> Clear default and exception states
                  </li>
                  <li>
                    <Check /> Permission-aware actions
                  </li>
                  <li>
                    <Check /> Export-ready table layouts
                  </li>
                </ul>
              </article>
            ))}
        </div>
      </section>
    </main>
  )
}

function RegistrationPage({
  submitted,
  setSubmitted,
  navigate,
}: {
  submitted: boolean
  setSubmitted: (v: boolean) => void
  navigate: (p: PublicPage) => void
}) {
  const { notify } = usePrototype()
  const [step, setStep] = useState(1)
  if (submitted)
    return (
      <div className="auth-page">
        <div className="auth-brand">
          <Logo light />
        </div>
        <div className="success-panel">
          <div className="success-icon">
            <Check />
          </div>
          <div className="eyebrow">Request received</div>
          <h1>Your shop registration request has been submitted.</h1>
          <p>
            We’ll review your information and notify you when a decision has
            been made. Keep this reference if you need to follow up.
          </p>
          <div className="reference-box">
            <span>Request reference</span>
            <strong>REQ-2025-0185</strong>
            <button
              onClick={() => {
                navigator.clipboard?.writeText("REQ-2025-0185")
                notify("Request reference copied.", "success")
              }}
            >
              Copy
            </button>
          </div>
          <div className="timeline">
            <div className="done">
              <Check />
              Submitted
            </div>
            <span />
            <div>
              <Clock3 />
              Administrator review
            </div>
            <span />
            <div>
              <LockKeyhole />
              Account setup
            </div>
          </div>
          <Button onClick={() => navigate("status")}>
            Check application status
          </Button>
          <button className="text-link" onClick={() => navigate("home")}>
            Return to website
          </button>
        </div>
      </div>
    )
  return (
    <div className="registration-page">
      <div className="registration-aside">
        <Logo light />
        <div>
          <span className="aside-step">STEP {step} OF 3</span>
          <h2>
            {step === 1
              ? "Tell us about your shop."
              : step === 2
                ? "Who will own the account?"
                : "Choose how you’ll start."}
          </h2>
          <p>
            Your request is reviewed before any inventory access is created.
          </p>
        </div>
        <div className="privacy-note">
          <ShieldCheck />
          <span>
            <strong>Your data stays protected.</strong> A request reference
            alone cannot expose your application details.
          </span>
        </div>
      </div>
      <div className="registration-main">
        <button
          className="back-link"
          onClick={() => (step > 1 ? setStep(step - 1) : navigate("home"))}
        >
          ← Back
        </button>
        <div className="form-wrap">
          <div className="form-progress">
            <span className={step >= 1 ? "active" : ""}>Shop</span>
            <i />
            <span className={step >= 2 ? "active" : ""}>Owner</span>
            <i />
            <span className={step >= 3 ? "active" : ""}>Plan & consent</span>
          </div>
          {step === 1 && (
            <>
              <h1>Shop information</h1>
              <p className="form-intro">
                Tell us about the business you want to manage.
              </p>
              <div className="field-grid">
                <Field
                  label="Shop name"
                  placeholder="e.g. Northstar Building Supply"
                  required
                  wide
                />
                <Field
                  label="Shop email"
                  placeholder="inventory@northstar.example"
                  required
                />
                <Field label="Shop phone" placeholder="+1 555 0182" required />
                <Field
                  label="Shop address"
                  placeholder="Street, city, region"
                  required
                  wide
                />
                <Field label="Country" value="United States" select />
                <Field
                  label="Preferred currency"
                  value="USD — US Dollar"
                  select
                />
              </div>
            </>
          )}
          {step === 2 && (
            <>
              <h1>Account owner</h1>
              <p className="form-intro">
                This person will receive the approval decision and initial setup
                invitation.
              </p>
              <div className="field-grid">
                <Field
                  label="Full name"
                  placeholder="Full legal name"
                  required
                  wide
                />
                <Field
                  label="Work email"
                  placeholder="owner@shop.example"
                  required
                />
                <Field label="Phone number" placeholder="+1 555 0144" />
                <div className="info-banner wide">
                  <AlertCircle />
                  <span>
                    No password yet. If approved, the owner will receive a
                    secure account setup invitation.
                  </span>
                </div>
              </div>
            </>
          )}
          {step === 3 && (
            <>
              <h1>Plan preference</h1>
              <p className="form-intro">
                This is a preference, not a payment. An administrator confirms
                activation.
              </p>
              <div className="field-grid">
                <Field
                  label="Selected plan"
                  value="Growth — up to 1,000 products"
                  select
                  wide
                />
                <Field label="Billing period" value="Monthly" select />
                <Field label="Preferred shop ID" value="northstar-building" />
                <Field
                  label="Optional message"
                  placeholder="Anything our review team should know?"
                  wide
                  textarea
                />
                <label className="check-row wide">
                  <input type="checkbox" defaultChecked />
                  <span>
                    <strong>I agree to the Terms and Privacy Policy.</strong>
                    <small>
                      I confirm I’m authorized to submit this request for the
                      shop.
                    </small>
                  </span>
                </label>
              </div>
            </>
          )}
          <div className="form-actions">
            {step < 3 ? (
              <Button onClick={() => setStep(step + 1)}>
                Continue <ArrowRight size={16} />
              </Button>
            ) : (
              <Button onClick={() => setSubmitted(true)}>
                Submit request <ArrowRight size={16} />
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function Field({
  label,
  placeholder,
  value,
  required,
  wide,
  select,
  textarea,
}: {
  label: string
  placeholder?: string
  value?: string
  required?: boolean
  wide?: boolean
  select?: boolean
  textarea?: boolean
}) {
  const optionSets: Record<string, string[]> = {
    Country: ["United States", "Ghana", "Nigeria", "Kenya"],
    "Preferred currency": [
      "USD — US Dollar",
      "GHS — Ghanaian Cedi",
      "NGN — Nigerian Naira",
      "KES — Kenyan Shilling",
    ],
    "Selected plan": [
      "Starter — up to 250 products",
      "Growth — up to 1,000 products",
      "Scale — custom capacity",
    ],
    "Billing period": ["Monthly", "Yearly"],
    "Default currency": [
      "USD — US Dollar",
      "GHS — Ghanaian Cedi",
      "NGN — Nigerian Naira",
    ],
    "Default timezone": [
      "UTC−05:00 Eastern Time",
      "UTC+00:00 Greenwich Mean Time",
      "UTC+03:00 East Africa Time",
    ],
    "Public registration": [
      "Enabled — approval required",
      "Temporarily paused",
    ],
    "Grace period": ["No grace period", "3 days", "7 days", "14 days"],
    "Default trial duration": ["Disabled", "7 days", "14 days", "30 days"],
    Timezone: [
      "UTC−05:00 Eastern Time",
      "UTC+00:00 Greenwich Mean Time",
      "UTC+03:00 East Africa Time",
    ],
    "Quantity precision": [
      "Whole numbers",
      "2 decimal places",
      "3 decimal places",
    ],
    "Negative stock": ["Not allowed", "Allowed with warning"],
  }
  const options = [
    ...new Set([value || "", ...(optionSets[label] || [])]),
  ].filter(Boolean)
  return (
    <label className={`field ${wide ? "wide" : ""}`}>
      <span>
        {label}
        {required && <b>*</b>}
      </span>
      {textarea ? (
        <textarea placeholder={placeholder} />
      ) : select ? (
        <select defaultValue={value}>
          {options.map((option) => (
            <option key={option}>{option}</option>
          ))}
        </select>
      ) : (
        <input placeholder={placeholder} defaultValue={value} />
      )}
    </label>
  )
}

function StatusPage({ navigate }: { navigate: (p: PublicPage) => void }) {
  const { notify } = usePrototype()
  const [checked, setChecked] = useState(false)
  return (
    <div className="auth-page status-page">
      <div className="auth-brand">
        <Logo light />
        <button onClick={() => navigate("home")}>Back to website</button>
      </div>
      <div className="status-card">
        <div className="eyebrow">Application status</div>
        <h1>
          {checked
            ? "Your request is under review."
            : "Check your registration request."}
        </h1>
        {checked ? (
          <>
            <div className="status-result">
              <div className="status-result-head">
                <div>
                  <small>REQUEST</small>
                  <strong>REQ-2025-0185</strong>
                </div>
                <Badge>Pending</Badge>
              </div>
              <div className="review-visual">
                <span className="active">
                  <Check />
                </span>
                <i />
                <span className="active">
                  <Clock3 />
                </span>
                <i />
                <span>
                  <LockKeyhole />
                </span>
              </div>
              <h3>Administrator review</h3>
              <p>
                Submitted today at 10:14. We’ll email maya@northstar.example
                when a decision is made or if more information is needed.
              </p>
            </div>
            <Button tone="secondary" onClick={() => setChecked(false)}>
              Check another request
            </Button>
          </>
        ) : (
          <>
            <p>
              Enter your reference and the verification code sent to your
              contact email. A reference alone cannot reveal private details.
            </p>
            <Field
              label="Request reference"
              placeholder="REQ-2025-0000"
              required
            />
            <Field
              label="Email verification code"
              placeholder="6-digit code"
              required
            />
            <Button onClick={() => setChecked(true)}>
              Check status <ArrowRight size={16} />
            </Button>
            <button
              className="text-link"
              onClick={() =>
                notify(
                  "A new prototype verification code has been requested.",
                  "success",
                )
              }
            >
              Resend verification code
            </button>
          </>
        )}
      </div>
    </div>
  )
}

function LoginPage({
  navigate,
  onEnterShop,
  onEnterAdmin,
}: {
  navigate: (p: PublicPage) => void
  onEnterShop: () => void
  onEnterAdmin: () => void
}) {
  const { notify } = usePrototype()
  return (
    <div className="login-page">
      <div className="login-image">
        <Logo light />
        <div>
          <span className="quote-mark">“</span>
          <blockquote>
            The first time our stock count and system balance agreed, the whole
            team trusted the process.
          </blockquote>
          <p>Illustrative product scenario</p>
        </div>
      </div>
      <div className="login-form">
        <button className="back-link" onClick={() => navigate("home")}>
          ← Back to website
        </button>
        <div className="login-box">
          <div className="eyebrow">Welcome back</div>
          <h1>Log in to StockForge</h1>
          <p>Use the workspace assigned to your account.</p>
          <Field
            label="Email address"
            placeholder="you@shop.example"
            required
          />
          <Field label="Password" placeholder="Enter your password" required />
          <div className="login-help">
            <label>
              <input type="checkbox" /> Remember me
            </label>
            <button
              onClick={() =>
                notify(
                  "Password reset instructions would be sent to the entered email.",
                  "success",
                )
              }
            >
              Forgot password?
            </button>
          </div>
          <Button onClick={onEnterShop}>Log in to shop portal</Button>
          <div className="or">
            <span />
            or preview
            <span />
          </div>
          <Button tone="secondary" onClick={onEnterAdmin}>
            Platform admin demo
          </Button>
          <div className="login-notice">
            <LockKeyhole /> Pending applications do not have portal access.{" "}
            <button onClick={() => navigate("status")}>Check status</button>
          </div>
        </div>
      </div>
    </div>
  )
}

function Shell({
  mode,
  active,
  setActive,
  children,
}: {
  mode: "shop" | "admin"
  active: string
  setActive: (v: any) => void
  children: React.ReactNode
}) {
  const { notify } = usePrototype()
  const shopItems: Array<[ShopPage, React.ReactNode]> = [
    ["Dashboard", <LayoutDashboard />],
    ["Products", <Boxes />],
    ["Categories", <Store />],
    ["Inventory", <Warehouse />],
    ["Receive stock", <PackageOpen />],
    ["Adjustments", <SlidersHorizontal />],
    ["Stock ledger", <History />],
    ["Reports", <FileBarChart />],
    ["Users & roles", <Users />],
    ["Subscription", <CircleDollarSign />],
  ]
  const adminItems: Array<[AdminPage, React.ReactNode]> = [
    ["Overview", <LayoutDashboard />],
    ["Registration requests", <ClipboardCheck />],
    ["Shops / tenants", <Building2 />],
    ["Plans", <FileText />],
    ["Subscriptions", <CircleDollarSign />],
    ["Platform users", <ShieldCheck />],
    ["Audit logs", <History />],
    ["Settings", <Settings />],
  ]
  const items = mode === "shop" ? shopItems : adminItems
  return (
    <div className={`app-shell ${mode}`}>
      <aside className="sidebar">
        <Logo light />
        {mode === "shop" ? (
          <div className="tenant-card">
            <span className="tenant-logo">N</span>
            <div>
              <strong>Northstar Building</strong>
              <small>Growth plan · Active</small>
            </div>
            <ChevronDown />
          </div>
        ) : (
          <div className="admin-label">
            <ShieldCheck /> Platform administration
          </div>
        )}
        <nav>
          {items.map(([label, icon]) => (
            <button
              key={label}
              className={active === label ? "active" : ""}
              onClick={() => setActive(label)}
            >
              {icon}
              <span>{label}</span>
              {label === "Registration requests" && <b>3</b>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          {mode === "shop" && (
            <div className="plan-usage">
              <div>
                <span>Products</span>
                <b>126 / 1,000</b>
              </div>
              <div className="usage-bar">
                <i />
              </div>
              <button onClick={() => setActive("Subscription")}>
                View plan
              </button>
            </div>
          )}
          <button
            onClick={() => notify("Help center opened in prototype mode.")}
          >
            <HelpCircle /> Help & support
          </button>
          <button
            onClick={() =>
              setActive(mode === "shop" ? "Shop settings" : "Settings")
            }
          >
            <Settings /> {mode === "shop" ? "Shop settings" : "Admin settings"}
          </button>
        </div>
      </aside>
      <div className="shell-main">
        <header className="topbar">
          <button
            className="icon-button shell-menu"
            onClick={() =>
              notify(
                "Use the prototype switcher or desktop navigation to change sections.",
              )
            }
          >
            <Menu />
          </button>
          <button
            className="global-search"
            onClick={() =>
              notify(
                "Global search is ready. Use each page search to filter its current records.",
              )
            }
          >
            <Search />
            <span>
              Search{" "}
              {mode === "shop"
                ? "products, receipts, references"
                : "shops, requests, subscriptions"}
            </span>
            <kbd>⌘ K</kbd>
          </button>
          <div className="top-actions">
            <button
              className="icon-button"
              onClick={() =>
                notify(
                  "You have 2 inventory alerts and 1 account notification.",
                )
              }
            >
              <Bell />
              <i />
            </button>
            <button
              className="user-menu"
              onClick={() =>
                notify(
                  "Profile menu: account preferences, security, and sign out.",
                )
              }
            >
              <span className="avatar">{mode === "shop" ? "MO" : "AL"}</span>
              <div>
                <strong>{mode === "shop" ? "Maya Okafor" : "Ari Lane"}</strong>
                <small>
                  {mode === "shop" ? "Owner" : "Platform administrator"}
                </small>
              </div>
              <ChevronDown />
            </button>
          </div>
        </header>
        <main className="workspace">{children}</main>
      </div>
    </div>
  )
}

function ShopPortal() {
  const [active, setActive] = useState<ShopPage>("Dashboard")
  const [selectedSku, setSelectedSku] = useState<string>()
  const [editingSku, setEditingSku] = useState<string>()

  const openProduct = (sku: string) => {
    setSelectedSku(sku)
    setActive("Product detail")
  }
  const editProduct = (sku?: string) => {
    setEditingSku(sku)
    setActive("Product form")
  }
  return (
    <Shell mode="shop" active={active} setActive={setActive}>
      {active === "Dashboard" && (
        <ShopDashboard
          setActive={setActive}
          onCreateProduct={() => editProduct()}
        />
      )}
      {(active === "Products" || active === "Inventory") && (
        <ProductTable
          mode={active}
          onCreate={() => editProduct()}
          onView={openProduct}
          onEdit={editProduct}
          onAdjust={() => setActive("Adjustments")}
        />
      )}
      {active === "Product form" && (
        <ProductForm
          sku={editingSku}
          onCancel={() => setActive(editingSku ? "Product detail" : "Products")}
          onSaved={(sku) => {
            setSelectedSku(sku)
            setActive("Product detail")
          }}
        />
      )}
      {active === "Product detail" && selectedSku && (
        <ProductDetail
          sku={selectedSku}
          onBack={() => setActive("Products")}
          onEdit={() => editProduct(selectedSku)}
          onReceive={() => setActive("Receive stock")}
          onAdjust={() => setActive("Adjustments")}
        />
      )}
      {active === "Categories" && <Categories />}
      {active === "Receive stock" && <ReceiveStock setActive={setActive} />}
      {active === "Adjustments" && <Adjustments setActive={setActive} />}
      {active === "Stock ledger" && <Ledger />}
      {active === "Reports" && <Reports />}
      {active === "Users & roles" && <UsersRoles />}
      {active === "Subscription" && <Subscription />}
      {active === "Shop settings" && <ShopSettings />}
    </Shell>
  )
}

function PageHead({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string
  title: string
  description?: string
  actions?: React.ReactNode
}) {
  return (
    <div className="page-head">
      <div>
        <div className="breadcrumbs">
          Northstar Building Supply <ChevronRight /> {eyebrow}
        </div>
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  )
}

function ShopDashboard({
  setActive,
  onCreateProduct,
}: {
  setActive: (p: ShopPage) => void
  onCreateProduct: () => void
}) {
  const { products, movements } = usePrototype()
  const inventoryValue = products.reduce(
    (sum, product) => sum + product.qty * product.cost,
    0,
  )
  const lowCount = products.filter(
    (product) => product.status === "Low stock",
  ).length
  const outCount = products.filter(
    (product) => product.status === "Out of stock",
  ).length
  return (
    <>
      <PageHead
        eyebrow="Dashboard"
        title="Good morning, Maya."
        description="Here’s what needs attention across your inventory today."
        actions={
          <>
            <Button tone="secondary" onClick={onCreateProduct} icon={<Plus />}>
              Add product
            </Button>
            <Button
              onClick={() => setActive("Receive stock")}
              icon={<PackageOpen />}
            >
              Receive stock
            </Button>
          </>
        }
      />
      <div className="metric-grid">
        <Metric
          icon={<Boxes />}
          label="Active products"
          value={String(products.filter((product) => product.active).length)}
          meta={`Across ${new Set(products.map((product) => product.category)).size} categories`}
          onClick={() => setActive("Products")}
        />
        <Metric
          icon={<CircleDollarSign />}
          label="Total inventory value"
          value={`$${inventoryValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
          meta="+4.8% this month"
          positive
          onClick={() => setActive("Reports")}
        />
        <Metric
          icon={<TriangleAlert />}
          label="Low-stock products"
          value={String(lowCount)}
          meta="2 more since last week"
          warning
          onClick={() => setActive("Inventory")}
        />
        <Metric
          icon={<AlertCircle />}
          label="Out of stock"
          value={String(outCount)}
          meta="Requires attention"
          danger
          onClick={() => setActive("Inventory")}
        />
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel">
          <PanelHead
            title="Inventory value"
            note="Last 6 months"
            action="View valuation report"
            onAction={() => setActive("Reports")}
          />
          <div className="chart-meta">
            <strong>
              $
              {inventoryValue.toLocaleString(undefined, {
                minimumFractionDigits: 2,
              })}
            </strong>
            <span>
              <ArrowRight /> Net change +$287.45
            </span>
          </div>
          <div className="line-chart">
            <svg viewBox="0 0 700 190" preserveAspectRatio="none">
              <defs>
                <linearGradient id="area" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0" stopColor="#1f7a72" stopOpacity=".22" />
                  <stop offset="1" stopColor="#1f7a72" stopOpacity="0" />
                </linearGradient>
              </defs>
              <path
                d="M0,152 C60,145 82,121 142,128 C208,137 232,87 292,101 C356,116 390,59 446,75 C510,94 547,42 596,57 C648,69 665,29 700,34 L700,190 L0,190Z"
                fill="url(#area)"
              />
              <path
                d="M0,152 C60,145 82,121 142,128 C208,137 232,87 292,101 C356,116 390,59 446,75 C510,94 547,42 596,57 C648,69 665,29 700,34"
                fill="none"
                stroke="#1f7a72"
                strokeWidth="3"
              />
              <circle
                cx="700"
                cy="34"
                r="5"
                fill="#fff"
                stroke="#1f7a72"
                strokeWidth="3"
              />
            </svg>
            <div className="chart-labels">
              <span>Dec</span>
              <span>Jan</span>
              <span>Feb</span>
              <span>Mar</span>
              <span>Apr</span>
              <span>May</span>
            </div>
          </div>
        </section>
        <section className="panel attention-panel">
          <PanelHead
            title="Needs attention"
            note="8 products"
            action="View inventory"
            onAction={() => setActive("Inventory")}
          />
          <div className="attention-list">
            {products
              .filter((p) => p.status !== "In stock")
              .map((p) => (
                <div key={p.sku}>
                  <span
                    className={`stock-icon ${
                      p.status === "Out of stock" ? "danger" : ""
                    }`}
                  >
                    <PackageOpen />
                  </span>
                  <div>
                    <strong>{p.name}</strong>
                    <small>
                      {p.sku} · Reorder at {p.reorder} {p.unit}
                    </small>
                  </div>
                  <div className="qty">
                    <strong>
                      {p.qty} {p.unit}
                    </strong>
                    <Badge>{p.status}</Badge>
                  </div>
                </div>
              ))}
          </div>
          <button className="panel-link" onClick={() => setActive("Inventory")}>
            Review all low-stock items <ArrowRight />
          </button>
        </section>
        <section className="panel movements-panel">
          <PanelHead
            title="Recent stock movements"
            note="Latest posted activity"
            action="Open ledger"
            onAction={() => setActive("Stock ledger")}
          />
          <div className="movement-list">
            {movements.slice(0, 4).map((movement) => (
              <button
                className="movement-row"
                key={movement.reference}
                onClick={() => setActive("Stock ledger")}
              >
                <span className={movement.quantity > 0 ? "in" : "out"}>
                  {movement.quantity > 0 ? <Plus /> : <Minus />}
                </span>
                <div>
                  <strong>{movement.product}</strong>
                  <small>
                    {movement.type} · {movement.reference}
                  </small>
                </div>
                <b>
                  {movement.quantity > 0 ? "+" : ""}
                  {movement.quantity.toLocaleString()} {movement.unit}
                </b>
                <time>{movement.date}</time>
              </button>
            ))}
          </div>
        </section>
        <section className="panel activity-panel">
          <PanelHead title="This month" note="May 2025" />
          <div className="mini-metric">
            <PackageOpen />
            <div>
              <small>Stock receipts</small>
              <strong>12</strong>
            </div>
            <span>$4,184.20 value added</span>
          </div>
          <div className="mini-metric">
            <SlidersHorizontal />
            <div>
              <small>Adjustments</small>
              <strong>4</strong>
            </div>
            <span>3 out · 1 in</span>
          </div>
          <div className="audit-note">
            <ShieldCheck />
            <span>All activity is tenant-scoped and audit recorded.</span>
          </div>
        </section>
      </div>
    </>
  )
}

function Metric({
  icon,
  label,
  value,
  meta,
  positive,
  warning,
  danger,
  onClick,
}: {
  icon: React.ReactNode
  label: string
  value: string
  meta: string
  positive?: boolean
  warning?: boolean
  danger?: boolean
  onClick?: () => void
}) {
  const content = (
    <>
      <div
        className={`metric-icon ${
          warning ? "warning" : danger ? "danger" : ""
        }`}
      >
        {icon}
      </div>
      <span>{label}</span>
      <strong>{value}</strong>
      <small
        className={
          positive
            ? "positive"
            : warning
              ? "warning-text"
              : danger
                ? "danger-text"
                : ""
        }
      >
        {meta}
      </small>
    </>
  )
  return onClick ? (
    <button className="metric-card metric-button" onClick={onClick}>
      {content}
    </button>
  ) : (
    <div className="metric-card">{content}</div>
  )
}

function PanelHead({
  title,
  note,
  action,
  onAction,
}: {
  title: string
  note?: string
  action?: string
  onAction?: () => void
}) {
  const { notify } = usePrototype()
  return (
    <div className="panel-head">
      <div>
        <h2>{title}</h2>
        {note && <span>{note}</span>}
      </div>
      {action && (
        <button
          onClick={
            onAction || (() => notify(`${action} opened in prototype mode.`))
          }
        >
          {action} <ArrowRight />
        </button>
      )}
    </div>
  )
}

function ProductTable({
  mode,
  onCreate,
  onView,
  onEdit,
  onAdjust,
}: {
  mode: "Products" | "Inventory"
  onCreate: () => void
  onView: (sku: string) => void
  onEdit: (sku: string) => void
  onAdjust: () => void
}) {
  const { products, removeProduct, notify } = usePrototype()
  const [search, setSearch] = useState("")
  const [category, setCategory] = useState("All categories")
  const [stock, setStock] = useState("All stock")
  const [view, setView] = useState<"table" | "grid">("table")
  const [page, setPage] = useState(1)
  const [deleteSku, setDeleteSku] = useState<string>()
  const filtered = products.filter(
    (product) =>
      `${product.sku} ${product.name} ${product.description}`
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (category === "All categories" || product.category === category) &&
      (stock === "All stock" || product.status === stock),
  )
  const visible = filtered.slice((page - 1) * 6, page * 6)
  const exportProducts = () => {
    downloadCsv("stockforge-products.csv", [
      [
        "SKU",
        "Product",
        "Description",
        "Category",
        "Quantity",
        "Unit",
        "Average cost",
        "Status",
      ],
      ...filtered.map((product) => [
        product.sku,
        product.name,
        product.description,
        product.category,
        product.qty,
        product.unit,
        product.cost,
        product.status,
      ]),
    ])
    notify("Product export downloaded.", "success")
  }
  return (
    <>
      <PageHead
        eyebrow={mode}
        title={mode === "Products" ? "Products" : "Inventory overview"}
        description={
          mode === "Products"
            ? "Manage catalog details, units, conversions, and stock thresholds."
            : "Current balances are stored and displayed in each product’s base unit."
        }
        actions={
          <>
            <Button tone="secondary" onClick={exportProducts}>
              Export CSV
            </Button>
            <Button
              icon={<Plus />}
              onClick={mode === "Products" ? onCreate : onAdjust}
            >
              {mode === "Products" ? "Add product" : "Stock adjustment"}
            </Button>
          </>
        }
      />
      <div className="panel table-panel">
        <div className="table-tools">
          <div className="table-search">
            <Search />
            <input
              placeholder="Search by SKU, name, or description"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setPage(1)
              }}
            />
          </div>
          <select
            value={category}
            onChange={(event) => setCategory(event.target.value)}
          >
            <option>All categories</option>
            {[...new Set(products.map((product) => product.category))].map(
              (item) => (
                <option key={item}>{item}</option>
              ),
            )}
          </select>
          <select
            value={stock}
            onChange={(event) => setStock(event.target.value)}
          >
            <option>All stock</option>
            <option>In stock</option>
            <option>Low stock</option>
            <option>Out of stock</option>
          </select>
          {(search ||
            category !== "All categories" ||
            stock !== "All stock") && (
            <button
              className="reset-filter"
              onClick={() => {
                setSearch("")
                setCategory("All categories")
                setStock("All stock")
              }}
            >
              Reset filters
            </button>
          )}
          <div className="view-toggle">
            <button
              className={view === "table" ? "active" : ""}
              onClick={() => setView("table")}
              aria-label="Table view"
            >
              <Table2 />
            </button>
            <button
              className={view === "grid" ? "active" : ""}
              onClick={() => setView("grid")}
              aria-label="Grid view"
            >
              <Grid2X2 />
            </button>
          </div>
          <span className="filter-count">{filtered.length} products</span>
        </div>
        {!filtered.length ? (
          <EmptyState
            icon={<Search />}
            title={
              products.length
                ? "No products match these filters"
                : "No products yet"
            }
            description={
              products.length
                ? "Try a different product name, category, or stock status."
                : "Create your first product to begin tracking stock."
            }
            action={
              products.length ? (
                <Button
                  tone="secondary"
                  onClick={() => {
                    setSearch("")
                    setCategory("All categories")
                    setStock("All stock")
                  }}
                >
                  Clear filters
                </Button>
              ) : (
                <Button onClick={onCreate}>Create first product</Button>
              )
            }
          />
        ) : view === "table" ? (
          <div className="data-table product-table">
            <div className="tr th">
              <span>
                <input type="checkbox" />
              </span>
              <span>SKU</span>
              <span>Product</span>
              <span>Category</span>
              <span>On hand</span>
              <span>Avg. cost</span>
              <span>Inventory value</span>
              <span>Status</span>
              <span />
            </div>
            {visible.map((p) => (
              <div className="tr" key={p.sku}>
                <span>
                  <input type="checkbox" />
                </span>
                <span className="mono">{p.sku}</span>
                <span className="product-name">
                  {p.image ? (
                    <img src={p.image} alt="" />
                  ) : (
                    <i>{p.name.charAt(0)}</i>
                  )}
                  <strong>{p.name}</strong>
                  <small>{p.description}</small>
                </span>
                <span>{p.category}</span>
                <span>
                  <strong>
                    {p.qty.toLocaleString()} {p.unit}
                  </strong>
                  <small>Reorder: {p.reorder}</small>
                </span>
                <span>${p.cost.toFixed(2)}</span>
                <span>
                  <strong>
                    $
                    {(p.qty * p.cost).toLocaleString(undefined, {
                      minimumFractionDigits: 2,
                    })}
                  </strong>
                </span>
                <span>
                  <Badge>{p.status}</Badge>
                </span>
                <span>
                  <div className="row-actions">
                    <button onClick={() => onView(p.sku)}>View</button>
                    <button onClick={() => onEdit(p.sku)}>Edit</button>
                    <button onClick={() => setDeleteSku(p.sku)}>
                      {p.active ? "Deactivate" : "Remove"}
                    </button>
                  </div>
                </span>
              </div>
            ))}
          </div>
        ) : (
          <div className="product-grid">
            {visible.map((product) => (
              <article className="product-card" key={product.sku}>
                <button
                  className="product-image"
                  onClick={() => onView(product.sku)}
                >
                  {product.image ? (
                    <img src={product.image} alt={product.name} />
                  ) : (
                    <ImagePlus />
                  )}
                  <Badge>{product.status}</Badge>
                </button>
                <div className="product-card-body">
                  <span className="mono">{product.sku}</span>
                  <h2>{product.name}</h2>
                  <p>{product.description}</p>
                  <div>
                    <span>On hand</span>
                    <strong>
                      {product.qty.toLocaleString()} {product.unit}
                    </strong>
                  </div>
                  <div>
                    <span>Inventory value</span>
                    <strong>
                      $
                      {(product.qty * product.cost).toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                      })}
                    </strong>
                  </div>
                  <div className="product-card-actions">
                    <Button
                      tone="secondary"
                      onClick={() => onView(product.sku)}
                    >
                      View details
                    </Button>
                    <button onClick={() => onEdit(product.sku)}>Edit</button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
        <div className="pagination">
          <span>
            Showing {filtered.length ? (page - 1) * 6 + 1 : 0}–
            {Math.min(page * 6, filtered.length)} of {filtered.length} products
          </span>
          <div>
            <button
              disabled={page === 1}
              onClick={() => setPage((value) => Math.max(1, value - 1))}
            >
              Previous
            </button>
            {Array.from(
              { length: Math.max(1, Math.ceil(filtered.length / 6)) },
              (_, index) => (
                <button
                  key={index}
                  className={page === index + 1 ? "active" : ""}
                  onClick={() => setPage(index + 1)}
                >
                  {index + 1}
                </button>
              ),
            )}
            <button
              disabled={page >= Math.ceil(filtered.length / 6)}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </div>
      <div className="logic-note">
        <AlertCircle />
        <span>
          <strong>Stock status logic:</strong> Out of stock = 0. Low stock =
          above 0 and at or below reorder level. In stock = above reorder level.
        </span>
      </div>
      {deleteSku && (
        <Modal
          title={`${
            products.find((product) => product.sku === deleteSku)?.active
              ? "Deactivate"
              : "Remove"
          } this product?`}
          onClose={() => setDeleteSku(undefined)}
        >
          <div className="modal-alert">
            <TriangleAlert />
            <span>
              Products with stock history should be deactivated instead of
              erased. Their movement history remains traceable.
            </span>
          </div>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setDeleteSku(undefined)}>
              Cancel
            </Button>
            <Button
              tone="danger"
              onClick={() => {
                removeProduct(deleteSku)
                setDeleteSku(undefined)
                notify("Product status updated.", "success")
              }}
            >
              Confirm product change
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function ProductForm({
  sku,
  onCancel,
  onSaved,
}: {
  sku?: string
  onCancel: () => void
  onSaved: (sku: string) => void
}) {
  const { products, saveProduct, notify } = usePrototype()
  const existing = products.find((product) => product.sku === sku)
  const [name, setName] = useState(existing?.name || "")
  const [productSku, setProductSku] = useState(existing?.sku || "")
  const [description, setDescription] = useState(existing?.description || "")
  const [category, setCategory] = useState(existing?.category || "Cement")
  const [customCategory, setCustomCategory] = useState("")
  const [unit, setUnit] = useState(existing?.unit || "PCS")
  const [qty, setQty] = useState(existing?.qty || 0)
  const [reorder, setReorder] = useState(existing?.reorder || 0)
  const [cost, setCost] = useState(existing?.cost || 0)
  const [altUnit, setAltUnit] = useState(existing?.conversion?.unit || "")
  const [factor, setFactor] = useState(existing?.conversion?.factor || 1)
  const [brand, setBrand] = useState(existing?.brand || "")
  const [barcode, setBarcode] = useState(existing?.barcode || "")
  const [image, setImage] = useState(existing?.image || "")
  const [error, setError] = useState("")
  const [advanced, setAdvanced] = useState(Boolean(existing?.conversion))

  const handleImage = (file?: File) => {
    if (!file) return
    if (!file.type.startsWith("image/")) {
      setError("Choose a JPG, PNG, or WebP product image.")
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("The product image must be smaller than 5 MB.")
      return
    }
    setImage(URL.createObjectURL(file))
    setError("")
  }
  const submit = () => {
    if (
      !name.trim() ||
      !productSku.trim() ||
      !description.trim() ||
      (category === "__new" && !customCategory.trim())
    ) {
      setError("Enter a product name, unique SKU, and useful description.")
      return
    }
    if (
      products.some(
        (product) =>
          product.sku.toLowerCase() === productSku.toLowerCase() &&
          product.sku !== existing?.sku,
      )
    ) {
      setError("This SKU is already used by another product in your shop.")
      return
    }
    if (reorder < 0 || cost < 0 || factor <= 0) {
      setError(
        "Reorder level and cost cannot be negative. Conversion factor must be greater than zero.",
      )
      return
    }
    if (altUnit && altUnit === unit) {
      setError("The base unit cannot also be used as an alternate unit.")
      return
    }
    const product: Product = {
      sku: productSku.trim().toUpperCase(),
      name: name.trim(),
      description: description.trim(),
      category: category === "__new" ? customCategory.trim() : category,
      unit,
      qty,
      reorder,
      cost,
      status: getStockStatus(qty, reorder),
      image,
      brand,
      barcode,
      active: existing?.active ?? true,
      conversion: advanced && altUnit ? { unit: altUnit, factor } : undefined,
    }
    saveProduct(product, existing?.sku)
    notify(
      existing ? "Product changes saved." : "Product created successfully.",
      "success",
    )
    onSaved(product.sku)
  }

  return (
    <>
      <PageHead
        eyebrow={`Products / ${existing ? "Edit" : "New product"}`}
        title={existing ? `Edit ${existing.name}` : "Create product"}
        description="Product identity, units, stock thresholds, and cost are kept together for a reliable inventory record."
        actions={
          <>
            <Button tone="secondary" onClick={onCancel}>
              Cancel
            </Button>
            <Button onClick={submit}>
              {existing ? "Save changes" : "Create product"}
            </Button>
          </>
        }
      />
      {error && (
        <div className="form-error">
          <AlertCircle />
          <span>
            <strong>Check the product information</strong>
            {error}
          </span>
        </div>
      )}
      <div className="product-form-layout">
        <div className="product-form-main">
          <section className="panel form-section">
            <PanelHead
              title="Product identity"
              note="Required fields are marked with *"
            />
            <div className="identity-layout">
              <div className="image-upload">
                <div className="image-preview">
                  {image ? (
                    <img src={image} alt="Product preview" />
                  ) : (
                    <>
                      <ImagePlus />
                      <span>No product image</span>
                    </>
                  )}
                </div>
                <label className="upload-button">
                  <Upload />
                  {image ? "Replace image" : "Upload image"}
                  <input
                    type="file"
                    accept="image/png,image/jpeg,image/webp"
                    onChange={(event) => handleImage(event.target.files?.[0])}
                  />
                </label>
                {image && (
                  <button onClick={() => setImage("")}>Remove image</button>
                )}
                <small>
                  JPG, PNG, or WebP. Up to 5 MB. A square crop works best.
                </small>
              </div>
              <div className="field-grid">
                <label className="field wide">
                  <span>Product name*</span>
                  <input
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    placeholder="e.g. Portland Cement 50 kg"
                  />
                </label>
                <label className="field">
                  <span>SKU or product code*</span>
                  <input
                    value={productSku}
                    onChange={(event) => setProductSku(event.target.value)}
                    placeholder="e.g. CEM-OPC-50"
                  />
                </label>
                <label className="field">
                  <span>Category*</span>
                  <select
                    value={category}
                    onChange={(event) => setCategory(event.target.value)}
                  >
                    {[
                      "Cement",
                      "Steel",
                      "Roofing",
                      "Hardware",
                      "Plumbing",
                      "Paint",
                      "Electrical",
                      "Timber",
                    ].map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                    <option value="__new">+ Create a new category</option>
                  </select>
                  {category === "__new" && (
                    <input
                      autoFocus
                      value={customCategory}
                      onChange={(event) =>
                        setCustomCategory(event.target.value)
                      }
                      placeholder="New category name"
                    />
                  )}
                  <small>The group used to filter and report products.</small>
                </label>
                <label className="field wide">
                  <span>Description*</span>
                  <textarea
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="Explain what this product is and the characteristics staff use to identify it."
                  />
                  <small>
                    Use practical details such as size, material, finish, or
                    container size. Avoid unverified technical claims.
                  </small>
                </label>
                <label className="field">
                  <span>Brand or manufacturer</span>
                  <input
                    value={brand}
                    onChange={(event) => setBrand(event.target.value)}
                    placeholder="Optional"
                  />
                </label>
                <label className="field">
                  <span>Barcode</span>
                  <input
                    value={barcode}
                    onChange={(event) => setBarcode(event.target.value)}
                    placeholder="Optional barcode"
                  />
                </label>
              </div>
            </div>
          </section>
          <section className="panel form-section">
            <PanelHead
              title="Inventory configuration"
              note="Stock is always stored in the base unit"
            />
            <div className="field-grid section-fields">
              <label className="field">
                <span>Base unit*</span>
                <select
                  value={unit}
                  onChange={(event) => setUnit(event.target.value)}
                >
                  {["PCS", "BAG", "KG", "L", "M", "PAIL"].map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
                <small>
                  The standard unit used to track this product’s stock.
                </small>
              </label>
              <label className="field">
                <span>Low-stock threshold*</span>
                <input
                  type="number"
                  min="0"
                  value={reorder}
                  onChange={(event) => setReorder(Number(event.target.value))}
                />
                <small>
                  Low stock appears when balance is at or below this value.
                </small>
              </label>
              <label className="field">
                <span>
                  {existing ? "Current stock" : "Opening stock quantity"}
                </span>
                <input
                  type="number"
                  min="0"
                  value={qty}
                  disabled={Boolean(existing)}
                  onChange={(event) => setQty(Number(event.target.value))}
                />
                <small>
                  {existing
                    ? "Use a receipt or adjustment to change stock."
                    : "Only use this for initial inventory setup."}
                </small>
              </label>
              <label className="field">
                <span>Initial cost per {unit}</span>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={cost}
                  onChange={(event) => setCost(Number(event.target.value))}
                />
                <small>Used as the opening weighted-average unit cost.</small>
              </label>
            </div>
            <button
              className="advanced-toggle"
              onClick={() => setAdvanced(!advanced)}
            >
              {advanced ? "Hide" : "Add"} receiving unit conversion{" "}
              <ChevronDown />
            </button>
            {advanced && (
              <div className="conversion-editor">
                <div>
                  <strong>Receiving unit</strong>
                  <small>For example, BOX</small>
                  <select
                    value={altUnit}
                    onChange={(event) => setAltUnit(event.target.value)}
                  >
                    <option value="">Choose unit</option>
                    {["BOX", "BAG", "BUNDLE", "KG", "PAIL"].map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </div>
                <span>1 {altUnit || "UNIT"} =</span>
                <div>
                  <strong>Conversion factor</strong>
                  <small>Must be greater than zero</small>
                  <input
                    type="number"
                    min="0.01"
                    value={factor}
                    onChange={(event) => setFactor(Number(event.target.value))}
                  />
                </div>
                <b>{unit}</b>
              </div>
            )}
          </section>
        </div>
        <aside className="form-guidance">
          <ShieldCheck />
          <h2>Inventory-safe setup</h2>
          <p>
            After a product has movement history, stock is changed through
            receipts and adjustments—not by editing this form.
          </p>
          <div>
            <span>Calculated opening value</span>
            <strong>
              $
              {(qty * cost).toLocaleString(undefined, {
                minimumFractionDigits: 2,
              })}
            </strong>
          </div>
          {advanced && altUnit && (
            <div>
              <span>Conversion preview</span>
              <strong>
                1 {altUnit} = {factor} {unit}
              </strong>
            </div>
          )}
        </aside>
      </div>
    </>
  )
}

function ProductDetail({
  sku,
  onBack,
  onEdit,
  onReceive,
  onAdjust,
}: {
  sku: string
  onBack: () => void
  onEdit: () => void
  onReceive: () => void
  onAdjust: () => void
}) {
  const { products, movements } = usePrototype()
  const product = products.find((item) => item.sku === sku)
  const [tab, setTab] = useState("Overview")
  if (!product)
    return (
      <EmptyState
        icon={<Boxes />}
        title="Product not found"
        description="This product may have been removed from the active prototype data."
        action={<Button onClick={onBack}>Return to products</Button>}
      />
    )
  const related = movements.filter(
    (movement) => movement.product === product.name,
  )
  return (
    <>
      <button className="back-link workspace-back" onClick={onBack}>
        ← Back to products
      </button>
      <PageHead
        eyebrow={`Products / ${product.sku}`}
        title={product.name}
        description={product.description}
        actions={
          <>
            <Button tone="secondary" onClick={onAdjust}>
              Adjust stock
            </Button>
            <Button tone="secondary" onClick={onReceive}>
              Receive stock
            </Button>
            <Button onClick={onEdit}>Edit product</Button>
          </>
        }
      />
      <div className="product-detail-hero">
        <div className="detail-image">
          {product.image ? (
            <img src={product.image} alt={product.name} />
          ) : (
            <ImagePlus />
          )}
        </div>
        <div className="detail-summary">
          <div>
            <Badge>{product.status}</Badge>
            <Badge tone={product.active ? "active" : "draft"}>
              {product.active ? "Active product" : "Inactive product"}
            </Badge>
          </div>
          <span className="mono">{product.sku}</span>
          <h2>{product.name}</h2>
          <p>{product.description}</p>
          <dl>
            <dt>Category</dt>
            <dd>{product.category}</dd>
            <dt>Brand</dt>
            <dd>{product.brand || "Not specified"}</dd>
            <dt>Base unit</dt>
            <dd>{product.unit}</dd>
          </dl>
        </div>
        <div className="stock-summary">
          <small>Current stock</small>
          <strong>
            {product.qty.toLocaleString()} <span>{product.unit}</span>
          </strong>
          <Badge>{product.status}</Badge>
          <hr />
          <div>
            <span>Average cost / {product.unit}</span>
            <b>${product.cost.toFixed(2)}</b>
          </div>
          <div>
            <span>Inventory value</span>
            <b>
              $
              {(product.qty * product.cost).toLocaleString(undefined, {
                minimumFractionDigits: 2,
              })}
            </b>
          </div>
          <div>
            <span>Reorder level</span>
            <b>
              {product.reorder} {product.unit}
            </b>
          </div>
        </div>
      </div>
      <div className="report-tabs product-tabs">
        {["Overview", "Stock history", "Units & conversions"].map((item) => (
          <button
            className={tab === item ? "active" : ""}
            onClick={() => setTab(item)}
            key={item}
          >
            {item}
          </button>
        ))}
      </div>
      {tab === "Overview" && (
        <div className="detail-panels">
          <section className="panel detail-card">
            <PanelHead title="Product information" />
            <dl>
              <dt>Description</dt>
              <dd>{product.description}</dd>
              <dt>Barcode</dt>
              <dd>{product.barcode || "Not provided"}</dd>
              <dt>Created</dt>
              <dd>May 2, 2025 by Maya Okafor</dd>
              <dt>Last updated</dt>
              <dd>Today at 09:18</dd>
            </dl>
          </section>
          <section className="panel detail-card">
            <PanelHead title="Stock policy" />
            <dl>
              <dt>Negative stock</dt>
              <dd>Not allowed</dd>
              <dt>Low-stock rule</dt>
              <dd>
                At or below {product.reorder} {product.unit}
              </dd>
              <dt>Cost method</dt>
              <dd>Moving weighted average</dd>
            </dl>
          </section>
        </div>
      )}
      {tab === "Stock history" &&
        (related.length ? (
          <MovementMiniTable movements={related} />
        ) : (
          <EmptyState
            icon={<History />}
            title="No movement history yet"
            description="Posted receipts and adjustments for this product will appear here."
            action={<Button onClick={onReceive}>Receive first stock</Button>}
          />
        ))}
      {tab === "Units & conversions" && (
        <section className="panel detail-card conversion-detail">
          <PanelHead title="Units & conversions" />
          <dl>
            <dt>Base inventory unit</dt>
            <dd>1 {product.unit}</dd>
            <dt>Receiving unit</dt>
            <dd>
              {product.conversion
                ? `1 ${product.conversion.unit} = ${product.conversion.factor} ${product.unit}`
                : "No alternate unit configured"}
            </dd>
          </dl>
          <div className="info-banner">
            <AlertCircle />
            <span>
              Changing a conversion does not change the quantity or cost stored
              on historical movements.
            </span>
          </div>
        </section>
      )}
    </>
  )
}

function MovementMiniTable({ movements }: { movements: Movement[] }) {
  return (
    <div className="panel table-panel">
      <div className="data-table mini-ledger">
        <div className="tr th">
          {[
            "Date",
            "Reference",
            "Type",
            "Quantity",
            "Cost / base unit",
            "User",
          ].map((item) => (
            <span key={item}>{item}</span>
          ))}
        </div>
        {movements.map((movement) => (
          <div className="tr" key={movement.reference}>
            <span>{movement.date}</span>
            <span className="mono">{movement.reference}</span>
            <span>{movement.type}</span>
            <span
              className={
                movement.quantity > 0 ? "number-positive" : "number-negative"
              }
            >
              {movement.quantity > 0 ? "+" : ""}
              {movement.quantity} {movement.unit}
            </span>
            <span>${movement.cost.toFixed(2)}</span>
            <span>{movement.user}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function Categories() {
  const { products, notify } = usePrototype()
  const [categories, setCategories] = useState([
    "Cement",
    "Steel",
    "Roofing",
    "Hardware",
    "Plumbing",
    "Paint",
  ])
  const [adding, setAdding] = useState(false)
  const [name, setName] = useState("")
  const addCategory = () => {
    if (
      !name.trim() ||
      categories.some(
        (category) => category.toLowerCase() === name.trim().toLowerCase(),
      )
    )
      return
    setCategories((items) => [...items, name.trim()])
    setName("")
    setAdding(false)
    notify("Category created and available to product forms.", "success")
  }
  return (
    <>
      <PageHead
        eyebrow="Catalog"
        title="Categories"
        description="Organize products into familiar groups for faster search and reporting."
        actions={
          <Button icon={<Plus />} onClick={() => setAdding(true)}>
            Add category
          </Button>
        }
      />
      <div className="category-grid">
        {categories.map((category) => {
          const count = products.filter(
            (product) => product.category === category,
          ).length
          return (
            <article className="panel category-card" key={category}>
              <span className="category-icon">
                <Boxes />
              </span>
              <div>
                <h2>{category}</h2>
                <p>
                  {count} {count === 1 ? "product" : "products"}
                </p>
              </div>
              <button
                onClick={() =>
                  count
                    ? notify(
                        `${category} cannot be removed while ${count} products use it.`,
                        "error",
                      )
                    : setCategories((items) =>
                        items.filter((item) => item !== category),
                      )
                }
              >
                {count ? "In use" : "Remove"}
              </button>
            </article>
          )
        })}
      </div>
      {!categories.length && (
        <EmptyState
          icon={<Boxes />}
          title="No categories yet"
          description="Create a category before adding your first product."
          action={
            <Button onClick={() => setAdding(true)}>Create category</Button>
          }
        />
      )}
      {adding && (
        <Modal title="Create category" onClose={() => setAdding(false)}>
          <label className="field">
            <span>Category name*</span>
            <input
              autoFocus
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="e.g. Electrical"
            />
            <small>Use a short name your shop staff will recognize.</small>
          </label>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setAdding(false)}>
              Cancel
            </Button>
            <Button disabled={!name.trim()} onClick={addCategory}>
              Create category
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function ReceiveStock({ setActive }: { setActive: (p: ShopPage) => void }) {
  const { products, updateStock, addMovement, notify } = usePrototype()
  const [posted, setPosted] = useState(false)
  const [confirm, setConfirm] = useState(false)
  const [draftSaved, setDraftSaved] = useState(false)
  const [error, setError] = useState("")
  const [lines, setLines] = useState([
    { id: 1, sku: "HDW-NL-3", qty: 5, unit: "BOX", cost: 400 },
  ])
  const calculations = lines.map((line) => {
    const product =
      products.find((item) => item.sku === line.sku) || products[0]
    const factor =
      line.unit === product.unit ? 1 : product.conversion?.factor || 1
    const baseQty = line.qty * factor
    const baseCost = line.cost / factor
    return {
      ...line,
      product,
      factor,
      baseQty,
      baseCost,
      value: line.qty * line.cost,
    }
  })
  const receiptTotal = calculations.reduce((sum, line) => sum + line.value, 0)

  const postReceipt = () => {
    if (
      calculations.some(
        (line) => !line.product || line.qty <= 0 || line.cost < 0,
      )
    ) {
      setError(
        "Every receipt line needs a product, quantity greater than zero, and a valid unit cost.",
      )
      setConfirm(false)
      return
    }
    calculations.forEach((line) => {
      updateStock(
        line.product.sku,
        line.product.qty + line.baseQty,
        line.baseCost,
      )
      addMovement({
        reference: "REC-2025-0048",
        product: line.product.name,
        type: "Stock receipt",
        quantity: line.baseQty,
        unit: line.product.unit,
        cost: line.baseCost,
        user: "Maya O.",
        date: "Today, 10:30",
      })
    })
    setConfirm(false)
    setPosted(true)
    notify("Receipt posted and stock balances updated.", "success")
  }
  if (posted)
    return (
      <div className="document-success">
        <div className="success-icon">
          <Check />
        </div>
        <Badge>Posted</Badge>
        <h1>Receipt posted successfully.</h1>
        <p>
          Inventory balances and weighted-average costs were updated atomically.
        </p>
        <div className="posted-summary">
          <div>
            <span>Receipt reference</span>
            <strong>REC-2025-0048</strong>
          </div>
          <div>
            <span>Inventory cost added</span>
            <strong>
              $
              {receiptTotal.toLocaleString(undefined, {
                minimumFractionDigits: 2,
              })}
            </strong>
          </div>
          <div>
            <span>Movement entries</span>
            <strong>1</strong>
          </div>
        </div>
        <div className="success-actions">
          <Button tone="secondary" onClick={() => setPosted(false)}>
            View receipt
          </Button>
          <Button onClick={() => setActive("Stock ledger")}>
            Open stock ledger
          </Button>
        </div>
      </div>
    )
  if (draftSaved)
    return (
      <div className="document-success">
        <div className="success-icon draft-success">
          <FileText />
        </div>
        <Badge tone="draft">Draft saved</Badge>
        <h1>Receipt REC-2025-0048 is saved as a draft.</h1>
        <p>
          No stock balance or movement record has changed. Reopen the draft when
          you are ready to review and post it.
        </p>
        <div className="success-actions">
          <Button tone="secondary" onClick={() => setActive("Inventory")}>
            Return to inventory
          </Button>
          <Button onClick={() => setDraftSaved(false)}>Reopen draft</Button>
        </div>
      </div>
    )
  return (
    <>
      <PageHead
        eyebrow="Stock receipts / New"
        title="Receive stock"
        description="Record incoming goods. No supplier payment or accounting transaction is created."
        actions={<Badge tone="draft">Draft</Badge>}
      />
      <div className="receipt-layout">
        <div className="receipt-main">
          <section className="panel form-panel">
            <PanelHead title="Receipt details" />
            <div className="field-grid compact">
              <Field label="Receipt reference" value="REC-2025-0048" required />
              <Field
                label="Received date & time"
                value="May 21, 2025 · 10:30"
                required
              />
              <Field
                label="Reference note"
                placeholder="Delivery note or short description"
                wide
              />
            </div>
          </section>
          <section className="panel receipt-items">
            <PanelHead
              title="Items"
              note={`${lines.length} line ${
                lines.length === 1 ? "item" : "items"
              }`}
              action="Add item"
              onAction={() =>
                setLines((items) => [
                  ...items,
                  {
                    id: Date.now(),
                    sku: products[0].sku,
                    qty: 1,
                    unit: products[0].unit,
                    cost: products[0].cost,
                  },
                ])
              }
            />
            {error && (
              <div className="form-error inline-error">
                <AlertCircle />
                <span>{error}</span>
              </div>
            )}
            {calculations.map((line, index) => (
              <div className="receipt-line" key={line.id}>
                <div className="line-top">
                  <span>{String(index + 1).padStart(2, "0")}</span>
                  <div>
                    <strong>{line.product.name}</strong>
                    <small>
                      {line.product.sku} · Base unit: {line.product.unit}
                    </small>
                  </div>
                  <button
                    disabled={lines.length === 1}
                    onClick={() =>
                      setLines((items) =>
                        items.filter((item) => item.id !== line.id),
                      )
                    }
                    aria-label="Remove receipt item"
                  >
                    <X />
                  </button>
                </div>
                <div className="line-fields">
                  <label className="field">
                    <span>Product*</span>
                    <select
                      value={line.sku}
                      onChange={(event) => {
                        const selected = products.find(
                          (product) => product.sku === event.target.value,
                        )!
                        setLines((items) =>
                          items.map((item) =>
                            item.id === line.id
                              ? {
                                  ...item,
                                  sku: selected.sku,
                                  unit:
                                    selected.conversion?.unit || selected.unit,
                                  cost: selected.conversion
                                    ? selected.cost * selected.conversion.factor
                                    : selected.cost,
                                }
                              : item,
                          ),
                        )
                      }}
                    >
                      {products.map((product) => (
                        <option value={product.sku} key={product.sku}>
                          {product.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field">
                    <span>Quantity received*</span>
                    <input
                      type="number"
                      min="0.01"
                      value={line.qty}
                      onChange={(event) =>
                        setLines((items) =>
                          items.map((item) =>
                            item.id === line.id
                              ? { ...item, qty: Number(event.target.value) }
                              : item,
                          ),
                        )
                      }
                    />
                  </label>
                  <label className="field">
                    <span>Entry unit*</span>
                    <select
                      value={line.unit}
                      onChange={(event) =>
                        setLines((items) =>
                          items.map((item) =>
                            item.id === line.id
                              ? { ...item, unit: event.target.value }
                              : item,
                          ),
                        )
                      }
                    >
                      <option>{line.product.unit}</option>
                      {line.product.conversion && (
                        <option>{line.product.conversion.unit}</option>
                      )}
                    </select>
                  </label>
                  <label className="field">
                    <span>Cost per {line.unit}*</span>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={line.cost}
                      onChange={(event) =>
                        setLines((items) =>
                          items.map((item) =>
                            item.id === line.id
                              ? { ...item, cost: Number(event.target.value) }
                              : item,
                          ),
                        )
                      }
                    />
                  </label>
                </div>
                <div className="conversion-summary">
                  <div>
                    <span>Product conversion</span>
                    <strong>
                      1 {line.unit} = {line.factor} {line.product.unit}
                    </strong>
                  </div>
                  <ChevronRight />
                  <div>
                    <span>Calculated stock increase</span>
                    <strong>
                      {line.baseQty.toLocaleString()} {line.product.unit}
                    </strong>
                  </div>
                  <div>
                    <span>Cost / base unit</span>
                    <strong>
                      ${line.baseCost.toFixed(2)} / {line.product.unit}
                    </strong>
                  </div>
                  <div className="line-total">
                    <span>Line value</span>
                    <strong>
                      $
                      {line.value.toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                      })}
                    </strong>
                  </div>
                </div>
              </div>
            ))}
          </section>
          <div className="unsaved-note">
            <AlertCircle />
            <span>
              You have unsaved changes. Draft receipts do not affect inventory.
            </span>
          </div>
        </div>
        <aside className="receipt-summary">
          <h2>Receipt summary</h2>
          <div>
            <span>Line items</span>
            <strong>{lines.length}</strong>
          </div>
          <div>
            <span>Total base quantity</span>
            <strong>
              {calculations
                .reduce((sum, line) => sum + line.baseQty, 0)
                .toLocaleString()}{" "}
              base units
            </strong>
          </div>
          <hr />
          <div className="summary-total">
            <span>Total inventory cost</span>
            <strong>
              $
              {receiptTotal.toLocaleString(undefined, {
                minimumFractionDigits: 2,
              })}
            </strong>
          </div>
          <Button onClick={() => setConfirm(true)}>Review & post</Button>
          <Button
            tone="secondary"
            onClick={() => {
              setDraftSaved(true)
              notify(
                "Draft receipt saved. Inventory was not changed.",
                "success",
              )
            }}
          >
            Save draft
          </Button>
          <button
            className="cancel-link"
            onClick={() => setActive("Inventory")}
          >
            Cancel receipt
          </button>
          <div className="posting-rule">
            <ShieldCheck />
            <p>
              <strong>Posting is permanent.</strong> It changes stock and
              creates ledger records. Corrections require a controlled
              adjustment or reversal.
            </p>
          </div>
        </aside>
      </div>
      {confirm && (
        <Modal
          title="Post receipt REC-2025-0048?"
          onClose={() => setConfirm(false)}
        >
          <div className="modal-alert">
            <TriangleAlert />
            <span>
              This action will immediately increase stock balances and
              recalculate moving average cost.
            </span>
          </div>
          <div className="review-list">
            <span>1 line item</span>
            <strong>
              {calculations
                .map(
                  (line) =>
                    `${line.product.name} · +${line.baseQty.toLocaleString()} ${line.product.unit}`,
                )
                .join(", ")}
            </strong>
            <span>Total inventory cost</span>
            <strong>
              $
              {receiptTotal.toLocaleString(undefined, {
                minimumFractionDigits: 2,
              })}
            </strong>
          </div>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setConfirm(false)}>
              Go back
            </Button>
            <Button onClick={postReceipt}>Confirm & post receipt</Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function Modal({
  title,
  children,
  onClose,
}: {
  title: string
  children: React.ReactNode
  onClose: () => void
}) {
  useEffect(() => {
    const close = (event: KeyboardEvent) => event.key === "Escape" && onClose()
    window.addEventListener("keydown", close)
    return () => window.removeEventListener("keydown", close)
  }, [onClose])
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className="modal">
        <div className="modal-head">
          <h2>{title}</h2>
          <button onClick={onClose}>
            <X />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

function Adjustments({ setActive }: { setActive: (page: ShopPage) => void }) {
  const { products, updateStock, addMovement, notify } = usePrototype()
  const [sku, setSku] = useState("CEM-OPC-50")
  const product = products.find((item) => item.sku === sku) || products[0]
  const [count, setCount] = useState(String(product.qty))
  const [reason, setReason] = useState("Stock count correction")
  const [notes, setNotes] = useState("")
  const [confirm, setConfirm] = useState(false)
  const [error, setError] = useState("")
  const diff = Number(count) - product.qty
  const submit = () => {
    if (!reason || !notes.trim()) {
      setError(
        "Choose a reason and add notes that explain the verified stock difference.",
      )
      setConfirm(false)
      return
    }
    if (Number(count) < 0) {
      setError(
        "The adjustment would create a negative stock balance. Enter zero or a positive counted quantity.",
      )
      setConfirm(false)
      return
    }
    updateStock(product.sku, Number(count))
    addMovement({
      reference: `ADJ-2025-${String(Date.now()).slice(-4)}`,
      product: product.name,
      type: diff >= 0 ? "Adjustment in" : "Adjustment out",
      quantity: diff,
      unit: product.unit,
      cost: product.cost,
      user: "Maya O.",
      date: "Today, 11:06",
    })
    notify("Stock adjustment posted and movement history updated.", "success")
    setActive("Stock ledger")
  }
  return (
    <>
      <PageHead
        eyebrow="Stock adjustments"
        title="Create stock adjustment"
        description="Correct a verified stock difference without editing historical movements."
        actions={<Badge tone="draft">Draft</Badge>}
      />
      {error && (
        <div className="form-error">
          <AlertCircle />
          <span>{error}</span>
        </div>
      )}
      <div className="two-column-form">
        <section className="panel form-panel">
          <PanelHead title="Adjustment details" />
          <div className="field-grid">
            <label className="field wide">
              <span>Product*</span>
              <select
                value={sku}
                onChange={(event) => {
                  const next = products.find(
                    (item) => item.sku === event.target.value,
                  )!
                  setSku(next.sku)
                  setCount(String(next.qty))
                }}
              >
                {products.map((item) => (
                  <option value={item.sku} key={item.sku}>
                    {item.name} · {item.sku}
                  </option>
                ))}
              </select>
              <small>
                Choose the product whose physical count was verified.
              </small>
            </label>
            <div className="current-balance wide">
              <span>Current system balance</span>
              <strong>
                {product.qty} {product.unit}
              </strong>
              <small>
                Average cost: ${product.cost.toFixed(2)} / {product.unit}
              </small>
            </div>
            <label className="field">
              <span>Reason*</span>
              <select
                value={reason}
                onChange={(event) => setReason(event.target.value)}
              >
                {[
                  "Stock count correction",
                  "Damage",
                  "Loss",
                  "Found stock",
                  "Correction",
                  "Other",
                ].map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Adjustment method</span>
              <select>
                <option>Enter counted quantity</option>
              </select>
              <small>We calculate the increase or decrease for you.</small>
            </label>
            <label className="field">
              <span>Counted quantity*</span>
              <input value={count} onChange={(e) => setCount(e.target.value)} />
            </label>
            <div className="difference-field">
              <span>Calculated difference</span>
              <strong>
                {diff > 0 ? "+" : ""}
                {diff} {product.unit}
              </strong>
              <small>
                Inventory value change: ${(diff * product.cost).toFixed(2)}
              </small>
            </div>
            <label className="field wide">
              <span>Notes*</span>
              <textarea
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Explain what was counted and why it differs"
              />
            </label>
          </div>
        </section>
        <aside className="review-card">
          <h2>Review adjustment</h2>
          <div className="balance-change">
            <div>
              <span>Before</span>
              <strong>
                {product.qty} {product.unit}
              </strong>
            </div>
            <ArrowRight />
            <div>
              <span>After</span>
              <strong>
                {count} {product.unit}
              </strong>
            </div>
          </div>
          <div className={diff < 0 ? "decrease" : "decrease increase"}>
            {diff < 0 ? <Minus /> : <Plus />} Stock will{" "}
            {diff < 0 ? "decrease" : "increase"} by {Math.abs(diff)}{" "}
            {product.unit}
          </div>
          <p>
            Negative adjustments use the current weighted-average cost of{" "}
            <strong>
              ${product.cost.toFixed(2)} / {product.unit}
            </strong>
            .
          </p>
          <Button disabled={diff === 0} onClick={() => setConfirm(true)}>
            Review & confirm
          </Button>
          <Button
            tone="secondary"
            onClick={() =>
              notify(
                "Adjustment draft saved. Stock was not changed.",
                "success",
              )
            }
          >
            Save draft
          </Button>
        </aside>
      </div>
      {confirm && (
        <Modal
          title={`Post ${reason.toLowerCase()}?`}
          onClose={() => setConfirm(false)}
        >
          <div className="modal-alert">
            <TriangleAlert />
            <span>
              This will change {product.name} from {product.qty} to {count}{" "}
              {product.unit} and create a permanent movement record.
            </span>
          </div>
          <div className="review-list">
            <span>Quantity change</span>
            <strong>
              {diff > 0 ? "+" : ""}
              {diff} {product.unit}
            </strong>
            <span>Value effect</span>
            <strong>${(diff * product.cost).toFixed(2)}</strong>
          </div>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setConfirm(false)}>
              Go back
            </Button>
            <Button onClick={submit}>Confirm adjustment</Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function Ledger() {
  const { movements, notify } = usePrototype()
  const [search, setSearch] = useState("")
  const [type, setType] = useState("All movement types")
  const [selected, setSelected] = useState<Movement>()
  const rows = movements.filter(
    (movement) =>
      `${movement.product} ${movement.reference}`
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (type === "All movement types" || movement.type === type),
  )
  const exportLedger = () => {
    downloadCsv("stockforge-stock-movements.csv", [
      [
        "Date",
        "Reference",
        "Product",
        "Type",
        "Quantity",
        "Unit",
        "Cost",
        "User",
      ],
      ...rows.map((item) => [
        item.date,
        item.reference,
        item.product,
        item.type,
        item.quantity,
        item.unit,
        item.cost,
        item.user,
      ]),
    ])
    notify("Movement ledger export downloaded.", "success")
  }
  return (
    <>
      <PageHead
        eyebrow="Inventory"
        title="Stock movement ledger"
        description="A read-only, audit-friendly record of every posted stock change."
        actions={
          <Button tone="secondary" onClick={exportLedger}>
            Export ledger
          </Button>
        }
      />
      <div className="panel table-panel">
        <div className="table-tools">
          <div className="table-search">
            <Search />
            <input
              placeholder="Search product or reference"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <select
            value={type}
            onChange={(event) => setType(event.target.value)}
          >
            <option>All movement types</option>
            <option>Stock receipt</option>
            <option>Adjustment in</option>
            <option>Adjustment out</option>
          </select>
          {(search || type !== "All movement types") && (
            <button
              className="reset-filter"
              onClick={() => {
                setSearch("")
                setType("All movement types")
              }}
            >
              Reset filters
            </button>
          )}
        </div>
        <div className="data-table ledger-table">
          <div className="tr th">
            {[
              "Date & time",
              "Reference",
              "Product",
              "Type",
              "Quantity change",
              "Cost / base unit",
              "Value change",
              "User",
            ].map((x) => (
              <span key={x}>{x}</span>
            ))}
          </div>
          {rows.map((movement) => (
            <button
              className="tr ledger-row"
              key={movement.reference}
              onClick={() => setSelected(movement)}
            >
              <span>{movement.date}</span>
              <span className="mono linkish">{movement.reference}</span>
              <span>{movement.product}</span>
              <span>{movement.type}</span>
              <span
                className={
                  movement.quantity > 0 ? "number-positive" : "number-negative"
                }
              >
                {movement.quantity > 0 ? "+" : ""}
                {movement.quantity.toLocaleString()} {movement.unit}
              </span>
              <span>${movement.cost.toFixed(2)}</span>
              <span
                className={
                  movement.quantity > 0 ? "number-positive" : "number-negative"
                }
              >
                {movement.quantity > 0 ? "+" : ""}$
                {Math.abs(movement.quantity * movement.cost).toLocaleString(
                  undefined,
                  { minimumFractionDigits: 2 },
                )}
              </span>
              <span>{movement.user}</span>
            </button>
          ))}
        </div>
        {!rows.length && (
          <EmptyState
            icon={<Search />}
            title="No movements found"
            description="Try another product, reference, or movement type."
            action={
              <Button
                tone="secondary"
                onClick={() => {
                  setSearch("")
                  setType("All movement types")
                }}
              >
                Clear filters
              </Button>
            }
          />
        )}
        <div className="pagination">
          <span>
            Showing {rows.length} of {movements.length} movements
          </span>
          <div>
            <button disabled>Previous</button>
            <button className="active" disabled>
              1
            </button>
            <button disabled>Next</button>
          </div>
        </div>
      </div>
      {selected && (
        <Modal
          title={`Movement ${selected.reference}`}
          onClose={() => setSelected(undefined)}
        >
          <div className="approval-grid">
            <div>
              <span>Product</span>
              <strong>{selected.product}</strong>
            </div>
            <div>
              <span>Movement type</span>
              <strong>{selected.type}</strong>
            </div>
            <div>
              <span>Quantity change</span>
              <strong>
                {selected.quantity > 0 ? "+" : ""}
                {selected.quantity} {selected.unit}
              </strong>
            </div>
            <div>
              <span>Cost / base unit</span>
              <strong>${selected.cost.toFixed(2)}</strong>
            </div>
            <div>
              <span>Actor</span>
              <strong>{selected.user}</strong>
            </div>
            <div>
              <span>Recorded</span>
              <strong>{selected.date}</strong>
            </div>
          </div>
          <div className="atomic-note">
            <ShieldCheck />
            <p>
              This ledger entry is read-only. Corrections require a controlled
              reversal or compensating adjustment.
            </p>
          </div>
          <div className="modal-actions">
            <Button onClick={() => setSelected(undefined)}>
              Close details
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function Reports() {
  const { products, movements, notify } = usePrototype()
  const tabs = [
    "Current stock",
    "Inventory valuation",
    "Low stock",
    "Stock movements",
    "Receipts",
    "Adjustments",
  ]
  const [activeTab, setActiveTab] = useState("Inventory valuation")
  const reportProducts =
    activeTab === "Low stock"
      ? products.filter((product) => product.status !== "In stock")
      : products
  const totalValue = reportProducts.reduce(
    (sum, product) => sum + product.qty * product.cost,
    0,
  )
  const exportReport = () => {
    downloadCsv("stockforge-inventory-report.csv", [
      [
        "SKU",
        "Product",
        "Quantity",
        "Unit",
        "Average cost",
        "Inventory value",
        "Status",
      ],
      ...reportProducts.map((product) => [
        product.sku,
        product.name,
        product.qty,
        product.unit,
        product.cost,
        product.qty * product.cost,
        product.status,
      ]),
    ])
    notify(`${activeTab} export downloaded.`, "success")
  }
  return (
    <>
      <PageHead
        eyebrow="Reports"
        title="Inventory reports"
        description="Operational inventory values—not cash balances, sales, or accounting profit."
        actions={
          <>
            <Button tone="secondary" onClick={() => window.print()}>
              Print report
            </Button>
            <Button onClick={exportReport}>Export report</Button>
          </>
        }
      />
      <div className="report-tabs">
        {tabs.map((t) => (
          <button
            className={activeTab === t ? "active" : ""}
            key={t}
            onClick={() => setActiveTab(t)}
          >
            {t}
          </button>
        ))}
      </div>
      <div className="report-header">
        <div>
          <small>{activeTab.toUpperCase()} REPORT</small>
          <h2>Northstar Building Supply</h2>
          <span>As of May 21, 2025 · Currency: USD</span>
        </div>
        <div className="report-total">
          <span>Total inventory value</span>
          <strong>
            $
            {totalValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}
          </strong>
          <small>Across {reportProducts.length} active products</small>
        </div>
      </div>
      {activeTab === "Stock movements" ||
      activeTab === "Receipts" ||
      activeTab === "Adjustments" ? (
        <MovementMiniTable
          movements={movements.filter((movement) =>
            activeTab === "Receipts"
              ? movement.type === "Stock receipt"
              : activeTab === "Adjustments"
                ? movement.type.startsWith("Adjustment")
                : true,
          )}
        />
      ) : (
        <div className="panel table-panel">
          <div className="data-table report-product-table">
            <div className="tr th">
              {[
                "SKU",
                "Product",
                "Base unit",
                "Current quantity",
                "Average cost",
                "Inventory value",
                "Status",
              ].map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>
            {reportProducts.map((product) => (
              <div className="tr" key={product.sku}>
                <span className="mono">{product.sku}</span>
                <span>
                  <strong>{product.name}</strong>
                  <small>{product.description}</small>
                </span>
                <span>{product.unit}</span>
                <span>
                  {product.qty.toLocaleString()} {product.unit}
                </span>
                <span>${product.cost.toFixed(2)}</span>
                <span>
                  $
                  {(product.qty * product.cost).toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                  })}
                </span>
                <span>
                  <Badge>{product.status}</Badge>
                </span>
              </div>
            ))}
          </div>
          {!reportProducts.length && (
            <EmptyState
              icon={<FileBarChart />}
              title="No records for this report"
              description="Adjust the report selection or add inventory activity first."
            />
          )}
        </div>
      )}
    </>
  )
}

function UsersRoles() {
  const { users, saveUser, notify } = usePrototype()
  const [editing, setEditing] = useState<ShopUser | "new">()
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [role, setRole] = useState<ShopUser["role"]>("VIEWER")
  const [error, setError] = useState("")
  const openEditor = (user?: ShopUser) => {
    setEditing(user || "new")
    setName(user?.name || "")
    setEmail(user?.email || "")
    setRole(user?.role || "VIEWER")
    setError("")
  }
  const submit = () => {
    if (!name.trim() || !email.includes("@")) {
      setError("Enter the staff member’s name and a valid email address.")
      return
    }
    const current = editing === "new" ? undefined : editing
    saveUser({
      id: current?.id || `u${Date.now()}`,
      name: name.trim(),
      email: email.trim(),
      role,
      status: current?.status || "Invitation pending",
    })
    setEditing(undefined)
    notify(
      current
        ? "User details and role updated."
        : "Staff invitation added to the prototype.",
      "success",
    )
  }
  return (
    <>
      <PageHead
        eyebrow="Access"
        title="Users & roles"
        description="Manage staff access within Northstar Building Supply only."
        actions={
          <Button icon={<Plus />} onClick={() => openEditor()}>
            Invite staff member
          </Button>
        }
      />
      <div className="panel table-panel">
        <div className="data-table users-table">
          <div className="tr th">
            <span>User</span>
            <span>Role</span>
            <span>Status</span>
            <span>Last active</span>
            <span />
          </div>
          {users.map((user) => (
            <div className="tr" key={user.id}>
              <span className="user-cell">
                <i>
                  {user.name
                    .split(" ")
                    .map((part) => part[0])
                    .join("")}
                </i>
                <span>
                  <strong>{user.name}</strong>
                  <small>{user.email}</small>
                </span>
              </span>
              <span>
                <Badge tone="role">{user.role}</Badge>
              </span>
              <span>
                <Badge
                  tone={
                    user.status === "Active"
                      ? "active"
                      : user.status === "Disabled"
                        ? "suspended"
                        : "pending"
                  }
                >
                  {user.status}
                </Badge>
              </span>
              <span>
                {user.role === "OWNER"
                  ? "Now"
                  : user.status === "Active"
                    ? "2h ago"
                    : "—"}
              </span>
              <span className="row-actions">
                <button onClick={() => openEditor(user)}>Edit</button>
                {user.role !== "OWNER" && (
                  <button
                    onClick={() => {
                      saveUser({
                        ...user,
                        status:
                          user.status === "Disabled" ? "Active" : "Disabled",
                      })
                      notify(
                        `${user.name} is now ${
                          user.status === "Disabled" ? "active" : "disabled"
                        }.`,
                        "success",
                      )
                    }}
                  >
                    {user.status === "Disabled" ? "Activate" : "Disable"}
                  </button>
                )}
              </span>
            </div>
          ))}
        </div>
      </div>
      <section className="panel permissions">
        <PanelHead
          title="Role permissions"
          note="Suggested V1 permission matrix"
        />
        <div className="permission-grid">
          <div />
          <strong>Owner</strong>
          <strong>Manager</strong>
          <strong>Storekeeper</strong>
          <strong>Viewer</strong>
          {[
            "View inventory",
            "Manage products",
            "Receive stock",
            "Adjust stock",
            "Manage staff",
            "View subscription",
          ].flatMap((p, row) => [
            <span key={`${p}-l`}>{p}</span>,
            ...["o", "m", "s", "v"].map((r, i) => (
              <i key={`${p}-${r}`}>
                {i === 0 ||
                row === 0 ||
                (i === 1 && row < 4) ||
                (i === 2 && row > 1 && row < 4) ? (
                  <Check />
                ) : (
                  "—"
                )}
              </i>
            )),
          ])}
        </div>
      </section>
      {editing && (
        <Modal
          title={
            editing === "new" ? "Invite shop user" : `Edit ${editing.name}`
          }
          onClose={() => setEditing(undefined)}
        >
          {error && (
            <div className="form-error inline-error">
              <AlertCircle />
              <span>{error}</span>
            </div>
          )}
          <div className="field-grid modal-fields">
            <label className="field wide">
              <span>Full name*</span>
              <input
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Staff member’s name"
              />
            </label>
            <label className="field wide">
              <span>Email address*</span>
              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="name@shop.example"
              />
            </label>
            <label className="field wide">
              <span>Shop role*</span>
              <select
                value={role}
                disabled={editing !== "new" && editing.role === "OWNER"}
                onChange={(event) =>
                  setRole(event.target.value as ShopUser["role"])
                }
              >
                {["MANAGER", "STOREKEEPER", "VIEWER"].map((item) => (
                  <option key={item}>{item}</option>
                ))}
                {editing !== "new" && editing.role === "OWNER" && (
                  <option>OWNER</option>
                )}
              </select>
              <small>
                {role === "MANAGER"
                  ? "Can manage products and stock operations, but not platform administration."
                  : role === "STOREKEEPER"
                    ? "Can receive stock and perform permitted adjustments."
                    : "Read-only access to inventory and reports."}
              </small>
            </label>
          </div>
          <div className="atomic-note">
            <ShieldCheck />
            <p>
              Shop roles never grant platform-administrator access. Backend
              authorization must enforce tenant membership.
            </p>
          </div>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setEditing(undefined)}>
              Cancel
            </Button>
            <Button onClick={submit}>
              {editing === "new" ? "Send invitation" : "Save user changes"}
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function Subscription() {
  const { notify } = usePrototype()
  return (
    <>
      <PageHead
        eyebrow="Account"
        title="Subscription"
        description="Plan access and activation are managed separately from your shop’s inventory."
      />
      <div className="subscription-hero">
        <div>
          <Badge>Active</Badge>
          <h2>Growth plan</h2>
          <p>For active materials shops with a growing team.</p>
          <div className="subscription-dates">
            <div>
              <span>Current period</span>
              <strong>May 1 – May 31, 2025</strong>
            </div>
            <div>
              <span>Renews / expires</span>
              <strong>May 31, 2025</strong>
            </div>
          </div>
        </div>
        <div className="plan-cost">
          <small>Monthly plan</small>
          <strong>
            $59<span>/ month</span>
          </strong>
          <Button
            tone="secondary"
            onClick={() =>
              notify(
                "Support request drafted for subscription assistance.",
                "success",
              )
            }
          >
            Contact plan administrator
          </Button>
        </div>
      </div>
      <div className="subscription-grid">
        <section className="panel">
          <PanelHead title="Plan usage" />
          <div className="usage-item">
            <div>
              <span>Products</span>
              <strong>126 of 1,000</strong>
            </div>
            <div className="usage-track">
              <i style={{ width: "13%" }} />
            </div>
          </div>
          <div className="usage-item">
            <div>
              <span>Staff users</span>
              <strong>4 of 10</strong>
            </div>
            <div className="usage-track">
              <i style={{ width: "40%" }} />
            </div>
          </div>
        </section>
        <section className="panel">
          <PanelHead title="Included features" />
          <ul className="included-list">
            {[
              "Core inventory management",
              "Product-specific unit conversions",
              "Moving average valuation",
              "Advanced inventory reports",
              "Role-based staff access",
            ].map((x) => (
              <li key={x}>
                <Check />
                {x}
              </li>
            ))}
          </ul>
        </section>
      </div>
      <div className="info-banner">
        <AlertCircle />
        <span>
          StockForge uses administrator-managed subscriptions in V1. No payment
          method or payment history is stored here. Contact support for renewal
          or plan changes.
        </span>
      </div>
    </>
  )
}

function ShopSettings() {
  const { notify } = usePrototype()
  const [shopName, setShopName] = useState("Northstar Building Supply")
  const [email, setEmail] = useState("inventory@northstar.example")
  const [currency, setCurrency] = useState("USD — US Dollar")
  return (
    <>
      <PageHead
        eyebrow="Shop"
        title="Shop settings"
        description="Update contact and regional preferences for this tenant only."
        actions={
          <Button onClick={() => notify("Shop settings saved.", "success")}>
            Save settings
          </Button>
        }
      />
      <div className="settings-grid">
        <section className="panel form-panel">
          <PanelHead title="Shop profile" />
          <div className="field-grid">
            <label className="field">
              <span>Shop name*</span>
              <input
                value={shopName}
                onChange={(event) => setShopName(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Contact email*</span>
              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
            <Field label="Phone number" value="+1 555 0182" />
            <Field label="Address" value="84 Foundry Road, Cedar Grove" />
          </div>
        </section>
        <section className="panel form-panel">
          <PanelHead title="Inventory preferences" />
          <div className="field-grid">
            <label className="field">
              <span>Currency*</span>
              <select
                value={currency}
                onChange={(event) => setCurrency(event.target.value)}
              >
                <option>USD — US Dollar</option>
                <option>EUR — Euro</option>
                <option>GBP — British Pound</option>
              </select>
              <small>
                Used consistently for inventory costs and valuation reports.
              </small>
            </label>
            <Field label="Timezone" value="UTC−05:00 Eastern Time" select />
            <Field label="Quantity precision" value="2 decimal places" select />
            <Field label="Negative stock" value="Not allowed" select />
          </div>
        </section>
      </div>
      <div className="logic-note">
        <ShieldCheck />
        <span>
          <strong>Tenant identity is fixed:</strong> These settings cannot move
          records to another shop or expose another tenant’s data.
        </span>
      </div>
    </>
  )
}

function AdminPortal() {
  const [active, setActive] = useState<AdminPage>("Overview")
  return (
    <Shell mode="admin" active={active} setActive={setActive}>
      {active === "Overview" && <AdminOverview setActive={setActive} />}
      {active === "Registration requests" && <RegistrationRequests />}
      {active === "Shops / tenants" && <Tenants />}
      {active === "Plans" && <Plans />}
      {active === "Subscriptions" && <Subscriptions />}
      {active === "Platform users" && <PlatformUsers />}
      {active === "Audit logs" && <AuditLogs />}
      {active === "Settings" && <AdminSettings />}
    </Shell>
  )
}

function AdminOverview({ setActive }: { setActive: (p: AdminPage) => void }) {
  const { requests } = usePrototype()
  return (
    <>
      <PageHead
        eyebrow="Platform administration"
        title="Platform overview"
        description="Tenant and subscription health across StockForge."
        actions={
          <Button onClick={() => setActive("Registration requests")}>
            Review pending requests <ArrowRight />
          </Button>
        }
      />
      <div className="metric-grid admin-metrics">
        <Metric
          icon={<Building2 />}
          label="Registered tenants"
          value="84"
          meta="72 active"
          positive
          onClick={() => setActive("Shops / tenants")}
        />
        <Metric
          icon={<ClipboardCheck />}
          label="Pending requests"
          value="3"
          meta="Oldest: 2 days"
          warning
          onClick={() => setActive("Registration requests")}
        />
        <Metric
          icon={<CircleDollarSign />}
          label="Active subscriptions"
          value="68"
          meta="81% of tenants"
          positive
          onClick={() => setActive("Subscriptions")}
        />
        <Metric
          icon={<Clock3 />}
          label="Expiring in 30 days"
          value="7"
          meta="2 require attention"
          warning
          onClick={() => setActive("Subscriptions")}
        />
      </div>
      <div className="dashboard-grid admin-dashboard">
        <section className="panel admin-chart">
          <PanelHead
            title="Tenant growth"
            note="Last 6 months"
            action="View tenants"
          />
          <div className="bar-chart">
            {[36, 48, 43, 64, 70, 82].map((h, i) => (
              <div key={i}>
                <i style={{ height: `${h}%` }} />
                <span>{["Dec", "Jan", "Feb", "Mar", "Apr", "May"][i]}</span>
              </div>
            ))}
          </div>
        </section>
        <section className="panel distribution">
          <PanelHead title="Subscription status" note="84 tenants" />
          <div className="donut">
            <div>
              <strong>84</strong>
              <span>Total</span>
            </div>
          </div>
          <ul>
            <li>
              <i className="active" />
              Active <strong>68</strong>
            </li>
            <li>
              <i className="trial" />
              Trialing <strong>8</strong>
            </li>
            <li>
              <i className="pending" />
              Pending <strong>5</strong>
            </li>
            <li>
              <i className="suspended" />
              Suspended / expired <strong>3</strong>
            </li>
          </ul>
        </section>
        <section className="panel requests-panel">
          <PanelHead
            title="Recent registration requests"
            note="3 awaiting review"
            action="View all requests"
          />
          {requests.slice(0, 3).map((r) => (
            <div className="request-row" key={r.ref}>
              <span className="tenant-logo">{r.shop.charAt(0)}</span>
              <div>
                <strong>{r.shop}</strong>
                <small>
                  {r.owner} · {r.ref}
                </small>
              </div>
              <span>{r.plan}</span>
              <time>{r.date}</time>
              <Badge>{r.status}</Badge>
              <button onClick={() => setActive("Registration requests")}>
                Review <ChevronRight />
              </button>
            </div>
          ))}
        </section>
        <section className="panel action-feed">
          <PanelHead title="Administrative activity" note="Audit recorded" />
          <div>
            <span>
              <Check />
            </span>
            <p>
              <strong>Subscription activated</strong>
              <small>Westline Roofing Depot · by Ari Lane</small>
            </p>
            <time>28m</time>
          </div>
          <div>
            <span>
              <Building2 />
            </span>
            <p>
              <strong>Tenant approved</strong>
              <small>MetroStone Hardware · by Ari Lane</small>
            </p>
            <time>2h</time>
          </div>
          <div>
            <span>
              <FileText />
            </span>
            <p>
              <strong>Growth plan updated</strong>
              <small>Product limit changed · by Jo Kim</small>
            </p>
            <time>1d</time>
          </div>
        </section>
      </div>
    </>
  )
}

function RegistrationRequests() {
  const { requests, updateRequest, notify } = usePrototype()
  const [selected, setSelected] = useState<string>()
  const [confirm, setConfirm] = useState(false)
  const [approved, setApproved] = useState(false)
  const [decision, setDecision] = useState<"reject" | "info">()
  const [decisionNote, setDecisionNote] = useState("")
  const [filter, setFilter] = useState("All")
  const [search, setSearch] = useState("")
  const request = requests.find((item) => item.ref === selected)
  const filteredRequests = requests.filter(
    (item) =>
      (filter === "All" || item.status === filter) &&
      `${item.ref} ${item.shop} ${item.owner}`
        .toLowerCase()
        .includes(search.toLowerCase()),
  )
  const completeDecision = () => {
    if (!request || !decisionNote.trim()) return
    updateRequest(request.ref, decision === "reject" ? "Rejected" : "More info")
    notify(
      decision === "reject"
        ? "Registration rejected and history updated."
        : "Information request recorded for the applicant.",
      "success",
    )
    setDecision(undefined)
    setDecisionNote("")
    setSelected(undefined)
  }
  if (approved)
    return (
      <div className="document-success admin-success">
        <div className="success-icon">
          <Check />
        </div>
        <Badge>Approved</Badge>
        <h1>Northstar Building Supply is ready for setup.</h1>
        <p>
          The tenant, owner membership, and Growth subscription were created in
          one transaction. An account setup invitation is queued for Maya.
        </p>
        <div className="posted-summary">
          <div>
            <span>Tenant ID</span>
            <strong>northstar-building</strong>
          </div>
          <div>
            <span>Subscription</span>
            <strong>Growth · Pending activation</strong>
          </div>
          <div>
            <span>Invitation</span>
            <strong>Queued</strong>
          </div>
        </div>
        <Button
          onClick={() => {
            setApproved(false)
            setSelected(undefined)
            notify("Tenant is now available in Shops / tenants.", "success")
          }}
        >
          Return to requests
        </Button>
      </div>
    )
  if (request)
    return (
      <>
        <PageHead
          eyebrow={`Registration requests / ${request.ref}`}
          title={request.shop}
          description={`Submitted ${request.date} by ${request.owner}.`}
          actions={
            <>
              <Button tone="danger" onClick={() => setDecision("reject")}>
                Reject registration
              </Button>
              <Button tone="secondary" onClick={() => setDecision("info")}>
                Request more information
              </Button>
              <Button onClick={() => setConfirm(true)}>Approve request</Button>
            </>
          }
        />
        <div className="review-layout">
          <div>
            <section className="panel detail-card">
              <PanelHead title="Shop details" />
              <dl>
                <dt>Shop name</dt>
                <dd>{request.shop}</dd>
                <dt>Shop identifier</dt>
                <dd className="mono">northstar-building</dd>
                <dt>Address</dt>
                <dd>84 Foundry Road, Cedar Grove</dd>
                <dt>Country / currency</dt>
                <dd>United States / USD</dd>
              </dl>
            </section>
            <section className="panel detail-card">
              <PanelHead title="Owner & contact" />
              <dl>
                <dt>Owner</dt>
                <dd>{request.owner}</dd>
                <dt>Email</dt>
                <dd>maya@northstar.example</dd>
                <dt>Phone</dt>
                <dd>+1 555 0144</dd>
              </dl>
            </section>
            <section className="panel detail-card">
              <PanelHead title="Application notes" />
              <p>
                We are moving from paper records and need to track packaging
                conversions for nails, cement, and roofing materials.
              </p>
            </section>
          </div>
          <aside>
            <section className="panel decision-card">
              <Badge>{request.status}</Badge>
              <h2>Review summary</h2>
              <div>
                <span>Requested plan</span>
                <strong>{request.plan}</strong>
              </div>
              <div>
                <span>Billing preference</span>
                <strong>Monthly</strong>
              </div>
              <div>
                <span>Submitted</span>
                <strong>{request.date}</strong>
              </div>
              <div className="validation-ok">
                <CheckCircle2 />
                <span>No duplicate email or tenant slug detected.</span>
              </div>
            </section>
            <section className="panel history-card">
              <PanelHead title="Decision history" />
              <div>
                <span />
                <p>
                  <strong>Request submitted</strong>
                  <small>May 21 · 09:42</small>
                </p>
              </div>
              <div>
                <span />
                <p>
                  <strong>Automated validation passed</strong>
                  <small>May 21 · 09:43</small>
                </p>
              </div>
            </section>
          </aside>
        </div>
        {confirm && (
          <Modal
            title={`Approve ${request.shop}?`}
            onClose={() => setConfirm(false)}
          >
            <div className="approval-grid">
              <div>
                <span>Tenant identifier</span>
                <strong>northstar-building</strong>
              </div>
              <div>
                <span>Selected plan</span>
                <strong>{request.plan} · Monthly</strong>
              </div>
              <div>
                <span>Initial subscription</span>
                <strong>Pending activation</strong>
              </div>
              <div>
                <span>Account setup</span>
                <strong>Email invitation</strong>
              </div>
            </div>
            <div className="atomic-note">
              <ShieldCheck />
              <p>
                <strong>Provision as one workflow</strong>
                <br />
                Tenant, owner membership, subscription, invitation, and audit
                event are created atomically. A failure must not leave a partial
                account.
              </p>
            </div>
            <div className="modal-actions">
              <Button tone="secondary" onClick={() => setConfirm(false)}>
                Cancel
              </Button>
              <Button
                onClick={() => {
                  updateRequest(request.ref, "Approved")
                  setConfirm(false)
                  setApproved(true)
                }}
              >
                Confirm approval
              </Button>
            </div>
          </Modal>
        )}
        {decision && (
          <Modal
            title={
              decision === "reject"
                ? `Reject ${request.shop}?`
                : "Request additional information"
            }
            onClose={() => setDecision(undefined)}
          >
            <div className="modal-alert">
              <TriangleAlert />
              <span>
                {decision === "reject"
                  ? "The request will remain in history and the applicant will not receive portal access."
                  : "The application returns to review when the applicant provides the requested details."}
              </span>
            </div>
            <label className="field decision-field">
              <span>
                {decision === "reject"
                  ? "Rejection reason*"
                  : "Message to applicant*"}
              </span>
              <textarea
                value={decisionNote}
                onChange={(event) => setDecisionNote(event.target.value)}
                placeholder={
                  decision === "reject"
                    ? "Explain why the request cannot be approved."
                    : "Describe the missing or unclear information."
                }
              />
            </label>
            {!decisionNote.trim() && (
              <small className="field-error">
                A clear message is required before continuing.
              </small>
            )}
            <div className="modal-actions">
              <Button tone="secondary" onClick={() => setDecision(undefined)}>
                Cancel
              </Button>
              <Button
                tone={decision === "reject" ? "danger" : "primary"}
                disabled={!decisionNote.trim()}
                onClick={completeDecision}
              >
                {decision === "reject"
                  ? "Confirm rejection"
                  : "Send information request"}
              </Button>
            </div>
          </Modal>
        )}
      </>
    )
  return (
    <>
      <PageHead
        eyebrow="Applications"
        title="Registration requests"
        description="Review, approve, or request more information before tenant access is created."
        actions={
          <Button
            tone="secondary"
            onClick={() => {
              downloadCsv("registration-requests.csv", [
                ["Reference", "Shop", "Owner", "Plan", "Date", "Status"],
                ...filteredRequests.map((item) => [
                  item.ref,
                  item.shop,
                  item.owner,
                  item.plan,
                  item.date,
                  item.status,
                ]),
              ])
              notify("Registration request export downloaded.", "success")
            }}
          >
            Export requests
          </Button>
        }
      />
      <div className="request-tabs">
        {["All", "Pending", "More info", "Approved", "Rejected"].map((item) => (
          <button
            key={item}
            className={filter === item ? "active" : ""}
            onClick={() => setFilter(item)}
          >
            {item}{" "}
            <b>
              {item === "All"
                ? requests.length
                : requests.filter((requestItem) => requestItem.status === item)
                    .length}
            </b>
          </button>
        ))}
      </div>
      <div className="panel table-panel">
        <div className="table-tools">
          <div className="table-search">
            <Search />
            <input
              placeholder="Search shop, owner, or reference"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          {(search || filter !== "All") && (
            <button
              className="reset-filter"
              onClick={() => {
                setSearch("")
                setFilter("All")
              }}
            >
              Reset filters
            </button>
          )}
        </div>
        <div className="data-table request-table">
          <div className="tr th">
            {[
              "Request",
              "Shop & owner",
              "Contact",
              "Requested plan",
              "Submitted",
              "Status",
              "",
            ].map((x) => (
              <span key={x}>{x}</span>
            ))}
          </div>
          {filteredRequests.map((r) => (
            <div className="tr" key={r.ref}>
              <span className="mono">{r.ref}</span>
              <span>
                <strong>{r.shop}</strong>
                <small>{r.owner}</small>
              </span>
              <span>
                <strong>
                  {r.owner.toLowerCase().split(" ").join(".")}@example.com
                </strong>
                <small>+1 555 01{82 + requests.indexOf(r)}</small>
              </span>
              <span>
                {r.plan}
                <small>Monthly</small>
              </span>
              <span>{r.date}</span>
              <span>
                <Badge>{r.status}</Badge>
              </span>
              <span>
                <button
                  className="review-button"
                  onClick={() => setSelected(r.ref)}
                >
                  Review <ChevronRight />
                </button>
              </span>
            </div>
          ))}
        </div>
        {!filteredRequests.length && (
          <EmptyState
            icon={<ClipboardCheck />}
            title="No registration requests found"
            description="There are no requests matching this status or search."
            action={
              <Button
                tone="secondary"
                onClick={() => {
                  setFilter("All")
                  setSearch("")
                }}
              >
                Clear filters
              </Button>
            }
          />
        )}
      </div>
    </>
  )
}

function Tenants() {
  const { notify } = usePrototype()
  const [tenants, setTenants] = useState([
    {
      shop: "Westline Roofing Depot",
      id: "westline-roofing",
      owner: "S. Mensah",
      registered: "May 17, 2025",
      status: "Active",
      subscription: "Scale · Active",
    },
    {
      shop: "MetroStone Hardware",
      id: "metrostone",
      owner: "Ibrahim Noor",
      registered: "May 16, 2025",
      status: "Active",
      subscription: "Growth · Trialing",
    },
    {
      shop: "Haven Paint & Tile",
      id: "haven-paint",
      owner: "Elena Su",
      registered: "Apr 28, 2025",
      status: "Suspended",
      subscription: "Growth · Past due",
    },
    {
      shop: "Toma Hardware & Timber",
      id: "toma-hardware",
      owner: "David Toma",
      registered: "Apr 19, 2025",
      status: "Pending",
      subscription: "Starter · Pending",
    },
  ])
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState("All statuses")
  const [selected, setSelected] = useState<typeof tenants[number]>()
  const [confirmSuspend, setConfirmSuspend] = useState(false)
  const visible = tenants.filter(
    (tenant) =>
      `${tenant.shop} ${tenant.owner} ${tenant.id}`
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (status === "All statuses" || tenant.status === status),
  )
  const changeStatus = () => {
    if (!selected) return
    const next = selected.status === "Suspended" ? "Active" : "Suspended"
    setTenants((items) =>
      items.map((item) =>
        item.id === selected.id ? { ...item, status: next } : item,
      ),
    )
    setSelected({ ...selected, status: next })
    setConfirmSuspend(false)
    notify(
      `${selected.shop} is now ${next.toLowerCase()}. Audit history updated.`,
      "success",
    )
  }
  if (selected)
    return (
      <>
        <button
          className="back-link workspace-back"
          onClick={() => setSelected(undefined)}
        >
          ← Back to tenants
        </button>
        <PageHead
          eyebrow={`Tenants / ${selected.id}`}
          title={selected.shop}
          description="Tenant lifecycle, owner membership, subscription, and administrative history."
          actions={
            <>
              <Button
                tone="secondary"
                onClick={() =>
                  notify("Subscription details opened from the tenant record.")
                }
              >
                View subscription
              </Button>
              <Button
                tone={selected.status === "Suspended" ? "primary" : "danger"}
                onClick={() => setConfirmSuspend(true)}
              >
                {selected.status === "Suspended"
                  ? "Reactivate tenant"
                  : "Suspend tenant"}
              </Button>
            </>
          }
        />
        <div className="review-layout">
          <div>
            <section className="panel detail-card">
              <PanelHead title="Tenant overview" />
              <dl>
                <dt>Tenant identifier</dt>
                <dd className="mono">{selected.id}</dd>
                <dt>Status</dt>
                <dd>
                  <Badge>{selected.status}</Badge>
                </dd>
                <dt>Registered</dt>
                <dd>{selected.registered}</dd>
                <dt>Inventory location</dt>
                <dd>One shop location (V1)</dd>
              </dl>
            </section>
            <section className="panel detail-card">
              <PanelHead title="Owner membership" />
              <dl>
                <dt>Owner</dt>
                <dd>{selected.owner}</dd>
                <dt>Membership</dt>
                <dd>Shop owner · Active</dd>
                <dt>Tenant access</dt>
                <dd>Restricted to {selected.shop}</dd>
              </dl>
            </section>
          </div>
          <aside>
            <section className="panel decision-card">
              <Badge>{selected.subscription.split(" · ")[1]}</Badge>
              <h2>{selected.subscription.split(" · ")[0]} plan</h2>
              <div>
                <span>Subscription</span>
                <strong>{selected.subscription}</strong>
              </div>
              <div>
                <span>Access policy</span>
                <strong>
                  {selected.status === "Suspended"
                    ? "Restricted"
                    : "Full inventory access"}
                </strong>
              </div>
            </section>
            <section className="panel history-card">
              <PanelHead title="Recent audit history" />
              <div>
                <span />
                <p>
                  <strong>Tenant reviewed</strong>
                  <small>Today · Ari Lane</small>
                </p>
              </div>
              <div>
                <span />
                <p>
                  <strong>Subscription status checked</strong>
                  <small>Yesterday · System</small>
                </p>
              </div>
            </section>
          </aside>
        </div>
        <div className="logic-note">
          <ShieldCheck />
          <span>
            Platform administrators can change tenant lifecycle and subscription
            metadata, but cannot directly edit shop stock balances.
          </span>
        </div>
        {confirmSuspend && (
          <Modal
            title={`${
              selected.status === "Suspended" ? "Reactivate" : "Suspend"
            } ${selected.shop}?`}
            onClose={() => setConfirmSuspend(false)}
          >
            <div className="modal-alert">
              <TriangleAlert />
              <span>
                {selected.status === "Suspended"
                  ? "Reactivating restores access according to subscription policy."
                  : "Shop users will lose write access immediately. Inventory records remain retained and audit history is preserved."}
              </span>
            </div>
            {selected.status !== "Suspended" && (
              <label className="field decision-field">
                <span>Suspension reason*</span>
                <textarea defaultValue="Subscription or policy review required" />
              </label>
            )}
            <div className="modal-actions">
              <Button tone="secondary" onClick={() => setConfirmSuspend(false)}>
                Cancel
              </Button>
              <Button
                tone={selected.status === "Suspended" ? "primary" : "danger"}
                onClick={changeStatus}
              >
                Confirm{" "}
                {selected.status === "Suspended"
                  ? "reactivation"
                  : "suspension"}
              </Button>
            </div>
          </Modal>
        )}
      </>
    )
  return (
    <>
      <PageHead
        eyebrow="Platform"
        title="Shops / tenants"
        description="Manage tenant lifecycle without accessing or editing shop inventory balances."
        actions={
          <Button
            tone="secondary"
            onClick={() => {
              downloadCsv("stockforge-tenants.csv", [
                [
                  "Shop",
                  "Tenant ID",
                  "Owner",
                  "Registered",
                  "Status",
                  "Subscription",
                ],
                ...visible.map((tenant) => [
                  tenant.shop,
                  tenant.id,
                  tenant.owner,
                  tenant.registered,
                  tenant.status,
                  tenant.subscription,
                ]),
              ])
              notify("Tenant export downloaded.", "success")
            }}
          >
            Export tenants
          </Button>
        }
      />
      <div className="metric-grid compact-metrics">
        <Metric
          icon={<Building2 />}
          label="Active"
          value="72"
          meta="85.7% of tenants"
        />
        <Metric
          icon={<Clock3 />}
          label="Pending"
          value="5"
          meta="Awaiting setup"
        />
        <Metric
          icon={<AlertCircle />}
          label="Suspended"
          value="4"
          meta="Access restricted"
        />
        <Metric
          icon={<LockKeyhole />}
          label="Closed"
          value="3"
          meta="Retained by policy"
        />
      </div>
      <div className="panel table-panel">
        <div className="table-tools">
          <div className="table-search">
            <Search />
            <input
              placeholder="Search shops or owners"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option>All statuses</option>
            <option>Active</option>
            <option>Suspended</option>
            <option>Pending</option>
          </select>
          {(search || status !== "All statuses") && (
            <button
              className="reset-filter"
              onClick={() => {
                setSearch("")
                setStatus("All statuses")
              }}
            >
              Reset filters
            </button>
          )}
        </div>
        <div className="data-table tenant-table">
          <div className="tr th">
            {[
              "Shop",
              "Tenant ID",
              "Owner",
              "Registered",
              "Tenant status",
              "Subscription",
              "",
            ].map((x) => (
              <span key={x}>{x}</span>
            ))}
          </div>
          {visible.map((tenant) => (
            <div className="tr" key={tenant.id}>
              <span>
                <strong>{tenant.shop}</strong>
              </span>
              <span className="mono">{tenant.id}</span>
              <span>{tenant.owner}</span>
              <span>{tenant.registered}</span>
              <span>
                <Badge>{tenant.status}</Badge>
              </span>
              <span>{tenant.subscription}</span>
              <span>
                <button
                  className="review-button"
                  onClick={() => setSelected(tenant)}
                >
                  View <ChevronRight />
                </button>
              </span>
            </div>
          ))}
        </div>
        {!visible.length && (
          <EmptyState
            icon={<Building2 />}
            title="No tenants found"
            description="Try another search or tenant status."
            action={
              <Button
                tone="secondary"
                onClick={() => {
                  setSearch("")
                  setStatus("All statuses")
                }}
              >
                Clear filters
              </Button>
            }
          />
        )}
      </div>
      <div className="logic-note">
        <ShieldCheck />
        <span>
          <strong>Tenant isolation:</strong> Platform administrators manage
          lifecycle and subscription metadata here. They cannot directly change
          a tenant’s stock balances.
        </span>
      </div>
    </>
  )
}

function Plans() {
  const { plans, savePlan, notify } = usePrototype()
  const [editing, setEditing] = useState<Plan | "new">()
  const [name, setName] = useState("")
  const [code, setCode] = useState("")
  const [price, setPrice] = useState("0")
  const [interval, setInterval] = useState<Plan["interval"]>("Monthly")
  const [productLimit, setProductLimit] = useState("250")
  const [userLimit, setUserLimit] = useState("3")
  const [description, setDescription] = useState("")
  const [isPublic, setIsPublic] = useState(false)
  const [error, setError] = useState("")
  const openPlan = (plan?: Plan) => {
    setEditing(plan || "new")
    setName(plan?.name || "")
    setCode(plan?.code || "")
    setPrice(plan?.price === null ? "" : String(plan?.price || ""))
    setInterval(plan?.interval || "Monthly")
    setProductLimit(
      plan?.productLimit === null ? "" : String(plan?.productLimit || 250),
    )
    setUserLimit(plan?.userLimit === null ? "" : String(plan?.userLimit || 3))
    setDescription(plan?.description || "")
    setIsPublic(plan?.public || false)
    setError("")
  }
  const submit = () => {
    if (!name.trim() || !code.trim() || !description.trim()) {
      setError("Plan name, unique code, and description are required.")
      return
    }
    const current = editing === "new" ? undefined : editing
    if (
      plans.some((plan) => plan.code === code && plan.code !== current?.code)
    ) {
      setError("This plan code is already in use.")
      return
    }
    const plan: Plan = {
      code: code.toUpperCase(),
      name,
      price: price ? Number(price) : null,
      interval,
      description,
      productLimit: productLimit ? Number(productLimit) : null,
      userLimit: userLimit ? Number(userLimit) : null,
      public: isPublic,
      active: current?.active ?? true,
      featured: current?.featured,
      features: current?.features || [
        "Core inventory",
        "Units & conversions",
        "Inventory reports",
      ],
    }
    savePlan(plan, current?.code)
    setEditing(undefined)
    notify(
      current
        ? "Plan changes saved. Existing subscription snapshots were not changed."
        : "Subscription plan created.",
      "success",
    )
  }
  return (
    <>
      <PageHead
        eyebrow="Subscriptions"
        title="Subscription plans"
        description="Published plans appear on the public pricing page. Existing subscriptions retain their plan snapshot."
        actions={
          <Button icon={<Plus />} onClick={() => openPlan()}>
            Create plan
          </Button>
        }
      />
      <div className="admin-plan-grid">
        {plans.map((plan, i) => (
          <article className="admin-plan" key={plan.code}>
            {plan.featured && <Badge tone="recommended">Recommended</Badge>}
            <div className="plan-status">
              <Badge>{plan.public ? "Public" : "Hidden"}</Badge>
              <Badge>{plan.active ? "Active" : "Inactive"}</Badge>
            </div>
            <h2>{plan.name}</h2>
            <span className="mono">{plan.code}</span>
            <div className="admin-price">
              <strong>
                {plan.price === null ? "Custom" : `$${plan.price}`}
              </strong>
              {plan.price !== null && (
                <span>/ {plan.interval.toLowerCase()}</span>
              )}
            </div>
            <dl>
              <dt>Product limit</dt>
              <dd>{plan.productLimit?.toLocaleString() || "Custom"}</dd>
              <dt>Staff limit</dt>
              <dd>{plan.userLimit || "Custom"}</dd>
              <dt>Display order</dt>
              <dd>{i + 1}</dd>
            </dl>
            <div className="card-actions">
              <Button tone="secondary" onClick={() => openPlan(plan)}>
                Edit plan
              </Button>
              <button
                onClick={() => {
                  savePlan({ ...plan, public: !plan.public }, plan.code)
                  notify(
                    `${plan.name} is now ${
                      plan.public ? "hidden from" : "visible on"
                    } public pricing.`,
                    "success",
                  )
                }}
              >
                {plan.public ? "Hide" : "Publish"}
              </button>
            </div>
          </article>
        ))}
      </div>
      {editing && (
        <Modal
          title={
            editing === "new"
              ? "Create subscription plan"
              : `Edit ${editing.name}`
          }
          onClose={() => setEditing(undefined)}
        >
          {error && (
            <div className="form-error inline-error">
              <AlertCircle />
              <span>{error}</span>
            </div>
          )}
          <div className="field-grid modal-fields">
            <label className="field">
              <span>Plan name*</span>
              <input
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="e.g. Growth"
              />
            </label>
            <label className="field">
              <span>Unique code*</span>
              <input
                value={code}
                onChange={(event) => setCode(event.target.value)}
                placeholder="GROWTH-M"
              />
            </label>
            <label className="field wide">
              <span>Description*</span>
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Who is this plan for?"
              />
            </label>
            <label className="field">
              <span>Price</span>
              <input
                type="number"
                min="0"
                value={price}
                onChange={(event) => setPrice(event.target.value)}
                placeholder="Leave blank for custom"
              />
            </label>
            <label className="field">
              <span>Billing interval*</span>
              <select
                value={interval}
                onChange={(event) =>
                  setInterval(event.target.value as Plan["interval"])
                }
              >
                <option>Monthly</option>
                <option>Yearly</option>
              </select>
            </label>
            <label className="field">
              <span>Product limit</span>
              <input
                type="number"
                min="1"
                value={productLimit}
                onChange={(event) => setProductLimit(event.target.value)}
              />
            </label>
            <label className="field">
              <span>Staff limit</span>
              <input
                type="number"
                min="1"
                value={userLimit}
                onChange={(event) => setUserLimit(event.target.value)}
              />
            </label>
            <label className="check-row wide">
              <input
                type="checkbox"
                checked={isPublic}
                onChange={(event) => setIsPublic(event.target.checked)}
              />
              <span>
                <strong>Show on public pricing page</strong>
                <small>
                  Hidden plans remain available for historical subscriptions.
                </small>
              </span>
            </label>
          </div>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setEditing(undefined)}>
              Cancel
            </Button>
            <Button onClick={submit}>
              {editing === "new" ? "Create plan" : "Save plan changes"}
            </Button>
          </div>
        </Modal>
      )}
      <div className="plan-policy">
        <AlertCircle />
        <div>
          <strong>Price snapshot policy</strong>
          <p>
            Changing a plan does not silently change the price or limits stored
            on an existing subscription. Confirm migrations separately.
          </p>
        </div>
      </div>
    </>
  )
}

function Subscriptions() {
  const { plans, notify } = usePrototype()
  const [records, setRecords] = useState([
    {
      shop: "Westline Roofing Depot",
      plan: "Scale",
      status: "Active",
      start: "May 18, 2025",
      period: "May 18 – Jun 17",
      expiry: "Jun 17, 2025",
    },
    {
      shop: "MetroStone Hardware",
      plan: "Growth",
      status: "Trialing",
      start: "May 16, 2025",
      period: "14-day trial",
      expiry: "May 30, 2025",
    },
    {
      shop: "Haven Paint & Tile",
      plan: "Growth",
      status: "Past due",
      start: "Apr 1, 2025",
      period: "May 1 – May 31",
      expiry: "May 31, 2025",
    },
    {
      shop: "Toma Hardware & Timber",
      plan: "Starter",
      status: "Pending",
      start: "—",
      period: "Not started",
      expiry: "—",
    },
  ])
  const [filter, setFilter] = useState("All")
  const [selected, setSelected] = useState<typeof records[number]>()
  const [planName, setPlanName] = useState("Growth")
  const [period, setPeriod] = useState("1 month")
  const visible = records.filter(
    (record) =>
      filter === "All" ||
      record.status === filter ||
      (filter === "Expiring soon" && record.expiry !== "—"),
  )
  const openSubscription = (record?: typeof records[number]) => {
    const target =
      record || records.find((item) => item.status === "Pending") || records[0]
    setSelected(target)
    setPlanName(target.plan)
  }
  const updateStatus = (status: string) => {
    if (!selected) return
    setRecords((items) =>
      items.map((item) =>
        item.shop === selected.shop
          ? {
              ...item,
              plan: planName,
              status,
              start: item.start === "—" ? "May 21, 2025" : item.start,
              period: period,
              expiry: "Jun 21, 2025",
            }
          : item,
      ),
    )
    setSelected(undefined)
    notify(
      `Subscription ${status.toLowerCase()} for ${selected.shop}. Audit history updated.`,
      "success",
    )
  }
  return (
    <>
      <PageHead
        eyebrow="Platform"
        title="Subscriptions"
        description="Activate and manage SaaS access without requiring a payment transaction."
        actions={
          <Button onClick={() => openSubscription()}>
            Activate subscription
          </Button>
        }
      />
      <div className="request-tabs">
        {["All", "Active", "Trialing", "Pending", "Expiring soon"].map(
          (item) => (
            <button
              className={filter === item ? "active" : ""}
              onClick={() => setFilter(item)}
              key={item}
            >
              {item}{" "}
              <b>
                {item === "All"
                  ? records.length
                  : records.filter((record) => record.status === item).length}
              </b>
            </button>
          ),
        )}
      </div>
      <div className="panel table-panel">
        <div className="data-table subscription-table">
          <div className="tr th">
            {[
              "Shop",
              "Plan",
              "Status",
              "Start date",
              "Current period",
              "Expires / renews",
              "",
            ].map((x) => (
              <span key={x}>{x}</span>
            ))}
          </div>
          {visible.map((record) => (
            <div className="tr" key={record.shop}>
              <span>
                <strong>{record.shop}</strong>
              </span>
              <span>{record.plan}</span>
              <span>
                <Badge>{record.status}</Badge>
              </span>
              <span>{record.start}</span>
              <span>{record.period}</span>
              <span>{record.expiry}</span>
              <span>
                <button
                  className="review-button"
                  onClick={() => openSubscription(record)}
                >
                  Manage <ChevronRight />
                </button>
              </span>
            </div>
          ))}
        </div>
      </div>
      {selected && (
        <Modal
          title={`Manage ${selected.shop}`}
          onClose={() => setSelected(undefined)}
        >
          <div className="field-grid modal-fields">
            <label className="field wide">
              <span>Subscription plan*</span>
              <select
                value={planName}
                onChange={(event) => setPlanName(event.target.value)}
              >
                {plans
                  .filter((plan) => plan.active)
                  .map((plan) => (
                    <option key={plan.code}>{plan.name}</option>
                  ))}
              </select>
            </label>
            <label className="field">
              <span>Start date</span>
              <input
                value={selected.start === "—" ? "2025-05-21" : "2025-05-21"}
                type="date"
                readOnly
              />
            </label>
            <label className="field">
              <span>Subscription period*</span>
              <select
                value={period}
                onChange={(event) => setPeriod(event.target.value)}
              >
                <option>1 month</option>
                <option>3 months</option>
                <option>1 year</option>
              </select>
            </label>
            <label className="field wide">
              <span>Internal note</span>
              <textarea placeholder="Reason for activation, extension, or status change" />
            </label>
          </div>
          <div className="atomic-note">
            <AlertCircle />
            <p>
              No payment transaction is required. The selected plan’s current
              price and limits are stored as a subscription snapshot.
            </p>
          </div>
          <div className="modal-actions subscription-actions">
            {selected.status === "Active" && (
              <Button tone="danger" onClick={() => updateStatus("Suspended")}>
                Suspend subscription
              </Button>
            )}
            <Button tone="secondary" onClick={() => setSelected(undefined)}>
              Cancel
            </Button>
            <Button onClick={() => updateStatus("Active")}>
              {selected.status === "Active"
                ? "Extend subscription"
                : "Activate subscription"}
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function PlatformUsers() {
  const { notify } = usePrototype()
  const [admins, setAdmins] = useState([
    {
      name: "Ari Lane",
      email: "ari@stockforge.example",
      role: "Super administrator",
      status: "Active",
      activity: "Now",
    },
    {
      name: "Jo Kim",
      email: "jo@stockforge.example",
      role: "Subscription admin",
      status: "Active",
      activity: "3 hours ago",
    },
    {
      name: "Nia Brooks",
      email: "nia@stockforge.example",
      role: "Support reviewer",
      status: "Active",
      activity: "Yesterday",
    },
  ])
  const [editing, setEditing] = useState<typeof admins[number] | "new">()
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [role, setRole] = useState("Support reviewer")
  const open = (admin?: typeof admins[number]) => {
    setEditing(admin || "new")
    setName(admin?.name || "")
    setEmail(admin?.email || "")
    setRole(admin?.role || "Support reviewer")
  }
  const save = () => {
    if (!name.trim() || !email.includes("@")) return
    const current = editing === "new" ? undefined : editing
    setAdmins((items) =>
      current
        ? items.map((item) =>
            item.email === current.email
              ? { ...item, name, email, role }
              : item,
          )
        : [
            ...items,
            {
              name,
              email,
              role,
              status: "Active",
              activity: "Invitation pending",
            },
          ],
    )
    setEditing(undefined)
    notify(
      current
        ? "Platform administrator updated."
        : "Platform administrator invitation added.",
      "success",
    )
  }
  return (
    <>
      <PageHead
        eyebrow="Security"
        title="Platform users"
        description="Platform administrator access is separate from every tenant membership."
        actions={
          <Button icon={<Plus />} onClick={() => open()}>
            Add administrator
          </Button>
        }
      />
      <div className="panel table-panel">
        <div className="data-table users-table">
          <div className="tr th">
            <span>Administrator</span>
            <span>Platform role</span>
            <span>Status</span>
            <span>Recent activity</span>
            <span />
          </div>
          {admins.map((admin) => (
            <div className="tr" key={admin.email}>
              <span className="user-cell">
                <i>
                  {admin.name
                    .split(" ")
                    .map((x) => x[0])
                    .join("")}
                </i>
                <span>
                  <strong>{admin.name}</strong>
                  <small>{admin.email}</small>
                </span>
              </span>
              <span>{admin.role}</span>
              <span>
                <Badge>{admin.status}</Badge>
              </span>
              <span>{admin.activity}</span>
              <span className="row-actions">
                <button onClick={() => open(admin)}>Edit</button>
                {admin.role !== "Super administrator" && (
                  <button
                    onClick={() => {
                      setAdmins((items) =>
                        items.map((item) =>
                          item.email === admin.email
                            ? {
                                ...item,
                                status:
                                  item.status === "Disabled"
                                    ? "Active"
                                    : "Disabled",
                              }
                            : item,
                        ),
                      )
                      notify("Administrator access updated.", "success")
                    }}
                  >
                    {admin.status === "Disabled" ? "Enable" : "Disable"}
                  </button>
                )}
              </span>
            </div>
          ))}
        </div>
      </div>
      {editing && (
        <Modal
          title={
            editing === "new"
              ? "Add platform administrator"
              : `Edit ${editing.name}`
          }
          onClose={() => setEditing(undefined)}
        >
          <div className="field-grid modal-fields">
            <label className="field wide">
              <span>Full name*</span>
              <input
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>
            <label className="field wide">
              <span>Email address*</span>
              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
            <label className="field wide">
              <span>Platform role*</span>
              <select
                value={role}
                onChange={(event) => setRole(event.target.value)}
              >
                <option>Support reviewer</option>
                <option>Subscription admin</option>
                <option>Super administrator</option>
              </select>
              <small>
                Use the least-privileged role needed. Platform roles do not
                create shop memberships.
              </small>
            </label>
          </div>
          <div className="modal-actions">
            <Button tone="secondary" onClick={() => setEditing(undefined)}>
              Cancel
            </Button>
            <Button
              disabled={!name.trim() || !email.includes("@")}
              onClick={save}
            >
              Save administrator
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function AuditLogs() {
  const { notify } = usePrototype()
  const records = [
    [
      "May 21, 10:44",
      "Ari Lane",
      "Subscription activated",
      "SUB-0084",
      "Westline Roofing",
      "Success",
    ],
    [
      "May 21, 09:58",
      "Ari Lane",
      "Registration approved",
      "REQ-2025-0181",
      "Westline Roofing",
      "Success",
    ],
    [
      "May 20, 16:12",
      "Jo Kim",
      "Plan published",
      "Growth Monthly",
      "—",
      "Success",
    ],
    [
      "May 20, 14:08",
      "Nia Brooks",
      "Tenant suspended",
      "Haven Paint & Tile",
      "Haven Paint",
      "Success",
    ],
  ]
  const [search, setSearch] = useState("")
  const [selected, setSelected] = useState<string[]>()
  const visible = records.filter((record) =>
    record.join(" ").toLowerCase().includes(search.toLowerCase()),
  )
  return (
    <>
      <PageHead
        eyebrow="Security"
        title="Audit logs"
        description="Read-only platform history for sensitive administrative actions."
        actions={
          <Button
            tone="secondary"
            onClick={() => {
              downloadCsv("stockforge-audit-log.csv", [
                ["Timestamp", "Actor", "Action", "Entity", "Tenant", "Result"],
                ...visible,
              ])
              notify("Audit log export downloaded.", "success")
            }}
          >
            Export audit log
          </Button>
        }
      />
      <div className="panel table-panel">
        <div className="table-tools">
          <div className="table-search">
            <Search />
            <input
              placeholder="Search action, tenant, or actor"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          {search && (
            <button className="reset-filter" onClick={() => setSearch("")}>
              Reset search
            </button>
          )}
        </div>
        <div className="data-table audit-table">
          <div className="tr th">
            {["Timestamp", "Actor", "Action", "Entity", "Tenant", "Result"].map(
              (x) => (
                <span key={x}>{x}</span>
              ),
            )}
          </div>
          {visible.map((r) => (
            <button
              className="tr ledger-row"
              key={r[0] + r[2]}
              onClick={() => setSelected(r)}
            >
              {r.map((c, i) => (
                <span key={c} className={i === 3 ? "mono" : ""}>
                  {i === 5 ? <Badge tone="active">{c}</Badge> : c}
                </span>
              ))}
            </button>
          ))}
        </div>
        {!visible.length && (
          <EmptyState
            icon={<Search />}
            title="No audit events found"
            description="Try a different actor, tenant, or action."
            action={
              <Button tone="secondary" onClick={() => setSearch("")}>
                Clear search
              </Button>
            }
          />
        )}
      </div>
      {selected && (
        <Modal
          title="Audit event details"
          onClose={() => setSelected(undefined)}
        >
          <div className="approval-grid">
            {["Timestamp", "Actor", "Action", "Entity", "Tenant", "Result"].map(
              (label, index) => (
                <div key={label}>
                  <span>{label}</span>
                  <strong>{selected[index]}</strong>
                </div>
              ),
            )}
          </div>
          <div className="atomic-note">
            <LockKeyhole />
            <p>
              Audit records are read-only from the normal platform interface.
            </p>
          </div>
          <div className="modal-actions">
            <Button onClick={() => setSelected(undefined)}>Close</Button>
          </div>
        </Modal>
      )}
    </>
  )
}

function AdminSettings() {
  const { notify } = usePrototype()
  return (
    <>
      <PageHead
        eyebrow="Platform"
        title="Platform settings"
        description="Configure registration, defaults, support details, and subscription policy."
        actions={
          <Button onClick={() => notify("Platform settings saved.", "success")}>
            Save changes
          </Button>
        }
      />
      <div className="settings-grid">
        <section className="panel form-panel">
          <PanelHead title="Public website" />
          <div className="field-grid">
            <Field label="Product name" value="StockForge" />
            <Field label="Support email" value="support@stockforge.example" />
            <Field label="Contact phone" value="+1 555 0100" />
            <Field
              label="Public registration"
              value="Enabled — approval required"
              select
            />
          </div>
        </section>
        <section className="panel form-panel">
          <PanelHead title="Regional defaults" />
          <div className="field-grid">
            <Field label="Default currency" value="USD — US Dollar" select />
            <Field
              label="Default timezone"
              value="UTC−05:00 Eastern Time"
              select
            />
          </div>
        </section>
        <section className="panel form-panel">
          <PanelHead title="Subscription policy" />
          <div className="field-grid">
            <Field label="Grace period" value="7 days" select />
            <Field label="Default trial duration" value="14 days" select />
            <label className="check-row wide">
              <input type="checkbox" defaultChecked />
              <span>
                <strong>Allow read-only access during grace period</strong>
                <small>
                  Posting and configuration changes remain disabled.
                </small>
              </span>
            </label>
          </div>
        </section>
      </div>
    </>
  )
}

function PrototypeApp() {
  const [experience, setExperience] = useState<Experience>("public")
  const content = useMemo(() => {
    if (experience === "shop") return <ShopPortal />
    if (experience === "admin") return <AdminPortal />
    return (
      <PublicSite
        onEnterShop={() => setExperience("shop")}
        onEnterAdmin={() => setExperience("admin")}
      />
    )
  }, [experience])
  return (
    <>
      <ExperienceSwitch experience={experience} setExperience={setExperience} />
      {content}
    </>
  )
}

export default function App() {
  const [products, setProducts] = useState(initialProducts)
  const [requests, setRequests] = useState(initialRequests)
  const [plans, setPlans] = useState(initialPlans)
  const [users, setUsers] = useState(initialUsers)
  const [movements, setMovements] = useState(initialMovements)
  const [toast, setToast] = useState<{ message: string tone: string }>()

  const notify = (
    message: string,
    tone: "success" | "error" | "info" = "info",
  ) => {
    setToast({ message, tone })
    window.setTimeout(() => setToast(undefined), 3200)
  }
  const saveProduct = (product: Product, previousSku?: string) =>
    setProducts((items) => {
      const match = previousSku || product.sku
      return items.some((item) => item.sku === match)
        ? items.map((item) => (item.sku === match ? product : item))
        : [product, ...items]
    })
  const removeProduct = (sku: string) =>
    setProducts((items) =>
      items
        .map((item) =>
          item.sku === sku && item.active ? { ...item, active: false } : item,
        )
        .filter((item) => item.sku !== sku || item.active),
    )
  const updateStock = (sku: string, quantity: number, incomingCost?: number) =>
    setProducts((items) =>
      items.map((item) => {
        if (item.sku !== sku) return item
        const added = Math.max(0, quantity - item.qty)
        const cost =
          incomingCost !== undefined && added > 0
            ? (item.qty * item.cost + added * incomingCost) /
              Math.max(quantity, 1)
            : item.cost
        return {
          ...item,
          qty: quantity,
          cost,
          status: getStockStatus(quantity, item.reorder),
        }
      }),
    )
  const savePlan = (plan: Plan, previousCode?: string) =>
    setPlans((items) => {
      const match = previousCode || plan.code
      return items.some((item) => item.code === match)
        ? items.map((item) => (item.code === match ? plan : item))
        : [...items, plan]
    })
  const saveUser = (user: ShopUser) =>
    setUsers((items) =>
      items.some((item) => item.id === user.id)
        ? items.map((item) => (item.id === user.id ? user : item))
        : [...items, user],
    )
  const value: PrototypeContextValue = {
    products,
    saveProduct,
    removeProduct,
    updateStock,
    requests,
    updateRequest: (ref, status) =>
      setRequests((items) =>
        items.map((item) => (item.ref === ref ? { ...item, status } : item)),
      ),
    plans,
    savePlan,
    users,
    saveUser,
    movements,
    addMovement: (movement) => setMovements((items) => [movement, ...items]),
    notify,
  }
  return (
    <PrototypeContext.Provider value={value}>
      <PrototypeApp />
      {toast && (
        <div className={`prototype-toast ${toast.tone}`}>
          {toast.tone === "success" ? <CheckCircle2 /> : <AlertCircle />}
          <span>{toast.message}</span>
          <button
            onClick={() => setToast(undefined)}
            aria-label="Dismiss notification"
          >
            <X />
          </button>
        </div>
      )}
    </PrototypeContext.Provider>
  )
}
