export function PrivacyPage() {
  return (
    <main style={{ minHeight: '100vh', background: '#000', padding: '7rem 1.5rem 4rem' }}>
      <div style={{ maxWidth: 760, margin: '0 auto' }}>
        <span style={{ display: 'inline-block', padding: '0.3rem 1rem', borderRadius: 999, fontSize: '0.75rem', fontWeight: 700, letterSpacing: '0.12em', color: '#FF5A00', border: '1px solid rgba(255,90,0,0.30)', background: 'rgba(255,90,0,0.08)', marginBottom: '1.5rem' }}>
          PRIVACY
        </span>
        <h1 style={{ fontSize: '2.5rem', fontWeight: 800, color: '#fff', marginBottom: '0.75rem' }}>Privacy Policy</h1>
        <p style={{ color: '#555', marginBottom: '3rem', fontSize: '0.875rem' }}>Last updated: September 2026</p>

        <PSection title="Summary">
          <p>KnowToMigrate does not collect, store, or transmit any personal data to any server operated by us. All transfers happen directly between the two devices you choose.</p>
        </PSection>

        <PSection title="Data we do NOT collect">
          <ul>
            <li>No account information  -  there is no account system.</li>
            <li>No file content  -  your files never pass through our servers.</li>
            <li>No device identifiers, IP addresses, or location data.</li>
            <li>No analytics or telemetry from the installed app.</li>
            <li>No crash reports (planned: opt-in only, with redacted logs).</li>
          </ul>
        </PSection>

        <PSection title="Data stored locally on your device">
          <ul>
            <li>Your device keypair (X25519 public/private key)  -  used for pairing. Stays on-device.</li>
            <li>Your trusted device list  -  public key fingerprints of devices you've approved. Stays on-device.</li>
            <li>Transfer history and checkpoint files  -  stored in the app's local data directory. Never synced.</li>
            <li>Your settings (device name, receive folder, visibility preference).</li>
          </ul>
        </PSection>

        <PSection title="Network traffic">
          <p>During a local transfer, the following data is sent <em>only between your two devices on your local network</em>:</p>
          <ul>
            <li>UDP discovery beacons (device name, public key fingerprint, port). Broadcast to your local subnet only.</li>
            <li>Encrypted file chunks (AES-256-GCM). Recipient device only.</li>
            <li>Transfer manifest (file names, sizes). Recipient device only.</li>
          </ul>
          <p>None of this traffic is routed through our infrastructure.</p>
        </PSection>

        <PSection title="Website (knowtomigrate.web.app)">
          <p>This marketing website is hosted on Firebase Hosting (Google). Firebase may collect standard web server logs (IP addresses, user agents, request paths) as part of normal hosting infrastructure. We do not use Google Analytics, cookies, or any client-side tracking on this site.</p>
          <p>See <a href="https://firebase.google.com/support/privacy" target="_blank" rel="noopener noreferrer" style={{ color: '#FF8A00' }}>Firebase Privacy</a> for their data practices.</p>
        </PSection>

        <PSection title="Contact">
          <p>Questions about privacy: open an issue on <a href="https://github.com/MrGokulaKrishnan/KnowToMigrate" target="_blank" rel="noopener noreferrer" style={{ color: '#FF8A00' }}>GitHub</a>.</p>
        </PSection>
      </div>
    </main>
  )
}

function PSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={{ marginBottom: '2.5rem' }}>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#fff', marginBottom: '1rem', paddingBottom: '0.5rem', borderBottom: '1px solid rgba(255,255,255,0.07)' }}>{title}</h2>
      <div style={{ color: '#8A8A8A', lineHeight: 1.75, fontSize: '0.9375rem' }}>{children}</div>
    </section>
  )
}
