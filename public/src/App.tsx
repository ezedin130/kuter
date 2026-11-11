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

export function App() {
  const [currentPage, setCurrentPage] = useState<PublicPage>(() => {
    const hash = window.location.hash.replace('#', '') as PublicPage;
    const validPages: PublicPage[] = ['home', 'features', 'pricing', 'register', 'status', 'about', 'contact'];
    return validPages.includes(hash) ? hash : 'home';
  });
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
    const handleHashChange = () => {
      const hash = window.location.hash.replace('#', '') as PublicPage;
      const validPages: PublicPage[] = ['home', 'features', 'pricing', 'register', 'status', 'about', 'contact'];
      if (validPages.includes(hash)) {
        setCurrentPage(hash);
      } else if (!window.location.hash) {
        setCurrentPage('home');
      }
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
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
