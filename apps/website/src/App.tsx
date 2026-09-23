import { useState } from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import { Navbar } from './components/Navbar'
import { Footer } from './components/Footer'
import { ErrorBoundary } from './components/ErrorBoundary'
import { ToastProvider } from './components/Toast'
import { TransferProvider } from './context/TransferContext'
import { AppShell } from './components/AppShell'
import { HomePage } from './pages/HomePage'
import { DownloadPage } from './pages/DownloadPage'
import { FeaturesPage } from './pages/FeaturesPage'
import { DocsPage } from './pages/DocsPage'
import { SecurityPage } from './pages/SecurityPage'
import { PrivacyPage } from './pages/PrivacyPage'
import { NotFoundPage } from './pages/NotFoundPage'

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
            <Routes location={location}>
              <Route path="/" element={<HomePage onOpenAppPreview={() => setAppShellOpen(true)} />} />
              <Route path="/download" element={<DownloadPage />} />
              <Route path="/features" element={<FeaturesPage />} />
              <Route path="/docs" element={<DocsPage />} />
              <Route path="/security" element={<SecurityPage />} />
              <Route path="/privacy" element={<PrivacyPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </div>
        </ErrorBoundary>
      </main>
      <Footer />

      {/* App Shell Overlay */}
      {appShellOpen && (
        <ToastProvider>
          <AppShell onClose={() => setAppShellOpen(false)} />
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
