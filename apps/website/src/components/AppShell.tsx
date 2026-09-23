import { useState, useCallback } from 'react'
import {
  Home, Upload, Download as DownloadIcon, Zap, History,
  Settings, X, ChevronRight, LayoutGrid
} from 'lucide-react'
import { HomeView } from '../views/HomeView'
import { SendView } from '../views/SendView'
import { ReceiveView } from '../views/ReceiveView'
import { TransferView } from '../views/TransferView'
import { MigrationView } from '../views/MigrationView'
import { HistoryView } from '../views/HistoryView'
import { SettingsView } from '../views/SettingsView'
import { ErrorBoundary } from './ErrorBoundary'
import { useToast } from './Toast'

type ViewId = 'home' | 'send' | 'receive' | 'transfer' | 'migration' | 'history' | 'settings'

const NAV_ITEMS: { id: ViewId; label: string; icon: React.ElementType; badge?: string }[] = [
  { id: 'home',      label: 'Devices',   icon: LayoutGrid },
  { id: 'send',      label: 'Send',      icon: Upload },
  { id: 'receive',   label: 'Receive',   icon: DownloadIcon },
  { id: 'migration', label: 'Migrate',   icon: Zap,     badge: 'Wizard' },
  { id: 'history',   label: 'History',   icon: History },
  { id: 'settings',  label: 'Settings',  icon: Settings },
]

