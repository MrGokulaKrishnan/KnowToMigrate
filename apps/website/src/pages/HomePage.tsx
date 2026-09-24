import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { Shield, Wifi, Smartphone, Monitor, ArrowRight, CheckCircle, Zap, RefreshCw, Lock, LayoutGrid } from 'lucide-react'

/* ─── Particle background ──────────────────────────────────── */
function ParticleField() {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    let animId: number
    const resize = () => { canvas.width = canvas.offsetWidth; canvas.height = canvas.offsetHeight }
    resize()
    window.addEventListener('resize', resize, { passive: true })

    // Respect reduced-motion
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReducedMotion) return

    const dots = Array.from({ length: 55 }, () => ({
      x: Math.random() * canvas.width, y: Math.random() * canvas.height,
      r: Math.random() * 1.5 + 0.5, vx: (Math.random() - 0.5) * 0.22,
      vy: (Math.random() - 0.5) * 0.22, alpha: Math.random() * 0.3 + 0.07,
    }))
    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      dots.forEach(d => {
        d.x += d.vx; d.y += d.vy
        if (d.x < 0) d.x = canvas.width
        if (d.x > canvas.width) d.x = 0
        if (d.y < 0) d.y = canvas.height
        if (d.y > canvas.height) d.y = 0
        ctx.beginPath(); ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(255,90,0,${d.alpha})`; ctx.fill()
      })
      for (let i = 0; i < dots.length; i++) {
        for (let j = i + 1; j < dots.length; j++) {
          const dx = dots[i].x - dots[j].x, dy = dots[i].y - dots[j].y
          const dist = Math.sqrt(dx * dx + dy * dy)
          if (dist < 110) {
            ctx.beginPath(); ctx.moveTo(dots[i].x, dots[i].y); ctx.lineTo(dots[j].x, dots[j].y)
            ctx.strokeStyle = `rgba(255,90,0,${0.04 * (1 - dist / 110)})`
            ctx.lineWidth = 0.5; ctx.stroke()
          }
        }
      }
      animId = requestAnimationFrame(draw)
    }
    draw()
    return () => { cancelAnimationFrame(animId); window.removeEventListener('resize', resize) }
  }, [])
  return <canvas ref={canvasRef} aria-hidden="true" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', pointerEvents: 'none' }} />
}

/* ─── Features ─────────────────────────────────────────────── */
const FEATURES = [
  { icon: Wifi,        title: 'Offline Local Transfer',    desc: 'Transfers over the same Wi-Fi network  -  no internet, no cloud, no upload. Both devices must be on the same local network.' },
  { icon: Zap,         title: 'Streaming 8 MB Chunks',     desc: 'Files never fully load into RAM. Each 8 MB chunk is streamed, hashed, and ACK\'d independently  -  enabling 100 GB+ transfers.' },
  { icon: RefreshCw,   title: 'Resumable Transfers',       desc: 'Checkpoint files track each verified chunk. If your connection drops, only unverified chunks are re-sent on reconnect.' },
  { icon: Shield,      title: 'AES-256-GCM Encryption',    desc: 'All data is encrypted with AES-256-GCM using ephemeral session keys derived via X25519 ECDH. Keys exist only for the session.' },
  { icon: CheckCircle, title: 'SHA-256 Integrity',         desc: 'Every chunk carries a SHA-256 digest. A Merkle root over all chunks is verified after transfer completes on both sides.' },
  { icon: Lock,        title: 'No Account Required',       desc: 'No sign-up, no email, no cloud account. Device identity is a locally-generated keypair stored on your device.' },
  { icon: Smartphone,  title: 'Android Native App',        desc: 'Kotlin + Jetpack Compose. JNI bridge to the Rust core library (libktm_ffi.so). Foreground service for background receiving.' },
  { icon: Monitor,     title: 'Windows Native App',        desc: 'WinUI 3 + C# with P/Invoke to ktm_ffi.dll. AMOLED-black design system, native window chrome, system tray support planned.' },
]

interface HomePageProps {
  onOpenAppPreview?: () => void
}

export function HomePage({ onOpenAppPreview }: HomePageProps) {
  // Update document title
  useEffect(() => {
    document.title = 'KnowToMigrate  -  Move Anything. Anywhere. Seamlessly.'
  }, [])

  return (
    <main style={{ minHeight: '100vh', background: '#000', color: '#fff' }}>

      {/* ── Beta Banner ─────────────────────────────────────── */}
      <div style={{
        position: 'sticky',
        top: 64,
        zIndex: 40,
        background: 'rgba(255,90,0,0.09)',
        borderBottom: '1px solid rgba(255,90,0,0.18)',
        padding: '0.5rem 1rem',
        textAlign: 'center',
        fontSize: '0.8125rem',
        color: '#FF8A00',
      }}>
        🚧 <strong>Public Beta</strong>  -  Android and Windows builds are being finalized. Local encrypted transfers are the current focus.
        &nbsp;
        <a
          href="https://github.com/MrGokulaKrishnan/KnowToMigrate"
          target="_blank"
          rel="noopener noreferrer"
          style={{ color: '#FF8A00', textDecoration: 'underline' }}
        >
          Follow progress on GitHub →
        </a>
      </div>

      {/* ── Hero ────────────────────────────────────────────── */}
      <section style={{ position: 'relative', overflow: 'hidden', paddingTop: '6rem', paddingBottom: '5rem', textAlign: 'center' }}>
        <ParticleField />
        <div style={{
          position: 'absolute', inset: 0, pointerEvents: 'none',
          background: 'radial-gradient(ellipse 70% 50% at 50% 0%, rgba(255,90,0,0.10) 0%, transparent 70%)',
        }} />

        <div style={{ position: 'relative', maxWidth: 720, margin: '0 auto', padding: '0 1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '2rem' }}>
            <img
              src="/logo.jpg"
              alt="KnowToMigrate Logo"
              width={120}
              height={120}
              style={{
                width: 120,
                height: 120,
                borderRadius: 28,
                border: '2px solid rgba(255,90,0,0.40)',
                objectFit: 'cover',
                boxShadow: '0 0 60px rgba(255,90,0,0.22), 0 0 120px rgba(255,90,0,0.07)',
                display: 'block',
              }}
            />
          </div>

          <div style={{
            display: 'inline-block',
            padding: '0.3rem 1rem',
            borderRadius: 999,
            fontSize: '0.75rem',
            fontWeight: 700,
            letterSpacing: '0.12em',
            color: '#FF5A00',
            border: '1px solid rgba(255,90,0,0.30)',
            background: 'rgba(255,90,0,0.08)',
            marginBottom: '1.5rem',
          }}>
            PUBLIC BETA
          </div>

          <h1 style={{ fontSize: 'clamp(2rem, 5vw, 3.5rem)', fontWeight: 800, lineHeight: 1.1, letterSpacing: '-0.03em', marginBottom: '1.25rem' }}>
            Move Anything.{' '}
            <span style={{
              background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}>
              Anywhere.
            </span>
            {' '}Seamlessly.
          </h1>

          <p style={{ fontSize: '1.0625rem', color: 'rgba(255,255,255,0.50)', lineHeight: 1.7, marginBottom: '2rem', maxWidth: 500, margin: '0 auto 2rem' }}>
            A standalone application for encrypted local-network file transfer and device migration.
            No cloud. No account. No browser required.
          </p>

          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap', marginBottom: '3rem' }}>
            <Link to="/download" style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
              padding: '0.75rem 1.75rem', borderRadius: 12, fontWeight: 600,
              fontSize: '0.9375rem', textDecoration: 'none', color: '#fff',
              background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
              boxShadow: '0 4px 24px rgba(255,90,0,0.30)',
              transition: 'transform 0.2s, box-shadow 0.2s',
            }}>
              Join Beta <ArrowRight size={16} />
            </Link>

            {onOpenAppPreview && (
              <button
                onClick={onOpenAppPreview}
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
                  padding: '0.75rem 1.75rem', borderRadius: 12, fontWeight: 600,
                  fontSize: '0.9375rem', color: '#FF8A00',
                  border: '1px solid rgba(255,90,0,0.30)', background: 'rgba(255,90,0,0.07)',
                  cursor: 'pointer',
                  transition: 'background 0.2s, border-color 0.2s',
                }}
              >
                <LayoutGrid size={16} /> Live App Preview
              </button>
            )}

            <a
              href="https://github.com/MrGokulaKrishnan/KnowToMigrate"
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
                padding: '0.75rem 1.75rem', borderRadius: 12, fontWeight: 600,
                fontSize: '0.9375rem', textDecoration: 'none', color: 'rgba(255,255,255,0.70)',
                border: '1px solid rgba(255,255,255,0.10)', background: 'rgba(255,255,255,0.03)',
              }}
            >
              View Source
            </a>
          </div>

          {/* Trust pills */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.875rem', justifyContent: 'center' }}>
            {['AES-256-GCM', 'SHA-256 Merkle', 'X25519 ECDH', 'No Account', 'Works Offline', 'Open Source'].map(t => (
              <span key={t} style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.40)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <CheckCircle size={12} style={{ color: '#FF5A00' }} /> {t}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* ── What it is ─────────────────────────────────────── */}
      <section style={{ padding: '4rem 1.25rem', maxWidth: 920, margin: '0 auto' }}>
        <div style={{
          borderRadius: 20,
          border: '1px solid rgba(255,90,0,0.18)',
          background: 'rgba(255,90,0,0.04)',
          padding: '2rem',
        }}>
          <h2 style={{ fontSize: '1.125rem', fontWeight: 700, marginBottom: '1rem', color: '#fff' }}>
            What KnowToMigrate actually is (Beta status)
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.5rem' }}>
            {[
              { label: '✅ Working now', items: ['Rust core library (ktm_ffi.dll / libktm_ffi.a) compiled', 'AES-256-GCM encryption layer', 'X25519 key exchange', 'SHA-256 Merkle integrity', 'UDP discovery (port 54123)', 'TCP chunk streaming (port 54124)', '8 MB streaming chunker + checkpoints'] },
              { label: '🔨 In development', items: ['Android APK (Kotlin + Compose + JNI bridge)', 'Windows EXE (WinUI 3 + P/Invoke)', 'QR pairing UI flow', 'Transfer resume UI', 'Migration wizard (9-step)', 'End-to-end integration testing'] },
              { label: '📋 Planned', items: ['Signed installer releases', 'Google Play / Sideload distribution', 'macOS app (.dmg)', 'Linux AppImage', 'Internet relay fallback (STUN/TURN)', 'Web receiver (optional, browser-based)'] },
            ].map(({ label, items }) => (
              <div key={label}>
                <p style={{ fontWeight: 700, color: '#FF8A00', marginBottom: '0.75rem', fontSize: '0.875rem' }}>{label}</p>
                <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                  {items.map(i => (
                    <li key={i} style={{ fontSize: '0.8125rem', color: 'rgba(255,255,255,0.40)', padding: '0.2rem 0' }}>
                       -  {i}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Connectivity modes ────────────────────────────── */}
      <section style={{ padding: '2rem 1.25rem 4rem', maxWidth: 920, margin: '0 auto' }}>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 700, textAlign: 'center', marginBottom: '0.5rem' }}>
          How connections work
        </h2>
        <p style={{ color: 'rgba(255,255,255,0.35)', textAlign: 'center', marginBottom: '2rem', fontSize: '0.875rem' }}>
          These are materially different modes  -  choose based on your setup.
        </p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          {[
            { mode: 'Same Wi-Fi',    icon: '📶', status: 'Implemented', desc: 'Both devices on the same router. UDP broadcast discovers peers automatically. No internet needed.', color: '#22C55E' },
            { mode: 'Wi-Fi Hotspot', icon: '📡', status: 'Implemented', desc: 'One device creates a hotspot; the other connects to it. Same protocol, direct link.', color: '#22C55E' },
            { mode: 'Wi-Fi Direct',  icon: '🔗', status: 'Planned',     desc: 'Android-to-Android peer-to-peer without a router. Requires Wi-Fi Direct API integration.', color: '#F59E0B' },
            { mode: 'Internet Relay',icon: '🌐', status: 'Planned',     desc: 'Encrypted STUN/TURN relay for cross-network transfers. Optional  -  all local-mode transfers work without it.', color: '#F59E0B' },
          ].map(({ mode, icon, status, desc, color }) => (
            <div key={mode} style={{
              borderRadius: 16,
              border: '1px solid rgba(255,255,255,0.06)',
              background: 'rgba(255,255,255,0.02)',
              padding: '1.25rem',
              transition: 'border-color 0.2s, background 0.2s',
            }}>
              <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>{icon}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <p style={{ fontWeight: 600, color: '#fff', fontSize: '0.9375rem', margin: 0 }}>{mode}</p>
                <span style={{ fontSize: '0.6875rem', padding: '0.1rem 0.5rem', borderRadius: 999, background: `${color}20`, color, fontWeight: 600 }}>{status}</span>
              </div>
              <p style={{ fontSize: '0.8125rem', color: 'rgba(255,255,255,0.38)', lineHeight: 1.6, margin: 0 }}>{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Features grid ────────────────────────────────── */}
      <section style={{ padding: '4rem 1.25rem', maxWidth: 1100, margin: '0 auto' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, textAlign: 'center', marginBottom: '0.75rem' }}>
          Core capabilities
        </h2>
        <p style={{ color: 'rgba(255,255,255,0.38)', textAlign: 'center', marginBottom: '3rem', fontSize: '0.9375rem' }}>
          Implemented in the Rust core library  -  UI wrappers for Android and Windows are in development.
        </p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
          {FEATURES.map(({ icon: Icon, title, desc }) => (
            <div
              key={title}
              className="glass-card"
              style={{ padding: '1.5rem' }}
            >
              <div style={{
                width: 40, height: 40, borderRadius: 12,
                background: 'rgba(255,90,0,0.10)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1rem'
              }}>
                <Icon size={20} style={{ color: '#FF5A00' }} strokeWidth={1.75} />
              </div>
              <h3 style={{ fontSize: '0.9375rem', fontWeight: 600, color: '#fff', marginBottom: '0.5rem' }}>{title}</h3>
              <p style={{ fontSize: '0.8125rem', color: 'rgba(255,255,255,0.40)', lineHeight: 1.65, margin: 0 }}>{desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA ──────────────────────────────────────────── */}
      <section style={{ padding: '5rem 1.25rem', textAlign: 'center', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
        <img src="/logo.jpg" alt="KnowToMigrate" width={64} height={64} style={{ width: 64, height: 64, borderRadius: 16, border: '1px solid rgba(255,90,0,0.25)', objectFit: 'cover', margin: '0 auto 1.5rem', display: 'block' }} />
        <h2 style={{ fontSize: 'clamp(1.5rem, 4vw, 2rem)', fontWeight: 800, marginBottom: '1rem' }}>Follow the build</h2>
        <p style={{ color: 'rgba(255,255,255,0.40)', marginBottom: '2rem', maxWidth: 420, margin: '0 auto 2rem', lineHeight: 1.65 }}>
          KnowToMigrate is developed openly on GitHub. Star the repo to get notified when the first signed release is published.
        </p>
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <a
            href="https://github.com/MrGokulaKrishnan/KnowToMigrate"
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
              padding: '0.75rem 1.75rem', borderRadius: 12, fontWeight: 600,
              fontSize: '0.9375rem', textDecoration: 'none', color: '#fff',
              background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
              boxShadow: '0 4px 24px rgba(255,90,0,0.30)',
            }}
          >
            Star on GitHub <ArrowRight size={16} />
          </a>
          {onOpenAppPreview && (
            <button
              onClick={onOpenAppPreview}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
                padding: '0.75rem 1.75rem', borderRadius: 12, fontWeight: 600,
                fontSize: '0.9375rem', color: '#FF8A00',
                border: '1px solid rgba(255,90,0,0.28)', background: 'rgba(255,90,0,0.06)',
                cursor: 'pointer',
              }}
            >
              <LayoutGrid size={16} /> Try App Preview
            </button>
          )}
        </div>
      </section>
    </main>
  )
}
