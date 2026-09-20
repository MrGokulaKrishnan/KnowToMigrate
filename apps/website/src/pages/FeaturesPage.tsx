import { Link } from 'react-router-dom'
import { Shield, Zap, Wifi, Lock, FolderOpen, RefreshCw, QrCode, Users, Globe } from 'lucide-react'

const features = [
  {
    icon: Wifi,
    title: 'Offline Peer-to-Peer',
    description: 'Transfer files directly between devices on the same Wi-Fi network — no internet required. No cloud. No upload. 100% local.',
    badge: 'Core Feature',
  },
  {
    icon: Zap,
    title: 'Smart Device Migration',
    description: 'Move everything from your Android phone to your Windows PC (or vice versa) with the guided 9-step migration wizard. Photos, Videos, Documents, Music, Downloads, WhatsApp — all organized automatically.',
    badge: 'Migration',
  },
  {
    icon: FolderOpen,
    title: 'Large File Support (100 GB+)',
    description: 'KnowToMigrate uses streaming chunked transfer — files are never fully loaded into memory. Transfer a 100 GB video library as easily as a single document.',
    badge: 'Performance',
  },
  {
    icon: RefreshCw,
    title: 'Resume Interrupted Transfers',
    description: 'If your connection drops mid-transfer, KnowToMigrate automatically resumes from where it left off using checkpoint files. Only the missing chunks are re-sent.',
    badge: 'Reliability',
  },
  {
    icon: Shield,
    title: 'SHA-256 Integrity Verification',
    description: 'Every transferred file is verified with a SHA-256 Merkle tree hash. You only see ✓ Integrity Verified after actual cryptographic confirmation.',
    badge: 'Security',
  },
  {
    icon: QrCode,
    title: 'QR Code Pairing',
    description: 'Pair two devices instantly by scanning a QR code — no account, no email, no setup. The QR contains an ephemeral session token that expires after use.',
    badge: 'Usability',
  },
  {
    icon: Users,
    title: 'Trusted Devices',
    description: 'Mark frequently used devices as trusted for instant connection without manual accept — while keeping unknown devices blocked by default.',
    badge: 'Privacy',
  },
  {
    icon: Lock,
    title: 'End-to-End Encryption',
    description: 'All transfers are encrypted with AES-256-GCM using ephemeral X25519 ECDH key exchange. Even if intercepted on your local network, data cannot be read.',
    badge: 'Security',
  },
  {
    icon: Globe,
    title: 'Consistent Cross-Platform Design',
    description: 'The Android app, Windows app, and website all use the identical KnowToMigrate design system — AMOLED black, KM orange gradients, Liquid Glass, same terminology and interaction language.',
    badge: 'Design',
  },
]

export function FeaturesPage() {
  return (
    <main className="min-h-screen bg-black">
      {/* Hero */}
      <section className="py-20 px-4 text-center border-b border-white/5">
        <div className="max-w-3xl mx-auto">
          <span className="inline-block px-4 py-1.5 rounded-full text-xs font-bold tracking-widest text-[#FF5A00] border border-[#FF5A00]/30 bg-[#FF5A00]/10 mb-6">
            FEATURES
          </span>
          <h1 className="text-5xl font-extrabold text-white mb-4">
            Everything you need.{' '}
            <span className="bg-gradient-to-r from-[#FF4D00] to-[#FF8A00] bg-clip-text text-transparent">
              Nothing you don't.
            </span>
          </h1>
          <p className="text-lg text-[#8A8A8A] max-w-2xl mx-auto">
            KnowToMigrate is a standalone application built for one purpose: moving your data between devices, securely and completely offline.
          </p>
        </div>
      </section>

      {/* Feature grid */}
      <section className="py-20 px-4">
        <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((f) => (
            <div
              key={f.title}
              className="rounded-2xl border border-white/[0.08] bg-white/[0.03] p-6 flex flex-col gap-4 hover:border-[#FF5A00]/30 hover:bg-[#FF5A00]/[0.04] transition-all duration-300"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-[#FF5A00]/15 flex items-center justify-center">
                  <f.icon className="w-5 h-5 text-[#FF5A00]" strokeWidth={1.75} />
                </div>
                <span className="text-xs font-bold text-[#FF5A00]/70 tracking-wider">{f.badge}</span>
              </div>
              <h3 className="text-lg font-semibold text-white">{f.title}</h3>
              <p className="text-sm text-[#8A8A8A] leading-relaxed">{f.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-4 text-center border-t border-white/5">
        <h2 className="text-3xl font-bold text-white mb-6">Ready to move your data?</h2>
        <Link
          to="/download"
          className="inline-flex items-center gap-2 px-8 py-4 rounded-xl font-semibold text-white"
          style={{ background: 'linear-gradient(135deg, #FF4D00, #FF8A00)' }}
        >
          Download KnowToMigrate
        </Link>
      </section>
    </main>
  )
}
