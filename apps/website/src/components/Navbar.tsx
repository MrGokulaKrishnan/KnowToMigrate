import { useState, useEffect, useRef } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Menu, X, Github, LayoutGrid } from 'lucide-react'

const navLinks = [
  { to: '/features', label: 'Features' },
  { to: '/download', label: 'Download' },
  { to: '/docs',     label: 'Docs' },
]

interface NavbarProps {
  onOpenAppPreview?: () => void
}

export function Navbar({ onOpenAppPreview }: NavbarProps) {
  const [scrolled, setScrolled]       = useState(false)
  const [mobileOpen, setMobileOpen]   = useState(false)
  const [menuHeight, setMenuHeight]   = useState(0)
  const menuRef                        = useRef<HTMLDivElement>(null)
  const location                       = useLocation()

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // Close mobile menu on route change
  useEffect(() => setMobileOpen(false), [location])

  // Animate mobile menu height
  useEffect(() => {
    if (menuRef.current) {
      setMenuHeight(mobileOpen ? menuRef.current.scrollHeight : 0)
    }
  }, [mobileOpen])

  return (
    <header
      role="banner"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 50,
        transition: 'background 0.3s, border-color 0.3s',
        background: scrolled ? 'rgba(0,0,0,0.90)' : 'transparent',
        backdropFilter: scrolled ? 'blur(16px)' : 'none',
        WebkitBackdropFilter: scrolled ? 'blur(16px)' : 'none',
        borderBottom: scrolled ? '1px solid rgba(255,255,255,0.06)' : '1px solid transparent',
      }}
    >
      <div
        style={{
          maxWidth: 1200,
          margin: '0 auto',
          padding: '0 1.25rem',
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        {/* Logo */}
        <Link
          to="/"
          aria-label="KnowToMigrate home"
          style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', textDecoration: 'none', flexShrink: 0 }}
        >
          <img
            src="/logo.jpg"
            alt=""
            aria-hidden="true"
            style={{
              width: 34,
              height: 34,
              borderRadius: 9,
              border: '1px solid rgba(255,90,0,0.30)',
              objectFit: 'cover',
              boxShadow: '0 0 12px rgba(255,90,0,0.15)',
              flexShrink: 0,
            }}
          />
          <span
            style={{
              fontWeight: 700,
              fontSize: '0.9375rem',
              letterSpacing: '-0.02em',
              background: 'linear-gradient(135deg, #FF8A00 0%, #FF5A00 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
              whiteSpace: 'nowrap',
            }}
          >
            KnowToMigrate
          </span>
        </Link>

        {/* Desktop nav */}
        <nav
          aria-label="Main navigation"
          style={{ display: 'flex', alignItems: 'center', gap: '0.125rem' }}
          className="hidden md:flex"
        >
          {navLinks.map(({ to, label }) => (
            <Link
              key={to}
              to={to}
              aria-current={location.pathname === to ? 'page' : undefined}
              style={{
                padding: '0.375rem 0.875rem',
                borderRadius: 8,
                fontSize: '0.875rem',
                fontWeight: 500,
                textDecoration: 'none',
                color: location.pathname === to ? '#FF8A00' : 'rgba(255,255,255,0.60)',
                background: location.pathname === to ? 'rgba(255,90,0,0.08)' : 'transparent',
                transition: 'color 0.2s, background 0.2s',
              }}
            >
              {label}
            </Link>
          ))}
        </nav>

        {/* Right actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
          {/* App Preview button  -  desktop */}
          {onOpenAppPreview && (
            <button
              onClick={onOpenAppPreview}
              title="Open App Preview"
              aria-label="Open App Preview"
              className="hidden md:flex"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.375rem',
                padding: '0.375rem 0.75rem',
                borderRadius: 8,
                fontSize: '0.8125rem',
                fontWeight: 600,
                color: '#FF8A00',
                background: 'rgba(255,90,0,0.08)',
                border: '1px solid rgba(255,90,0,0.20)',
                cursor: 'pointer',
                transition: 'background 0.2s, border-color 0.2s',
              }}
            >
              <LayoutGrid size={14} />
              App
            </button>
          )}

          <a
            href="https://github.com/MrGokulaKrishnan/KnowToMigrate"
            target="_blank"
            rel="noopener noreferrer"
            aria-label="GitHub repository"
            className="hidden md:flex"
            style={{ color: 'rgba(255,255,255,0.40)', display: 'flex', alignItems: 'center', transition: 'color 0.2s' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#fff')}
            onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.40)')}
          >
            <Github size={18} />
          </a>

          <Link
            to="/download"
            className="hidden md:flex"
            style={{
              padding: '0.4375rem 1rem',
              borderRadius: 9,
              fontSize: '0.8125rem',
              fontWeight: 600,
              textDecoration: 'none',
              color: '#fff',
              background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
              boxShadow: '0 2px 10px rgba(255,90,0,0.28)',
              whiteSpace: 'nowrap',
            }}
          >
            Download
          </Link>

          {/* Mobile hamburger */}
          <button
            onClick={() => setMobileOpen(v => !v)}
            aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={mobileOpen}
            aria-controls="mobile-nav"
            className="flex md:hidden"
            style={{
              color: '#fff',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              padding: 4,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {mobileOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      {/* Mobile menu  -  animated height */}
      <div
        id="mobile-nav"
        ref={menuRef}
        role="navigation"
        aria-label="Mobile navigation"
        style={{
          overflow: 'hidden',
          height: menuHeight,
          transition: 'height 0.28s cubic-bezier(0.16,1,0.3,1)',
          background: 'rgba(0,0,0,0.96)',
          borderTop: mobileOpen ? '1px solid rgba(255,255,255,0.06)' : 'none',
        }}
      >
        <div style={{ padding: '0.75rem 1.25rem 1.25rem' }}>
          {navLinks.map(({ to, label }) => (
            <Link
              key={to}
              to={to}
              aria-current={location.pathname === to ? 'page' : undefined}
              style={{
                display: 'block',
                padding: '0.75rem 0',
                borderBottom: '1px solid rgba(255,255,255,0.05)',
                fontSize: '1rem',
                fontWeight: 500,
                textDecoration: 'none',
                color: location.pathname === to ? '#FF8A00' : 'rgba(255,255,255,0.75)',
              }}
            >
              {label}
            </Link>
          ))}

          {onOpenAppPreview && (
            <button
              onClick={() => { onOpenAppPreview(); setMobileOpen(false) }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                width: '100%',
                marginTop: '0.75rem',
                padding: '0.625rem',
                borderRadius: 10,
                border: '1px solid rgba(255,90,0,0.25)',
                background: 'rgba(255,90,0,0.06)',
                color: '#FF8A00',
                fontWeight: 600,
                fontSize: '0.9375rem',
                cursor: 'pointer',
              }}
            >
              <LayoutGrid size={16} /> Open App Preview
            </button>
          )}

          <Link
            to="/download"
            style={{
              display: 'block',
              marginTop: '0.75rem',
              padding: '0.75rem',
              borderRadius: 10,
              textAlign: 'center',
              textDecoration: 'none',
              fontWeight: 600,
              fontSize: '0.9375rem',
              color: '#fff',
              background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
            }}
          >
            Download Beta
          </Link>
        </div>
      </div>
    </header>
  )
}
