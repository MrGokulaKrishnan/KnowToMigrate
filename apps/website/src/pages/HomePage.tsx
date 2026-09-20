import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { Shield, Wifi, Smartphone, Monitor, ArrowRight, CheckCircle } from 'lucide-react'

/* ─── Animated background particles ──────────────────────── */
function ParticleField() {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let animId: number

    const resize = () => {
      canvas.width = canvas.offsetWidth
      canvas.height = canvas.offsetHeight
    }
    resize()
    window.addEventListener('resize', resize)

    interface Dot {
      x: number; y: number; r: number
      vx: number; vy: number; alpha: number
    }

    const dots: Dot[] = Array.from({ length: 70 }, () => ({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 1.5 + 0.5,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3,
      alpha: Math.random() * 0.4 + 0.1,
    }))

    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      dots.forEach(d => {
        d.x += d.vx
        d.y += d.vy
        if (d.x < 0) d.x = canvas.width
        if (d.x > canvas.width) d.x = 0
        if (d.y < 0) d.y = canvas.height
        if (d.y > canvas.height) d.y = 0

        ctx.beginPath()
        ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(255, 90, 0, ${d.alpha})`
        ctx.fill()
      })

      // Draw lines between close dots
      for (let i = 0; i < dots.length; i++) {
        for (let j = i + 1; j < dots.length; j++) {
          const dx = dots[i].x - dots[j].x
          const dy = dots[i].y - dots[j].y
          const dist = Math.sqrt(dx * dx + dy * dy)
          if (dist < 120) {
            ctx.beginPath()
            ctx.moveTo(dots[i].x, dots[i].y)
            ctx.lineTo(dots[j].x, dots[j].y)
            ctx.strokeStyle = `rgba(255, 90, 0, ${0.06 * (1 - dist / 120)})`
            ctx.lineWidth = 0.5
            ctx.stroke()
          }
        }
      }

      animId = requestAnimationFrame(draw)
    }
    draw()

    return () => {
      cancelAnimationFrame(animId)
      window.removeEventListener('resize', resize)
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: 'absolute',
        inset: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
      }}
    />
  )
}

/* ─── Large KM Lightning Bolt SVG ─────────────────────────── */
function HeroBolt() {
  return (
    <div
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '2rem',
      }}
    >
      {/* Glow rings */}
      {[1, 2, 3].map(i => (
        <div
          key={i}
          style={{
            position: 'absolute',
            width: 80 + i * 40,
            height: 80 + i * 40,
            borderRadius: '50%',
            border: `1px solid rgba(255,90,0,${0.15 / i})`,
            animation: `km-pulse-ring ${1.5 + i * 0.5}s ease-out infinite`,
            animationDelay: `${i * 0.4}s`,
          }}
        />
      ))}
      <div
        style={{
          width: 96,
          height: 96,
          borderRadius: 28,
          background: 'rgba(255,90,0,0.08)',
          border: '1px solid rgba(255,90,0,0.25)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          zIndex: 1,
          boxShadow: '0 0 60px rgba(255,90,0,0.25), inset 0 1px 0 rgba(255,255,255,0.05)',
        }}
      >
        <svg
          width="56"
          height="56"
          viewBox="0 0 32 32"
          fill="none"
          className="km-animate-lightning"
        >
          <defs>
            <linearGradient id="hero-bolt" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FF8A00" />
              <stop offset="100%" stopColor="#FF4D00" />
            </linearGradient>
          </defs>
          <path d="M18 3L8 18h8l-2 11 14-15h-8l2-11z" fill="url(#hero-bolt)" />
        </svg>
      </div>
    </div>
  )
}

/* ─── Transfer animation (Android → Bolt → Windows) ──────── */
function TransferDemo() {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '1.5rem',
        padding: '3rem 2rem',
        flexWrap: 'wrap',
      }}
    >
      {/* Android */}
      <div style={{ textAlign: 'center' }}>
        <div
          style={{
            width: 80,
            height: 128,
            borderRadius: 16,
            background: 'rgba(255,255,255,0.04)',
            border: '2px solid rgba(255,255,255,0.1)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 6,
            margin: '0 auto 0.75rem',
            boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
            animation: 'km-float 3s ease-in-out infinite',
          }}
        >
          <div
            style={{
              width: 28,
              height: 4,
              borderRadius: 2,
              background: 'rgba(255,255,255,0.15)',
            }}
          />
          <Smartphone size={32} style={{ color: '#FF8A00' }} />
          <div
            style={{
              width: 20,
              height: 3,
              borderRadius: 2,
              background: 'rgba(255,90,0,0.4)',
            }}
          />
        </div>
        <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.5)', fontWeight: 500 }}>Android</span>
      </div>

      {/* Data stream arrows */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
        {[0, 1, 2, 3].map(i => (
          <div
            key={i}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 2,
            }}
          >
            {[0, 1, 2, 3, 4].map(j => (
              <div
                key={j}
                style={{
                  width: 8,
                  height: 2,
                  borderRadius: 1,
                  background: '#FF5A00',
                  opacity: 0,
                  animation: `km-data-stream 1.8s ease-in-out infinite`,
                  animationDelay: `${(i * 0.2) + (j * 0.1)}s`,
                }}
              />
            ))}
            <ArrowRight size={12} style={{ color: '#FF5A00', opacity: 0.6 }} />
          </div>
        ))}
        <div
          style={{
            marginTop: 4,
            padding: '0.25rem 0.75rem',
            borderRadius: 20,
            background: 'rgba(255,90,0,0.1)',
            border: '1px solid rgba(255,90,0,0.2)',
            fontSize: '0.7rem',
            fontWeight: 700,
            color: '#FF8A00',
            letterSpacing: '0.05em',
          }}
        >
          AES-256-GCM
        </div>
      </div>

      {/* Lightning bolt */}
      <div
        style={{
          width: 52,
          height: 52,
          borderRadius: 14,
          background: 'linear-gradient(135deg, rgba(255,138,0,0.15), rgba(255,77,0,0.08))',
          border: '1px solid rgba(255,90,0,0.3)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 30px rgba(255,90,0,0.3)',
        }}
      >
        <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
          <defs>
            <linearGradient id="demo-bolt" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#FF8A00" />
              <stop offset="100%" stopColor="#FF4D00" />
            </linearGradient>
          </defs>
          <path d="M18 3L8 18h8l-2 11 14-15h-8l2-11z" fill="url(#demo-bolt)" />
        </svg>
      </div>

      {/* Data stream to Windows */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
        {[0, 1, 2, 3].map(i => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <ArrowRight size={12} style={{ color: '#FF5A00', opacity: 0.6 }} />
            {[0, 1, 2, 3, 4].map(j => (
              <div
                key={j}
                style={{
                  width: 8,
                  height: 2,
                  borderRadius: 1,
                  background: '#FF5A00',
                  opacity: 0,
                  animation: `km-data-stream 1.8s ease-in-out infinite`,
                  animationDelay: `${0.9 + (i * 0.2) + (j * 0.1)}s`,
                }}
              />
            ))}
          </div>
        ))}
        <div
          style={{
            marginTop: 4,
            padding: '0.25rem 0.75rem',
            borderRadius: 20,
            background: 'rgba(255,90,0,0.1)',
            border: '1px solid rgba(255,90,0,0.2)',
            fontSize: '0.7rem',
            fontWeight: 700,
            color: '#FF8A00',
            letterSpacing: '0.05em',
          }}
        >
          SHA-256 ✓
        </div>
      </div>

      {/* Windows */}
      <div style={{ textAlign: 'center' }}>
        <div
          style={{
            width: 120,
            height: 80,
            borderRadius: 10,
            background: 'rgba(255,255,255,0.04)',
            border: '2px solid rgba(255,255,255,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto',
            boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
            animation: 'km-float 3s ease-in-out infinite',
            animationDelay: '1s',
            position: 'relative',
          }}
        >
          <Monitor size={36} style={{ color: '#FF8A00' }} />
        </div>
        {/* Laptop base */}
        <div
          style={{
            width: 140,
            height: 6,
            borderRadius: '0 0 6px 6px',
            background: 'rgba(255,255,255,0.06)',
            border: '1px solid rgba(255,255,255,0.08)',
            margin: '0 auto 0.75rem',
          }}
        />
        <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.5)', fontWeight: 500 }}>Windows</span>
      </div>
    </div>
  )
}

/* ─── Feature card ────────────────────────────────────────── */
interface FeatureCardProps {
  icon: React.ReactNode
  title: string
  description: string
  delay?: number
}

function FeatureCard({ icon, title, description, delay = 0 }: FeatureCardProps) {
  return (
    <div
      className="km-card"
      style={{
        padding: '2rem',
        animation: `km-fade-up 0.6s ease forwards`,
        animationDelay: `${delay}ms`,
        opacity: 0,
      }}
    >
      <div
        style={{
          width: 52,
          height: 52,
          borderRadius: 14,
          background: 'rgba(255,90,0,0.1)',
          border: '1px solid rgba(255,90,0,0.2)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '1.25rem',
          color: '#FF8A00',
        }}
      >
        {icon}
      </div>
      <h3
        style={{
          fontSize: '1.1rem',
          fontWeight: 700,
          marginBottom: '0.6rem',
          color: '#fff',
        }}
      >
        {title}
      </h3>
      <p style={{ fontSize: '0.9rem', color: 'rgba(255,255,255,0.55)', lineHeight: 1.65, margin: 0 }}>
        {description}
      </p>
    </div>
  )
}

/* ─── Platform badge ──────────────────────────────────────── */
interface PlatformBadgeProps {
  icon: React.ReactNode
  name: string
  subtext: string
  available: boolean
}

function PlatformBadge({ icon, name, subtext, available }: PlatformBadgeProps) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '0.75rem',
        padding: '1.75rem 2rem',
        borderRadius: 20,
        background: available ? 'rgba(255,90,0,0.06)' : 'rgba(255,255,255,0.02)',
        border: `1px solid ${available ? 'rgba(255,90,0,0.2)' : 'rgba(255,255,255,0.06)'}`,
        transition: 'all 0.25s',
        cursor: available ? 'default' : 'default',
        minWidth: 140,
      }}
    >
      <div style={{ color: available ? '#FF8A00' : 'rgba(255,255,255,0.25)' }}>{icon}</div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: '0.95rem', fontWeight: 600, color: available ? '#fff' : 'rgba(255,255,255,0.3)' }}>
          {name}
        </div>
        <div style={{ fontSize: '0.75rem', color: available ? '#FF8A00' : 'rgba(255,255,255,0.2)', marginTop: 2 }}>
          {subtext}
        </div>
      </div>
    </div>
  )
}

/* ─── HomePage ────────────────────────────────────────────── */
export function HomePage() {
  return (
    <main>
      {/* ── Hero section ─────────────────────────────────── */}
      <section
        style={{
          position: 'relative',
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          overflow: 'hidden',
          background: '#000',
          paddingTop: 64,
        }}
      >
        <ParticleField />

        {/* Radial glow behind content */}
        <div
          style={{
            position: 'absolute',
            top: '40%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: 600,
            height: 600,
            borderRadius: '50%',
            background: 'radial-gradient(circle, rgba(255,90,0,0.08) 0%, transparent 70%)',
            pointerEvents: 'none',
          }}
        />

        <div
          style={{
            position: 'relative',
            zIndex: 1,
            textAlign: 'center',
            padding: '2rem 1.5rem',
            maxWidth: 760,
            margin: '0 auto',
          }}
        >
          <HeroBolt />

          {/* Pill label */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.35rem 1rem',
              borderRadius: 20,
              background: 'rgba(255,90,0,0.1)',
              border: '1px solid rgba(255,90,0,0.25)',
              fontSize: '0.8rem',
              fontWeight: 600,
              color: '#FF8A00',
              marginBottom: '1.5rem',
              letterSpacing: '0.04em',
            }}
          >
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#FF5A00', display: 'inline-block' }} />
            v1.0.0 — Now Available
          </div>

          {/* Main heading */}
          <h1
            style={{
              fontSize: 'clamp(2.5rem, 7vw, 4.5rem)',
              fontWeight: 800,
              letterSpacing: '-0.03em',
              lineHeight: 1.1,
              marginBottom: '1rem',
              background: 'linear-gradient(135deg, #FF8A00 0%, #FF5A00 50%, #FF4D00 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}
          >
            KnowToMigrate
          </h1>

          {/* Subheading */}
          <p
            style={{
              fontSize: 'clamp(1.1rem, 2.5vw, 1.5rem)',
              fontWeight: 600,
              color: 'rgba(255,255,255,0.9)',
              letterSpacing: '-0.01em',
              marginBottom: '1rem',
            }}
          >
            Move Anything. Anywhere. Seamlessly.
          </p>

          {/* Description */}
          <p
            style={{
              fontSize: '1rem',
              color: 'rgba(255,255,255,0.5)',
              lineHeight: 1.75,
              maxWidth: 560,
              margin: '0 auto 2.5rem',
            }}
          >
            A secure, standalone application for cross-platform file sharing and device migration.
            Works offline. No browser needed. No account required.
          </p>

          {/* CTA buttons */}
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link to="/download" className="km-btn-primary" style={{ fontSize: '1rem', padding: '0.875rem 2rem' }}>
              <Monitor size={18} />
              Download for Windows
            </Link>
            <Link to="/download" className="km-btn-secondary" style={{ fontSize: '1rem', padding: '0.875rem 2rem' }}>
              <Smartphone size={18} />
              Download for Android
            </Link>
          </div>

          {/* Trust badges */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '1.5rem',
              marginTop: '2rem',
              flexWrap: 'wrap',
            }}
          >
            {['No Account Required', 'No Cloud', 'Open Source'].map(badge => (
              <div
                key={badge}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  fontSize: '0.8rem',
                  color: 'rgba(255,255,255,0.35)',
                  fontWeight: 500,
                }}
              >
                <CheckCircle size={13} style={{ color: '#FF5A00' }} />
                {badge}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Transfer Demo ─────────────────────────────────── */}
      <section
        style={{
          background: 'rgba(0,0,0,0.98)',
          borderTop: '1px solid rgba(255,90,0,0.08)',
          borderBottom: '1px solid rgba(255,90,0,0.08)',
          padding: '4rem 1.5rem',
        }}
      >
        <div style={{ maxWidth: 1000, margin: '0 auto', textAlign: 'center' }}>
          <p
            style={{
              fontSize: '0.75rem',
              fontWeight: 700,
              letterSpacing: '0.12em',
              color: '#FF5A00',
              textTransform: 'uppercase',
              marginBottom: '0.5rem',
            }}
          >
            How It Works
          </p>
          <h2
            style={{
              fontSize: 'clamp(1.5rem, 3vw, 2.25rem)',
              fontWeight: 700,
              color: '#fff',
              marginBottom: '0.75rem',
              letterSpacing: '-0.02em',
            }}
          >
            Direct Device-to-Device Transfer
          </h2>
          <p style={{ color: 'rgba(255,255,255,0.45)', fontSize: '0.95rem', marginBottom: '1rem' }}>
            No intermediary servers. Files go directly from source to destination over your local network.
          </p>

          <TransferDemo />

          <div
            style={{
              display: 'flex',
              gap: '2rem',
              justifyContent: 'center',
              flexWrap: 'wrap',
              marginTop: '1rem',
            }}
          >
            {['Direct', 'Secure', 'Local', 'Offline'].map(tag => (
              <div
                key={tag}
                style={{
                  padding: '0.4rem 1.25rem',
                  borderRadius: 20,
                  background: 'rgba(255,90,0,0.08)',
                  border: '1px solid rgba(255,90,0,0.18)',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  color: '#FF8A00',
                }}
              >
                {tag}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Key Features ──────────────────────────────────── */}
      <section style={{ padding: '6rem 1.5rem', background: '#000' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <p
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                letterSpacing: '0.12em',
                color: '#FF5A00',
                textTransform: 'uppercase',
                marginBottom: '0.5rem',
              }}
            >
              Built Different
            </p>
            <h2
              style={{
                fontSize: 'clamp(1.75rem, 3.5vw, 2.5rem)',
                fontWeight: 700,
                color: '#fff',
                letterSpacing: '-0.02em',
                marginBottom: '0.75rem',
              }}
            >
              Why KnowToMigrate?
            </h2>
            <p style={{ color: 'rgba(255,255,255,0.45)', fontSize: '0.95rem', maxWidth: 480, margin: '0 auto' }}>
              Engineered from the ground up for security, speed, and simplicity.
            </p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: '1.25rem',
            }}
          >
            <FeatureCard
              icon={<Wifi size={24} />}
              title="Offline First"
              description="Transfer files without any internet connection. Your data travels only over your local Wi-Fi or direct connection — never through a cloud server."
              delay={0}
            />
            <FeatureCard
              icon={<Shield size={24} />}
              title="End-to-End Encrypted"
              description="Every transfer is protected with AES-256-GCM encryption. Files are encrypted on the sender's device and decrypted only on the recipient's — no one else can read your data."
              delay={100}
            />
            <FeatureCard
              icon={<Smartphone size={24} />}
              title="Cross-Platform"
              description="Transfer between Android and Windows today, with macOS and Linux coming soon. One app, every device, seamless experience across all platforms."
              delay={200}
            />
          </div>
        </div>
      </section>

      {/* ── Platform Badges ───────────────────────────────── */}
      <section
        style={{
          padding: '5rem 1.5rem',
          background: 'rgba(0,0,0,0.97)',
          borderTop: '1px solid rgba(255,255,255,0.04)',
        }}
      >
        <div style={{ maxWidth: 900, margin: '0 auto', textAlign: 'center' }}>
          <h2
            style={{
              fontSize: 'clamp(1.5rem, 3vw, 2rem)',
              fontWeight: 700,
              color: '#fff',
              marginBottom: '0.75rem',
              letterSpacing: '-0.02em',
            }}
          >
            Available on Your Platform
          </h2>
          <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: '0.9rem', marginBottom: '2.5rem' }}>
            Download the app for your device and start migrating in minutes.
          </p>

          <div
            style={{
              display: 'flex',
              gap: '1rem',
              justifyContent: 'center',
              flexWrap: 'wrap',
            }}
          >
            <PlatformBadge
              icon={<Monitor size={32} />}
              name="Windows"
              subtext="Windows 10/11 · 64-bit"
              available={true}
            />
            <PlatformBadge
              icon={<Smartphone size={32} />}
              name="Android"
              subtext="Android 8.0+"
              available={true}
            />
            <PlatformBadge
              icon={
                <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
                </svg>
              }
              name="macOS"
              subtext="Coming Soon"
              available={false}
            />
            <PlatformBadge
              icon={
                <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.581 19.049c-.55-.446-.336-1.431-.907-1.917.553-3.365-.997-6.331-2.845-8.232-1.551-1.595-1.051-3.147-1.051-4.9C15.778 1.782 14.53 0 12.004 0c-2.522 0-3.774 1.782-3.774 4c0 1.753.5 3.305-1.051 4.9-1.848 1.9-3.398 4.867-2.845 8.232-.571.486-.357 1.471-.907 1.917C2.45 19.924 2 20.837 2 22h20c0-1.163-.45-2.076-1.419-2.951z"/>
                </svg>
              }
              name="Linux"
              subtext="Coming Soon"
              available={false}
            />
          </div>
        </div>
      </section>

      {/* ── Download CTA ──────────────────────────────────── */}
      <section
        style={{
          padding: '6rem 1.5rem',
          background: '#000',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Glow backdrop */}
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: 500,
            height: 300,
            borderRadius: '50%',
            background: 'radial-gradient(ellipse, rgba(255,90,0,0.1) 0%, transparent 70%)',
            pointerEvents: 'none',
          }}
        />
        <div style={{ position: 'relative', zIndex: 1, maxWidth: 600, margin: '0 auto' }}>
          <h2
            style={{
              fontSize: 'clamp(2rem, 4vw, 3rem)',
              fontWeight: 800,
              letterSpacing: '-0.03em',
              color: '#fff',
              marginBottom: '1rem',
            }}
          >
            Ready to move your data?
          </h2>
          <p style={{ color: 'rgba(255,255,255,0.45)', fontSize: '1rem', marginBottom: '2.5rem' }}>
            Download KnowToMigrate for free. No sign-up. No subscription. Always free.
          </p>
          <Link
            to="/download"
            className="km-btn-primary"
            style={{ fontSize: '1.1rem', padding: '1rem 2.5rem' }}
          >
            Download Free
            <ArrowRight size={18} />
          </Link>
          <p style={{ marginTop: '1.5rem', fontSize: '0.8rem', color: 'rgba(255,255,255,0.25)' }}>
            Open source · MIT License · No telemetry
          </p>
        </div>
      </section>
    </main>
  )
}
