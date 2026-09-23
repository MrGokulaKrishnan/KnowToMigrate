import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, Home } from 'lucide-react'
import { useEffect } from 'react'

export function NotFoundPage() {
  const navigate = useNavigate()

  // Update document title
  useEffect(() => {
    document.title = '404 — Page Not Found | KnowToMigrate'
    return () => { document.title = 'KnowToMigrate — Move Anything. Anywhere. Seamlessly.' }
  }, [])

  return (
    <main
      style={{
        minHeight: '80vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '4rem 1.5rem',
        textAlign: 'center',
      }}
    >
      {/* Large 404 */}
      <div
        style={{
          fontSize: 'clamp(5rem, 15vw, 9rem)',
          fontWeight: 800,
          lineHeight: 1,
          background: 'linear-gradient(135deg, rgba(255,90,0,0.35), rgba(255,138,0,0.15))',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          backgroundClip: 'text',
          marginBottom: '1rem',
          letterSpacing: '-0.04em',
        }}
      >
        404
      </div>

      <h1
        style={{
          fontSize: 'clamp(1.25rem, 3vw, 1.75rem)',
          fontWeight: 700,
          color: '#fff',
          marginBottom: '0.75rem',
        }}
      >
        Page not found
      </h1>
      <p
        style={{
          color: 'rgba(255,255,255,0.45)',
          fontSize: '0.9375rem',
          maxWidth: 380,
          lineHeight: 1.65,
          marginBottom: '2.5rem',
        }}
      >
        The page you're looking for doesn't exist or has been moved.
        KnowToMigrate transfers files — not broken links.
      </p>

      <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', justifyContent: 'center' }}>
        <button
          onClick={() => navigate(-1)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.625rem 1.25rem',
            borderRadius: 10,
            fontSize: '0.875rem',
            fontWeight: 600,
            color: 'rgba(255,255,255,0.7)',
            background: 'rgba(255,255,255,0.05)',
            border: '1px solid rgba(255,255,255,0.10)',
            cursor: 'pointer',
            transition: 'background 0.2s, border-color 0.2s',
          }}
        >
          <ArrowLeft size={15} /> Go Back
        </button>
        <Link
          to="/"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.625rem 1.25rem',
            borderRadius: 10,
            fontSize: '0.875rem',
            fontWeight: 600,
            color: '#fff',
            background: 'linear-gradient(135deg, #FF4D00, #FF8A00)',
            textDecoration: 'none',
            boxShadow: '0 4px 16px rgba(255,90,0,0.30)',
          }}
        >
          <Home size={15} /> Home
        </Link>
      </div>
    </main>
  )
}
