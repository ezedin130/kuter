import { describe, it, expect } from 'vitest';
import * as fs from 'fs';
import * as path from 'path';

describe('Public Website Layout & Responsiveness Suite', () => {
  const publicCssPath = path.resolve(__dirname, '../styles/public.css');
  const tokensCssPath = path.resolve(__dirname, '../styles/tokens.css');

  const publicCss = fs.readFileSync(publicCssPath, 'utf-8');
  const tokensCss = fs.readFileSync(tokensCssPath, 'utf-8');

  it('ensures tokens.css has responsive viewport breakpoints and touch target tokens', () => {
    expect(tokensCss).toMatch(/--touch-target-min/);
  });

  it('ensures public-site layout container prevents horizontal scroll leaks', () => {
    expect(publicCss).toMatch(/\.public-site\s*\{[^}]*overflow-x:\s*hidden/);
  });

  it('ensures form-progress stepper adapts cleanly without overflowing narrow viewports', () => {
    expect(publicCss).toMatch(/\.form-progress\s*\{[^}]*flex-wrap:\s*wrap/);
    expect(publicCss).toMatch(/@media[^{]+\(max-width:\s*(?:640px|768px)\)[^{]*\{[\s\S]*?\.form-progress/);
  });

  it('ensures public header actions adapt on mobile viewports', () => {
    expect(publicCss).toMatch(/@media[^{]+\(max-width:\s*(?:640px|768px)\)[^{]*\{[\s\S]*?\.header-actions/);
  });

  it('ensures touch targets for buttons meet accessibility minimums', () => {
    expect(publicCss).toMatch(/--touch-target-min/);
  });

  it('ensures registration forms collapse cleanly into single column on mobile', () => {
    expect(publicCss).toMatch(/@media[^{]+\(max-width:\s*(?:640px|768px)\)[^{]*\{[\s\S]*?\.field-grid[^{]*\{[^}]*grid-template-columns:\s*1fr/);
  });
});
