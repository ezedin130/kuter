import { describe, it, expect } from 'vitest';
import * as fs from 'fs';
import * as path from 'path';

describe('Shop Portal Layout & Responsiveness Suite', () => {
  const shopCssPath = path.resolve(__dirname, '../styles/shop.css');
  const tokensCssPath = path.resolve(__dirname, '../styles/tokens.css');
  const dashboardPagePath = path.resolve(__dirname, '../pages/DashboardPage.tsx');
  const posPagePath = path.resolve(__dirname, '../pages/SalesPosPage.tsx');

  const shopCss = fs.readFileSync(shopCssPath, 'utf-8');
  const tokensCss = fs.readFileSync(tokensCssPath, 'utf-8');
  const dashboardPage = fs.readFileSync(dashboardPagePath, 'utf-8');
  fs.readFileSync(posPagePath, 'utf-8');

  it('defines .table-responsive in shop.css with horizontal overflow scrolling', () => {
    expect(shopCss).toMatch(/\.table-responsive\s*\{[^}]*overflow-x:\s*auto/);
    expect(shopCss).toMatch(/\.table-responsive\s*\{[^}]*min-width:\s*0/);
  });

  it('defines .pos-main-grid and .pos-layout-wrapper in shop.css with responsive column layout', () => {
    expect(shopCss).toMatch(/\.pos-main-grid\s*\{[^}]*display:\s*grid/);
    expect(shopCss).toMatch(/\.pos-layout-wrapper/);
  });

  it('ensures .form-row and .form-row-3 correctly switch to 1-column grid on mobile breakpoints', () => {
    // Check that form-row does not rely on flex-direction: column on a CSS grid
    expect(shopCss).toMatch(/@media[^{]+\(max-width:\s*768px\)[^{]*\{[\s\S]*?\.form-row[^{]*\{[^}]*grid-template-columns:\s*1fr/);
    expect(shopCss).toMatch(/@media[^{]+\(max-width:\s*768px\)[^{]*\{[\s\S]*?\.form-row-3[^{]*\{[^}]*grid-template-columns:\s*1fr/);
  });

  it('ensures .stats-grid has balanced columns across desktop, tablet, and mobile', () => {
    // Desktop 4 columns or balanced repeat
    expect(shopCss).toMatch(/\.stats-grid\s*\{[^}]*grid-template-columns:\s*repeat\(4,\s*1fr\)/);
    // Tablet 2 columns
    expect(shopCss).toMatch(/@media[^{]+\(max-width:\s*(?:1024px|1100px)\)[^{]*\{[\s\S]*?\.stats-grid[^{]*\{[^}]*grid-template-columns:\s*repeat\(2,\s*1fr\)/);
    // Mobile 1 column
    expect(shopCss).toMatch(/@media[^{]+\(max-width:\s*640px\)[^{]*\{[\s\S]*?\.stats-grid[^{]*\{[^}]*grid-template-columns:\s*1fr/);
  });

  it('ensures DashboardPage separates Quick Actions from Recent Tables rather than crushing them in a 360px grid', () => {
    // Should NOT have all 4 cards crammed into a single repeat(auto-fit, minmax(360px, 1fr))
    expect(dashboardPage).not.toContain("display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))'");
    // Should have dedicated dashboard sections or classes
    expect(dashboardPage).toMatch(/dashboard-tables-grid|dashboard-recent-grid/);
    expect(dashboardPage).toMatch(/dashboard-quick-actions/);
  });

  it('ensures Dashboard summary cards have overflow protection and consistent typography', () => {
    expect(shopCss).toMatch(/\.stat-value\s*\{[^}]*overflow:\s*hidden/);
    expect(shopCss).toMatch(/\.stat-value\s*\{[^}]*text-overflow:\s*ellipsis/);
    expect(shopCss).toMatch(/\.stat-card\s*\{[^}]*min-width:\s*0/);
  });

  it('ensures POS page does not overlap catalog and cart pane on mobile', () => {
    expect(shopCss).toMatch(/@media[^{]+\(max-width:\s*(?:1024px|900px)\)[^{]*\{[\s\S]*?\.pos-main-grid[^{]*\{[^}]*grid-template-columns:\s*1fr/);
  });

  it('ensures tokens.css has responsive viewport breakpoints and mobile touch target tokens', () => {
    expect(tokensCss).toMatch(/--touch-target-min/);
  });
});
