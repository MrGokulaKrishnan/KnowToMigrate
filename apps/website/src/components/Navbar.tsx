import { useState, useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Menu, X, Github } from 'lucide-react'

const LightningLogo = () => (
  <svg width="28" height="28" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="nav-bolt" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#FF8A00" />
        <stop offset="100%" stopColor="#FF4D00" />
      </linearGradient>
    </defs>
    <path d="M18 3L8 18h8l-2 11 14-15h-8l2-11z" fill="url(#nav-bolt)" />
  </svg>
)

const navLinks = [
  { to: '/features', label: 'Features' },
  { to: '/download', label: 'Download' },
  { to: '/docs', label: 'Docs' },
]

export function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  // Lock body scroll when mobile menu open
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [mobileOpen])

  const isActive = (to: string) => location.pathname === to

  return (
    <>
      <nav
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          zIndex: 50,
          transition: 'background 0.3s, border-color 0.3s, backdrop-filter 0.3s',
          background: scrolled
            ? 'rgba(0, 0, 0, 0.85)'
            : 'transparent',
          borderBottom: scrolled
            ? '1px solid rgba(255, 90, 0, 0.12)'
            : '1px solid transparent',
          backdropFilter: scrolled ? 'blur(20px)' : 'none',
          WebkitBackdropFilter: scrolled ? 'blur(20px)' : 'none',
        }}
      >
        <div
          style={{
            maxWidth: 1200,
            margin: '0 auto',
            padding: '0 1.5rem',
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          {/* Logo */}
          <Link
            to="/"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              textDecoration: 'none',
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 10,
                background: 'rgba(255, 90, 0, 0.12)',
                border: '1px solid rgba(255, 90, 0, 0.25)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <LightningLogo />
            </div>
            <span
              style={{
                fontWeight: 700,
                fontSize: '1rem',
                letterSpacing: '-0.02em',
                background: 'linear-gradient(135deg, #FF8A00 0%, #FF5A00 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
              }}
            >
              KnowToMigrate
            </span>
          </Link>

          {/* Desktop nav */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.25rem',
            }}
            className="hidden md:flex"
          >
            {navLinks.map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                style={{
                  padding: '0.4rem 1rem',
                  borderRadius: 8,
                  fontSize: '0.9rem',
                  fontWeight: 500,
                  textDecoration: 'none',
                  transition: 'color 0.2s, background 0.2s',
                  color: isActive(to) ? '#FF8A00' : 'rgba(255,255,255,0.7)',
                  background: isActive(to) ? 'rgba(255,90,0,0.08)' : 'transparent',
                }}
              >
                {label}
              </Link>
            ))}
            <a
              href="https://github.com/MrGokulaKrishnan/KnowToMigrate"
              target="_blank"
              rel="noopener noreferrer"
              style={{
                marginLeft: '0.5rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.375rem',
                padding: '0.4rem 1rem',
                borderRadius: 8,
                fontSize: '0.9rem',
                fontWeight: 500,
                textDecoration: 'none',
                color: 'rgba(255,255,255,0.7)',
                border: '1px solid rgba(255,255,255,0.1)',
                transition: 'color 0.2s, border-color 0.2s, background 0.2s',
              }}
              onMouseEnter={e => {
                const el = e.currentTarget as HTMLAnchorElement
                el.style.color = '#FF8A00'
                el.style.borderColor = 'rgba(255,90,0,0.3)'
                el.style.background = 'rgba(255,90,0,0.06)'
              }}
              onMouseLeave={e => {
                const el = e.currentTarget as HTMLAnchorElement
                el.style.color = 'rgba(255,255,255,0.7)'
                el.style.borderColor = 'rgba(255,255,255,0.1)'
                el.style.background = 'transparent'
              }}
            >
              <Github size={15} />
              GitHub
            </a>
            <Link
              to="/download"
              className="km-btn-primary"
              style={{ marginLeft: '0.75rem', padding: '0.45rem 1.25rem', fontSize: '0.875rem', borderRadius: 10 }}
            >
              Download
            </Link>
          </div>

          {/* Mobile hamburger */}
          <button
            className="flex md:hidden"
            onClick={() => setMobileOpen(v => !v)}
            style={{
              background: 'none',
              border: 'none',
              color: '#fff',
              cursor: 'pointer',
              padding: '0.5rem',
              borderRadius: 8,
            }}
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </nav>

      {/* Mobile menu overlay */}
      {mobileOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            zIndex: 40,
            background: 'rgba(0,0,0,0.95)',
            backdropFilter: 'blur(24px)',
            WebkitBackdropFilter: 'blur(24px)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.5rem',
            paddingTop: 64,
          }}
        >
          {navLinks.map(({ to, label }) => (
            <Link
              key={to}
              to={to}
              style={{
                display: 'block',
                padding: '1rem 2rem',
                fontSize: '1.25rem',
                fontWeight: 600,
                textDecoration: 'none',
                color: isActive(to) ? '#FF8A00' : '#fff',
                textAlign: 'center',
                width: '100%',
              }}
            >
              {label}
            </Link>
          ))}
          <a
            href="https://github.com/MrGokulaKrishnan/KnowToMigrate"
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '1rem 2rem',
              fontSize: '1.25rem',
              fontWeight: 600,
              textDecoration: 'none',
              color: '#fff',
            }}
          >
            <Github size={20} />
            GitHub
          </a>
          <div style={{ marginTop: '1rem' }}>
            <Link to="/download" className="km-btn-primary">
              Download Now
            </Link>
          </div>
        </div>
      )}
    </>
  )
}