export function AppShell({ onClose }: { onClose: () => void }) {
  const [activeView, setActiveView] = useState<ViewId>('home')
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [prevView, setPrevView] = useState<ViewId | null>(null)
  const toast = useToast()

  const navigate = useCallback((view: ViewId) => {
    setPrevView(activeView)
    setActiveView(view)
  }, [activeView])

  const handleSelectDevice = useCallback(() => {
    navigate('send')
  }, [navigate])

  const handleStartTransfer = useCallback(() => {
    navigate('transfer')
  }, [navigate])

  const handleTransferComplete = useCallback(() => {
    toast.success('Transfer Complete', 'All files verified with SHA-256 Merkle root.')
    navigate('history')
  }, [navigate, toast])

  const handleCancel = useCallback(() => {
    toast.info('Transfer Cancelled', 'The transfer was stopped. No partial files written.')
    navigate('home')
  }, [navigate, toast])

  const handleSimulateIncoming = useCallback(() => {
    navigate('transfer')
  }, [navigate])

  const handleStartMigrationTransfer = useCallback(() => {
    navigate('transfer')
  }, [navigate])

  const renderView = () => {
    switch (activeView) {
      case 'home':
        return (
          <HomeView
            onSelectDevice={handleSelectDevice}
            onOpenMigration={() => navigate('migration')}
            onOpenWebReceiver={() => navigate('receive')}
          />
        )
      case 'send':
        return <SendView onStartTransfer={handleStartTransfer} />
      case 'receive':
        return <ReceiveView onSimulateIncoming={handleSimulateIncoming} />
      case 'transfer':
        return (
          <TransferView
            onTransferComplete={handleTransferComplete}
            onCancel={handleCancel}
          />
        )
      case 'migration':
        return <MigrationView onStartMigrationTransfer={handleStartMigrationTransfer} />
      case 'history':
        return <HistoryView />
      case 'settings':
        return <SettingsView />
      default:
        return null
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 100,
        background: 'rgba(0,0,0,0.88)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        display: 'flex',
        flexDirection: 'column',
        animation: 'km-fade-up 0.3s cubic-bezier(0.16,1,0.3,1) forwards',
      }}
    >
      {/* App header bar */}
      <div
        style={{
          height: 52,
          borderBottom: '1px solid rgba(255,255,255,0.07)',
          background: 'rgba(0,0,0,0.60)',
          display: 'flex',
          alignItems: 'center',
          padding: '0 1rem',
          gap: '0.75rem',
          flexShrink: 0,
        }}
      >
        <img
          src="/logo.jpg"
          alt="KM"
          style={{ width: 26, height: 26, borderRadius: 7, border: '1px solid rgba(255,90,0,0.30)', objectFit: 'cover' }}
        />
        <span
          style={{
            fontWeight: 700,
            fontSize: '0.875rem',
            background: 'linear-gradient(135deg, #FF8A00, #FF5A00)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}
        >
          KnowToMigrate
        </span>
        <span
          style={{
            fontSize: '0.6875rem',
            color: 'rgba(255,255,255,0.25)',
            padding: '0.125rem 0.5rem',
            borderRadius: 999,
            border: '1px solid rgba(255,255,255,0.08)',
          }}
        >
          App Preview
        </span>

        <div style={{ flex: 1 }} />

        <button
          onClick={() => setSidebarOpen(v => !v)}
          title="Toggle sidebar"
          aria-label="Toggle sidebar"
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'rgba(255,255,255,0.45)',
            display: 'flex',
            alignItems: 'center',
            padding: 4,
          }}
        >
          <ChevronRight
            size={16}
            style={{
              transform: sidebarOpen ? 'rotate(180deg)' : 'rotate(0deg)',
              transition: 'transform 0.25s cubic-bezier(0.16,1,0.3,1)',
            }}
          />
        </button>

        <button
          onClick={onClose}
          title="Close App Preview"
          aria-label="Close App Preview"
          style={{
            background: 'rgba(255,255,255,0.05)',
            border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 8,
            cursor: 'pointer',
            color: 'rgba(255,255,255,0.55)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 28,
            height: 28,
            transition: 'background 0.2s, color 0.2s',
          }}
          onMouseEnter={e => {
            ;(e.currentTarget as HTMLElement).style.background = 'rgba(239,68,68,0.15)'
            ;(e.currentTarget as HTMLElement).style.color = '#EF4444'
          }}
          onMouseLeave={e => {
            ;(e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.05)'
            ;(e.currentTarget as HTMLElement).style.color = 'rgba(255,255,255,0.55)'
          }}
        >
          <X size={14} />
        </button>
      </div>

      {/* Body */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Sidebar */}
        <nav
          aria-label="App navigation"
          style={{
            width: sidebarOpen ? 192 : 56,
            flexShrink: 0,
            borderRight: '1px solid rgba(255,255,255,0.06)',
            background: 'rgba(0,0,0,0.40)',
            padding: '0.75rem 0.5rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.25rem',
            transition: 'width 0.25s cubic-bezier(0.16,1,0.3,1)',
            overflowX: 'hidden',
          }}
        >
          {NAV_ITEMS.map(({ id, label, icon: Icon, badge }) => {
            const isActive = activeView === id
            return (
              <button
                key={id}
                onClick={() => navigate(id)}
                title={!sidebarOpen ? label : undefined}
                aria-label={label}
                aria-current={isActive ? 'page' : undefined}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.625rem',
                  padding: '0.5rem 0.625rem',
                  borderRadius: 10,
                  border: 'none',
                  cursor: 'pointer',
                  textAlign: 'left',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  width: '100%',
                  background: isActive ? 'rgba(255,90,0,0.12)' : 'transparent',
                  color: isActive ? '#FF8A00' : 'rgba(255,255,255,0.45)',
                  fontWeight: isActive ? 600 : 400,
                  fontSize: '0.8125rem',
                  transition: 'background 0.15s, color 0.15s',
                  boxShadow: isActive ? 'inset 0 0 0 1px rgba(255,90,0,0.20)' : 'none',
                }}
                onMouseEnter={e => {
                  if (!isActive) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.05)'
                }}
                onMouseLeave={e => {
                  if (!isActive) (e.currentTarget as HTMLElement).style.background = 'transparent'
                }}
              >
                <Icon size={16} style={{ flexShrink: 0 }} />
                {sidebarOpen && (
                  <>
                    <span style={{ flex: 1 }}>{label}</span>
                    {badge && (
                      <span
                        style={{
                          fontSize: '0.5625rem',
                          fontWeight: 700,
                          padding: '0.1rem 0.4rem',
                          borderRadius: 999,
                          background: 'rgba(255,90,0,0.15)',
                          color: '#FF8A00',
                          border: '1px solid rgba(255,90,0,0.25)',
                          letterSpacing: '0.04em',
                        }}
                      >
                        {badge}
                      </span>
                    )}
                  </>
                )}
              </button>
            )
          })}
        </nav>

        {/* View area */}
        <main
          style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'hidden',
            padding: '1.5rem',
          }}
        >
          <ErrorBoundary>
            <div
              key={activeView}
              style={{
                animation: 'km-fade-up 0.25s cubic-bezier(0.16,1,0.3,1) forwards',
              }}
            >
              {renderView()}
            </div>
          </ErrorBoundary>
        </main>
      </div>
    </div>
  )
}
