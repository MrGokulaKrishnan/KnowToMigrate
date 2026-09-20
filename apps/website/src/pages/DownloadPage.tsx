import { useState } from 'react'
import { Download, Shield, CheckCircle, Monitor, Smartphone, Copy, Check, AlertTriangle, ExternalLink } from 'lucide-react'

interface DownloadEntry {
  filename: string
  version: string
  size: string
  sha256: string
  requirements: string
  downloadUrl: string
}

const WINDOWS_DOWNLOADS: DownloadEntry[] = [
  {
    filename: 'KnowToMigrate-1.0.0-x64.msi',
    version: '1.0.0',
    size: '~28 MB',
    sha256: 'BUILD_PENDING_SHA256_MSI_AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFFFF0000001111112222223333334444',
    requirements: 'Windows 10 / 11, 64-bit',
    downloadUrl: '#',
  },
  {
    filename: 'KnowToMigrate-1.0.0-x64.exe',
    version: '1.0.0',
    size: '~25 MB',
    sha256: 'BUILD_PENDING_SHA256_EXE_AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFFFF0000001111112222223333334444',
    requirements: 'Windows 10 / 11, 64-bit (portable)',
    downloadUrl: '#',
  },
]

const ANDROID_DOWNLOADS: DownloadEntry[] = [
  {
    filename: 'KnowToMigrate-1.0.0.apk',
    version: '1.0.0',
    size: '~18 MB',
    sha256: 'BUILD_PENDING_SHA256_APK_AAAAAABBBBBBCCCCCCDDDDDDEEEEEEFFFFFFFF0000001111112222223333334444',
    requirements: 'Android 8.0 (Oreo) or newer, ARM64/x86_64',
    downloadUrl: '#',
  },
]

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // fallback
    }
  }

  return (
    <button
      onClick={handleCopy}
      title="Copy to clipboard"
      style={{
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        color: copied ? '#FF8A00' : 'rgba(255,255,255,0.3)',
        padding: '2px 4px',
        borderRadius: 4,
        transition: 'color 0.2s',
        display: 'inline-flex',
        alignItems: 'center',
        flexShrink: 0,
      }}
    >
      {copied ? <Check size={14} /> : <Copy size={14} />}
    </button>
  )
}

interface DownloadCardProps {
  entry: DownloadEntry
  platform: 'windows' | 'android'
}

function DownloadCard({ entry, platform }: DownloadCardProps) {
  return (
    <div
      className="km-card"
      style={{ padding: '1.75rem', marginBottom: '1rem' }}
    >
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '1rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.875rem' }}>
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: 12,
              background: 'rgba(255,90,0,0.1)',
              border: '1px solid rgba(255,90,0,0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FF8A00',
              flexShrink: 0,
            }}
          >
            {platform === 'windows' ? <Monitor size={20} /> : <Smartphone size={20} />}
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.975rem', color: '#fff', fontFamily: 'monospace' }}>
              {entry.filename}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.4)', marginTop: 2 }}>
              v{entry.version} · {entry.size}
            </div>
          </div>
        </div>

        <a
          href={entry.downloadUrl}
          className="km-btn-primary"
          style={{ padding: '0.5rem 1.25rem', fontSize: '0.875rem', borderRadius: 10, textDecoration: 'none' }}
          onClick={e => e.preventDefault()}
        >
          <Download size={15} />
          Download
        </a>
      </div>

      {/* Requirements */}
      <div style={{ marginBottom: '1.25rem' }}>
        <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'rgba(255,255,255,0.3)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.35rem' }}>
          Requirements
        </div>
        <div style={{ fontSize: '0.875rem', color: 'rgba(255,255,255,0.6)' }}>{entry.requirements}</div>
      </div>

      {/* SHA-256 */}
      <div
        style={{
          borderRadius: 10,
          background: 'rgba(0,0,0,0.4)',
          border: '1px solid rgba(255,255,255,0.06)',
          padding: '0.875rem 1rem',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            marginBottom: '0.4rem',
          }}
        >
          <Shield size={13} style={{ color: '#FF5A00' }} />
          <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'rgba(255,255,255,0.3)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
            SHA-256 Checksum
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <code
            style={{
              fontSize: '0.72rem',
              color: 'rgba(255,255,255,0.45)',
              fontFamily: 'monospace',
              wordBreak: 'break-all',
              flex: 1,
              lineHeight: 1.5,
            }}
          >
            {entry.sha256}
          </code>
          <CopyButton text={entry.sha256} />
        </div>
      </div>
    </div>
  )
}

