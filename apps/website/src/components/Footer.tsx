import { Link } from 'react-router-dom'
import { Github, Shield, Heart } from 'lucide-react'

const LightningLogo = () => (
  <svg width="22" height="22" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="footer-bolt" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#FF8A00" />
        <stop offset="100%" stopColor="#FF4D00" />
      </linearGradient>
    </defs>
    <path d="M18 3L8 18h8l-2 11 14-15h-8l2-11z" fill="url(#footer-bolt)" />
  </svg>
)

const footerLinks = {
  Product: [
    { label: 'Features', to: '/features' },
    { label: 'Download', to: '/download' },
    { label: 'Documentation', to: '/docs' },
  ],
  Legal: [
    { label: 'Privacy', href: '#' },
    { label: 'Security', href: '#' },
    { label: 'License (MIT)', href: 'https://github.com/MrGokulaKrishnan/KnowToMigrate/blob/main/LICENSE' },
  ],
  Community: [
    { label: 'GitHub', href: 'https://github.com/MrGokulaKrishnan/KnowToMigrate' },
    { label: 'Issues', href: 'https://github.com/MrGokulaKrishnan/KnowToMigrate/issues' },
    { label: 'Discussions', href: 'https://github.com/MrGokulaKrishnan/KnowToMigrate/discussions' },
  ],
}

export function Footer() {
  return (
    <footer
      style={{
        borderTop: '1px solid rgba(255, 90, 0, 0.1)',
        background: 'rgba(0,0,0,0.95)',
        padding: '4rem 1.5rem 2rem',
        marginTop: 'auto',
      }}
    >
      <div style={{ maxWidth: 1200, margin: '0 auto' }}>
        {/* Top row */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '3rem',
            marginBottom: '3rem',
          }}
        >
          {/* Brand */}
          <div>
            <Link
              to="/"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                textDecoration: 'none',
                marginBottom: '0.75rem',
              }}
            >
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: 'rgba(255,90,0,0.1)',
                  border: '1px solid rgba(255,90,0,0.2)',
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
                  fontSize: '0.95rem',
                  background: 'linear-gradient(135deg, #FF8A00, #FF4D00)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                  backgroundClip: 'text',
                }}
              >
                KnowToMigrate
              </span>
            </Link>
            <p
              style={{
                fontSize: '0.85rem',
                color: 'rgba(255,255,255,0.45)',
                lineHeight: 1.6,
                maxWidth: 220,
                margin: '0 0 1rem',
              }}
            >
              Move Anything. Anywhere. Seamlessly. Secure, offline-first file migration.
            </p>
            {/* Open source badge */}
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.375rem',
                padding: '0.3rem 0.75rem',
                borderRadius: 20,
                background: 'rgba(255,90,0,0.08)',
                border: '1px solid rgba(255,90,0,0.2)',
                fontSize: '0.75rem',
                fontWeight: 600,
                color: '#FF8A00',
              }}
            >
              <Github size={12} />
              Open Source · MIT
            </div>
          </div>

          {/* Link columns */}
          {Object.entries(footerLinks).map(([section, links]) => (
            <div key={section}>
              <h4
                style={{
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: '0.08em',
                  color: 'rgba(255,255,255,0.35)',
                  marginBottom: '1rem',
                }}
              >
                {section}
              </h4>
              <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {links.map(link => (
                  <li key={link.label}>
                    {'to' in link ? (
                      <Link
                        to={link.to}
                        style={{
                          fontSize: '0.875rem',
                          color: 'rgba(255,255,255,0.55)',
                          textDecoration: 'none',
                          transition: 'color 0.2s',
                        }}
                        onMouseEnter={e => ((e.target as HTMLAnchorElement).style.color = '#FF8A00')}
                        onMouseLeave={e => ((e.target as HTMLAnchorElement).style.color = 'rgba(255,255,255,0.55)')}
                      >
                        {link.label}
                      </Link>
                    ) : (
                      <a
                        href={link.href}
                        target={link.href?.startsWith('http') ? '_blank' : undefined}
                        rel="noopener noreferrer"
                        style={{
                          fontSize: '0.875rem',
                          color: 'rgba(255,255,255,0.55)',
                          textDecoration: 'none',
                          transition: 'color 0.2s',
                        }}
                        onMouseEnter={e => ((e.target as HTMLAnchorElement).style.color = '#FF8A00')}
                        onMouseLeave={e => ((e.target as HTMLAnchorElement).style.color = 'rgba(255,255,255,0.55)')}
                      >
                        {link.label}
                      </a>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Divider */}
        <div style={{ height: 1, background: 'rgba(255,255,255,0.06)', marginBottom: '1.5rem' }} />

        {/* Bottom row */}
        <div
          style={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '0.75rem',
          }}
        >
          <p style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.3)', margin: 0 }}>
            © {new Date().getFullYear()} KnowToMigrate. MIT License.
          </p>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8rem', color: 'rgba(255,255,255,0.3)' }}>
            <Shield size={13} />
            <span>No telemetry · No account · No cloud</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.375rem', fontSize: '0.8rem', color: 'rgba(255,255,255,0.3)' }}>
            <span>Made with</span>
            <Heart size={13} style={{ color: '#FF5A00' }} />
            <span>by MrGokulaKrishnan</span>
          </div>
        </div>
      </div>
    </footer>
  )
}
