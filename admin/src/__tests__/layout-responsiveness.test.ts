import { describe, it, expect } from 'vitest';
import * as fs from 'fs';
import * as path from 'path';

describe('Admin Portal Layout & Responsiveness Suite', () => {
  const adminCssPath = path.resolve(__dirname, '../styles/admin.css');
  const tokensCssPath = path.resolve(__dirname, '../styles/tokens.css');
  const headerPath = path.resolve(__dirname, '../components/Header.tsx');
  const sidebarPath = path.resolve(__dirname, '../components/Sidebar.tsx');
  const appPath = path.resolve(__dirname, '../App.tsx');

  const adminCss = fs.readFileSync(adminCssPath, 'utf-8');
  const tokensCss = fs.readFileSync(tokensCssPath, 'utf-8');
  const header = fs.readFileSync(headerPath, 'utf-8');
  const sidebar = fs.readFileSync(sidebarPath, 'utf-8');
  const app = fs.readFileSync(appPath, 'utf-8');

  it('ensures tokens.css has responsive viewport breakpoints and mobile touch target tokens', () => {
    expect(tokensCss).toMatch(/--touch-target-min/);
  });

  it('ensures Header has a mobile navigation toggle button', () => {
    expect(header).toMatch(/mobile-menu-btn|menu-toggle|onToggleSidebar/);
  });

  it('ensures Sidebar supports mobile open state and backdrop', () => {
    expect(sidebar).toMatch(/isOpen/);
    expect(sidebar).toMatch(/sidebar-backdrop/);
  });

  it('ensures App.tsx manages mobile navigation toggle state', () => {
    expect(app).toMatch(/isMobileSidebarOpen|mobileNavOpen/);
  });

  it('ensures admin.css defines off-canvas mobile drawer instead of stacking sidebar on top of screen', () => {
    // Should NOT have .admin-layout { flex-direction: column } with stacked sidebar
    expect(adminCss).not.toMatch(/@media[^{]+\(max-width:\s*768px\)[^{]*\{[\s\S]*?\.admin-layout[^{]*\{[^}]*flex-direction:\s*column/);
    // Should have mobile off-canvas drawer with transition and backdrop
    expect(adminCss).toMatch(/@media[^{]+\(max-width:\s*768px\)[^{]*\{[\s\S]*?\.admin-sidebar[^{]*\{[^}]*position:\s*fixed/);
    expect(adminCss).toMatch(/\.sidebar-backdrop/);
  });

  it('ensures DataTable responsive wrapper has overflow protection', () => {
    expect(adminCss).toMatch(/\.table-responsive-wrapper\s*\{[^}]*overflow-x:\s*auto/);
    expect(adminCss).toMatch(/\.table-responsive-wrapper\s*\{[^}]*min-width:\s*0/);
  });

  it('ensures Overview KPI grid adapts responsively to desktop, tablet, and mobile', () => {
    expect(adminCss).toMatch(/@media[^{]+\(max-width:\s*(?:1024px|1100px)\)[^{]*\{[\s\S]*?\.overview-kpi-grid[^{]*\{[^}]*grid-template-columns:\s*repeat\(2,\s*1fr\)/);
    expect(adminCss).toMatch(/@media[^{]+\(max-width:\s*640px\)[^{]*\{[\s\S]*?\.overview-kpi-grid[^{]*\{[^}]*grid-template-columns:\s*1fr/);
  });
});