function ComingSoonCard({ icon, name, description }: { icon: React.ReactNode; name: string; description: string }) {
  return (
    <div
      style={{
        padding: '1.75rem',
        borderRadius: 20,
        background: 'rgba(255,255,255,0.02)',
        border: '1px solid rgba(255,255,255,0.06)',
        display: 'flex',
        alignItems: 'center',
        gap: '1.25rem',
        flexWrap: 'wrap',
      }}
    >
      <div
        style={{
          width: 48,
          height: 48,
          borderRadius: 14,
          background: 'rgba(255,255,255,0.04)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'rgba(255,255,255,0.2)',
          flexShrink: 0,
        }}
      >
        {icon}
      </div>
      <div style={{ flex: 1 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '0.25rem' }}>
          <span style={{ fontWeight: 700, fontSize: '0.975rem', color: 'rgba(255,255,255,0.35)' }}>{name}</span>
          <span
            style={{
              padding: '0.2rem 0.6rem',
              borderRadius: 20,
              background: 'rgba(255,255,255,0.05)',
              border: '1px solid rgba(255,255,255,0.08)',
              fontSize: '0.7rem',
              fontWeight: 600,
              color: 'rgba(255,255,255,0.3)',
            }}
          >
            Coming Soon
          </span>
        </div>
        <div style={{ fontSize: '0.85rem', color: 'rgba(255,255,255,0.25)' }}>{description}</div>
      </div>
    </div>
  )
}

