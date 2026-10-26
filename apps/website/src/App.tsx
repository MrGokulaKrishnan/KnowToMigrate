import { useState, lazy, Suspense } from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import { Footer } from './components/Footer'
import { ErrorBoundary } from './components/ErrorBoundary'
import { ToastProvider } from './components/Toast'
import { TransferProvider } from './context/TransferContext'
import { HomePage } from './pages/HomePage'

// Lazy-loaded routes for code-splitting (landing page kept eager for instant FCP)
const DownloadPage = lazy(() => import('./pages/DownloadPage').then(m => ({ default: m.DownloadPage })))
const FeaturesPage = lazy(() => import('./pages/FeaturesPage').then(m => ({ default: m.FeaturesPage })))
const DocsPage = lazy(() => import('./pages/DocsPage').then(m => ({ default: m.DocsPage })))
const SecurityPage = lazy(() => import('./pages/SecurityPage').then(m => ({ default: m.SecurityPage })))
const PrivacyPage = lazy(() => import('./pages/PrivacyPage').then(m => ({ default: m.PrivacyPage })))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then(m => ({ default: m.NotFoundPage })))
const AppShell = lazy(() => import('./components/AppShell').then(m => ({ default: m.AppShell })))

function PageFallback() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '60vh' }}>
      <div
        style={{
          width: 32,
          height: 32,
          borderRadius: '50%',
          border: '2px solid rgba(255, 90, 0, 0.2)',
          borderTopColor: '#FF5A00',
          animation: 'km-spin 0.8s linear infinite',
        }}
      />
      <style>{`
        @keyframes km-spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  )
}

function AppContent() {
  const location = useLocation()
  const [appShellOpen, setAppShellOpen] = useState(false)

  return (
    <div style={{ minHeight: '100vh', background: '#000', color: '#fff', display: 'flex', flexDirection: 'column' }}>
      <Navbar onOpenAppPreview={() => setAppShellOpen(true)} />
      <main style={{ flex: 1 }}>
        <ErrorBoundary>
          {/* Animate page transitions */}
          <div key={location.pathname} style={{ animation: 'km-fade-up 0.35s cubic-bezier(0.16,1,0.3,1) forwards' }}>
            <Suspense fallback={<PageFallback />}>
              <Routes location={location}>
                <Route path="/" element={<HomePage onOpenAppPreview={() => setAppShellOpen(true)} />} />
                <Route path="/download" element={<DownloadPage />} />
                <Route path="/features" element={<FeaturesPage />} />
                <Route path="/docs" element={<DocsPage />} />
                <Route path="/security" element={<SecurityPage />} />
                <Route path="/privacy" element={<PrivacyPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </Suspense>
          </div>
        </ErrorBoundary>
      </main>
      <Footer />

      {/* App Shell Overlay */}
      {appShellOpen && (
        <ToastProvider>
          <Suspense fallback={<PageFallback />}>
            <AppShell onClose={() => setAppShellOpen(false)} />
          </Suspense>
        </ToastProvider>
      )}
    </div>
  )
}

export default function App() {
  return (
    <ToastProvider>
      <TransferProvider>
        <AppContent />
      </TransferProvider>
    </ToastProvider>
  )
}
