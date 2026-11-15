import { useEffect, useState } from 'react';
import { Header, PublicPage } from './components/Header';
import { Footer } from './components/Footer';
import { HomePage } from './pages/HomePage';
import { FeaturesPage } from './pages/FeaturesPage';
import { PricingPage } from './pages/PricingPage';
import { RegistrationPage } from './pages/RegistrationPage';
import { StatusPage } from './pages/StatusPage';
import { AboutPage } from './pages/AboutPage';
import { ContactPage } from './pages/ContactPage';
import './styles/public.css';

const VALID_PAGES: PublicPage[] = ['home', 'features', 'pricing', 'register', 'status', 'about', 'contact'];

function parseCurrentPage(): PublicPage {
  if (typeof window === 'undefined') return 'home';
  // Check hash first (e.g. #register or #/register)
  const rawHash = window.location.hash.replace(/^#[/]?/, '').split('?')[0].toLowerCase() as PublicPage;
  if (VALID_PAGES.includes(rawHash)) return rawHash;

  // Then check pathname (e.g. /register or /pricing)
  const rawPath = window.location.pathname.replace(/^\//, '').split('/')[0].split('?')[0].toLowerCase() as PublicPage;
  if (VALID_PAGES.includes(rawPath)) return rawPath;

  return 'home';
}

export function App() {
  const [currentPage, setCurrentPage] = useState<PublicPage>(parseCurrentPage);
  const [selectedPlan, setSelectedPlan] = useState<string>('growth');

  const navigate = (page: PublicPage, planCode?: string) => {
    if (planCode) {
      setSelectedPlan(planCode);
    }
    setCurrentPage(page);
    window.location.hash = page === 'home' ? '' : page;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPage(parseCurrentPage());
    };

    window.addEventListener('hashchange', handleLocationChange);
    window.addEventListener('popstate', handleLocationChange);
    return () => {
      window.removeEventListener('hashchange', handleLocationChange);
      window.removeEventListener('popstate', handleLocationChange);
    };
  }, []);

  // For Registration and Status pages, render their dedicated auth layout
  if (currentPage === 'register') {
    return (
      <div className="public-site">
        <RegistrationPage initialPlan={selectedPlan} onNavigate={navigate} />
      </div>
    );
  }

  if (currentPage === 'status') {
    return (
      <div className="public-site">
        <StatusPage onNavigate={navigate} />
      </div>
    );
  }

  return (
    <div className="public-site">
      <Header currentPage={currentPage} onNavigate={navigate} />

      {currentPage === 'home' && <HomePage onNavigate={navigate} />}
      {currentPage === 'features' && <FeaturesPage onNavigate={navigate} />}
      {currentPage === 'pricing' && <PricingPage onNavigate={navigate} />}
      {currentPage === 'about' && <AboutPage onNavigate={navigate} />}
      {currentPage === 'contact' && <ContactPage onNavigate={navigate} />}

      <Footer onNavigate={navigate} />
    </div>
  );
}

export default App;