export function DownloadPage() {
  return (
    <main style={{ paddingTop: 64 }}>
      {/* Page Header */}
      <div
        style={{
          background: 'linear-gradient(180deg, rgba(255,90,0,0.05) 0%, transparent 100%)',
          borderBottom: '1px solid rgba(255,90,0,0.08)',
          padding: '4rem 1.5rem 3rem',
          textAlign: 'center',
        }}
      >
        <div style={{ maxWidth: 700, margin: '0 auto' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              padding: '0.35rem 1rem',
              borderRadius: 20,
              background: 'rgba(255,90,0,0.1)',
              border: '1px solid rgba(255,90,0,0.2)',
              fontSize: '0.78rem',
              fontWeight: 600,
              color: '#FF8A00',
              marginBottom: '1.25rem',
              letterSpacing: '0.04em',
            }}
          >
            <Download size={13} />
            Download Center
          </div>
          <h1
            style={{
              fontSize: 'clamp(2rem, 5vw, 3rem)',
              fontWeight: 800,
              letterSpacing: '-0.03em',
              color: '#fff',
              marginBottom: '1rem',
            }}
          >
            Download{' '}
            <span
              style={{
                background: 'linear-gradient(135deg, #FF8A00, #FF4D00)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
              }}
            >
              KnowToMigrate
            </span>
          </h1>
          <p style={{ color: 'rgba(255,255,255,0.45)', fontSize: '1rem', lineHeight: 1.7 }}>
            Free, open source, and always offline-capable. Pick your platform and start migrating.
          </p>
        </div>
      </div>

      <div style={{ maxWidth: 860, margin: '0 auto', padding: '3rem 1.5rem' }}>

        {/* Windows Section */}
        <section style={{ marginBottom: '3.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: 10,
                background: 'rgba(255,90,0,0.1)',
                border: '1px solid rgba(255,90,0,0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#FF8A00',
              }}
            >
              <Monitor size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#fff', margin: 0 }}>Windows</h2>
              <p style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.4)', margin: 0 }}>Windows 10 / 11 — 64-bit</p>
            </div>
          </div>
          {WINDOWS_DOWNLOADS.map(entry => (
            <DownloadCard key={entry.filename} entry={entry} platform="windows" />
          ))}
        </section>

        {/* Android Section */}
        <section style={{ marginBottom: '3.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: 10,
                background: 'rgba(255,90,0,0.1)',
                border: '1px solid rgba(255,90,0,0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#FF8A00',
              }}
            >
              <Smartphone size={20} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#fff', margin: 0 }}>Android</h2>
              <p style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.4)', margin: 0 }}>Android 8.0 (Oreo) or newer</p>
            </div>
          </div>

          {/* Sideload warning */}
          <div
            style={{
              display: 'flex',
              gap: '0.875rem',
              padding: '1rem 1.25rem',
              borderRadius: 12,
              background: 'rgba(255,180,0,0.05)',
              border: '1px solid rgba(255,180,0,0.18)',
              marginBottom: '1rem',
            }}
          >
            <AlertTriangle size={16} style={{ color: '#FFAA00', flexShrink: 0, marginTop: 2 }} />
            <div style={{ fontSize: '0.85rem', color: 'rgba(255,255,255,0.55)', lineHeight: 1.6 }}>
              <strong style={{ color: '#FFAA00' }}>Sideloading required.</strong> Enable "Install from Unknown Sources" in{' '}
              <em>Settings → Apps → Special app access → Install unknown apps</em> before installing the APK.
            </div>
          </div>

          {ANDROID_DOWNLOADS.map(entry => (
            <DownloadCard key={entry.filename} entry={entry} platform="android" />
          ))}
        </section>

        {/* Coming Soon */}
        <section style={{ marginBottom: '3.5rem' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'rgba(255,255,255,0.4)', marginBottom: '1rem' }}>
            Coming Soon
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <ComingSoonCard
              icon={
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
                </svg>
              }
              name="macOS"
              description="Universal binary — Intel & Apple Silicon (M1/M2/M3). macOS 12 Monterey or newer."
            />
            <ComingSoonCard
              icon={
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.581 19.049c-.55-.446-.336-1.431-.907-1.917.553-3.365-.997-6.331-2.845-8.232-1.551-1.595-1.051-3.147-1.051-4.9C15.778 1.782 14.53 0 12.004 0c-2.522 0-3.774 1.782-3.774 4c0 1.753.5 3.305-1.051 4.9-1.848 1.9-3.398 4.867-2.845 8.232-.571.486-.357 1.471-.907 1.917C2.45 19.924 2 20.837 2 22h20c0-1.163-.45-2.076-1.419-2.951z"/>
                </svg>
              }
              name="Linux"
              description="AppImage, .deb (Ubuntu/Debian), and .rpm (Fedora/RHEL). x86_64 and ARM64."
            />
          </div>
        </section>

        {/* SHA-256 Verification Guide */}
        <section
          style={{
            padding: '2rem',
            borderRadius: 20,
            background: 'rgba(255,90,0,0.04)',
            border: '1px solid rgba(255,90,0,0.12)',
            marginBottom: '3rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', marginBottom: '1rem' }}>
            <Shield size={18} style={{ color: '#FF8A00' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', margin: 0 }}>Verify Your Download (SHA-256)</h3>
          </div>
          <p style={{ fontSize: '0.875rem', color: 'rgba(255,255,255,0.5)', lineHeight: 1.65, marginBottom: '1.25rem' }}>
            Always verify the checksum after downloading to ensure the file was not tampered with.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
            {/* Windows */}
            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#FF8A00', marginBottom: '0.375rem', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Windows (PowerShell)</div>
              <div
                style={{
                  fontFamily: 'monospace',
                  fontSize: '0.8rem',
                  padding: '0.75rem 1rem',
                  borderRadius: 8,
                  background: 'rgba(0,0,0,0.5)',
                  border: '1px solid rgba(255,255,255,0.06)',
                  color: 'rgba(255,255,255,0.6)',
                  wordBreak: 'break-all',
                }}
              >
                Get-FileHash .\KnowToMigrate-1.0.0-x64.msi -Algorithm SHA256
              </div>
            </div>
            {/* macOS/Linux */}
            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#FF8A00', marginBottom: '0.375rem', textTransform: 'uppercase', letterSpacing: '0.06em' }}>macOS / Linux (Terminal)</div>
              <div
                style={{
                  fontFamily: 'monospace',
                  fontSize: '0.8rem',
                  padding: '0.75rem 1rem',
                  borderRadius: 8,
                  background: 'rgba(0,0,0,0.5)',
                  border: '1px solid rgba(255,255,255,0.06)',
                  color: 'rgba(255,255,255,0.6)',
                }}
              >
                sha256sum KnowToMigrate-1.0.0.apk
              </div>
            </div>
          </div>
        </section>

        {/* Release Notes */}
        <section>
          <h2 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#fff', marginBottom: '1.25rem' }}>Release Notes — v1.0.0</h2>
          <div
            className="km-card"
            style={{ padding: '1.75rem' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <span
                style={{
                  padding: '0.25rem 0.625rem',
                  borderRadius: 20,
                  background: 'rgba(255,90,0,0.1)',
                  border: '1px solid rgba(255,90,0,0.2)',
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  color: '#FF8A00',
                }}
              >
                v1.0.0
              </span>
              <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.3)' }}>Initial Release</span>
            </div>
            <ul
              style={{
                listStyle: 'none',
                padding: 0,
                margin: 0,
                display: 'flex',
                flexDirection: 'column',
                gap: '0.625rem',
              }}
            >
              {[
                'Offline peer-to-peer file transfer over local network',
                'AES-256-GCM end-to-end encryption',
                'QR code pairing — no IP addresses needed',
                'SHA-256 integrity verification on every transfer',
                'Resume interrupted transfers',
                'Trusted device memory',
                'Android ↔ Windows cross-platform support',
                'Large file support (100 GB+)',
              ].map(item => (
                <li key={item} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.625rem', fontSize: '0.9rem', color: 'rgba(255,255,255,0.65)' }}>
                  <CheckCircle size={15} style={{ color: '#FF5A00', flexShrink: 0, marginTop: 2 }} />
                  {item}
                </li>
              ))}
            </ul>

            <div style={{ marginTop: '1.25rem', paddingTop: '1.25rem', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
              <a
                href="https://github.com/MrGokulaKrishnan/KnowToMigrate/releases"
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.375rem',
                  fontSize: '0.85rem',
                  color: '#FF8A00',
                  textDecoration: 'none',
                }}
              >
                <ExternalLink size={14} />
                View full release notes on GitHub
              </a>
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}
