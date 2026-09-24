export function SecurityPage() {
  return (
    <main style={{ minHeight: '100vh', background: '#000', padding: '7rem 1.5rem 4rem' }}>
      <div style={{ maxWidth: 760, margin: '0 auto' }}>
        <span style={{ display: 'inline-block', padding: '0.3rem 1rem', borderRadius: 999, fontSize: '0.75rem', fontWeight: 700, letterSpacing: '0.12em', color: '#FF5A00', border: '1px solid rgba(255,90,0,0.30)', background: 'rgba(255,90,0,0.08)', marginBottom: '1.5rem' }}>
          SECURITY
        </span>
        <h1 style={{ fontSize: '2.5rem', fontWeight: 800, color: '#fff', marginBottom: '0.75rem' }}>Security Model</h1>
        <p style={{ color: '#666', marginBottom: '3rem', lineHeight: 1.7 }}>
          This page describes the cryptographic design of KnowToMigrate v1.0 (Public Beta). Because the product is in active development, this document is updated with each significant change.
        </p>

        <Section title="Transport Encryption">
          <p>All data transferred between KnowToMigrate devices is encrypted using <strong>AES-256-GCM</strong>.</p>
          <ul>
            <li><strong>Key derivation:</strong> An ephemeral <strong>X25519 Elliptic-Curve Diffie-Hellman</strong> exchange is performed at session start. Neither device's long-term key is transmitted.</li>
            <li><strong>Session keys:</strong> Derived from the shared ECDH secret for that session only. Discarded after the session ends.</li>
            <li><strong>Nonce:</strong> 96-bit random nonce per AES-GCM encryption call.</li>
            <li><strong>Library:</strong> Rust <code>aes-gcm 0.10</code> crate (RustCrypto)  -  no custom crypto.</li>
          </ul>
        </Section>

        <Section title="File Integrity">
          <p>KnowToMigrate verifies file integrity with a <strong>SHA-256 Merkle tree</strong>:</p>
          <ul>
            <li>Each 8 MB chunk carries a SHA-256 digest in its packet header.</li>
            <li>After the transfer completes, a Merkle root over all chunk digests is computed on both sender and receiver.</li>
            <li>If roots do not match, the transfer is marked failed and the output is not exposed to the user.</li>
            <li>Library: Rust <code>sha2 0.10</code> crate (RustCrypto).</li>
          </ul>
        </Section>

        <Section title="Device Identity">
          <ul>
            <li>Each install generates a persistent <strong>X25519 keypair</strong> stored in the device's local app data directory.</li>
            <li>The public key is shared during pairing. It is not sent to any server.</li>
            <li>There is no certificate authority. Trust is established by direct QR-code or local-network pairing.</li>
          </ul>
        </Section>

        <Section title="Pairing">
          <ul>
            <li><strong>QR pairing:</strong> An ephemeral session token (UUID v4) is encoded in the QR code displayed by the receiver. The token expires after one use or 60 seconds.</li>
            <li><strong>Local discovery:</strong> UDP broadcast on port 54123. Packets include device name, public key fingerprint, and session availability flag. No pairing data is in the broadcast.</li>
            <li><strong>Trusted devices:</strong> A user may mark a device's public key as trusted for auto-accept. This list is stored locally and never synced.</li>
          </ul>
        </Section>

        <Section title="Path Traversal Protection">
          <p>All received file paths are sanitised by the <code>PathGuard</code> module before any file is written:</p>
          <ul>
            <li>Rejects empty paths, paths over 4096 bytes, and paths containing null bytes.</li>
            <li>Strips leading <code>/</code>, <code>../</code>, and Windows drive prefixes.</li>
            <li>All output files are written under the user-chosen receive directory only. Writing outside this directory is impossible regardless of what the sender sends.</li>
          </ul>
        </Section>

        <Section title="What is NOT encrypted">
          <ul>
            <li>UDP discovery broadcast packets (contains device name + public key fingerprint  -  no file data).</li>
            <li>The initial TCP handshake manifest (transfer metadata). Encryption begins on the first chunk packet.</li>
          </ul>
          <p><em>Note: The transfer handshake encryption is on the roadmap for v1.1.</em></p>
        </Section>

        <Section title="Known Limitations (Beta)">
          <ul>
            <li>The web receiver (optional browser-based mode) is not yet implemented. When it is, it will use a separate security model documented here.</li>
            <li>Internet relay fallback (STUN/TURN) is planned but not implemented. Transfers currently require same-network presence.</li>
            <li>No formal third-party audit has been performed. This product is in public beta.</li>
          </ul>
        </Section>

        <Section title="Vulnerability Reporting">
          <p>To report a security issue, open a GitHub issue marked <strong>[Security]</strong> or email the maintainer via the GitHub profile.</p>
          <p><a href="https://github.com/MrGokulaKrishnan/KnowToMigrate" target="_blank" rel="noopener noreferrer" style={{ color: '#FF8A00' }}>https://github.com/MrGokulaKrishnan/KnowToMigrate</a></p>
        </Section>
      </div>
    </main>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={{ marginBottom: '2.5rem' }}>
      <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#fff', marginBottom: '1rem', paddingBottom: '0.5rem', borderBottom: '1px solid rgba(255,255,255,0.07)' }}>{title}</h2>
      <div style={{ color: '#8A8A8A', lineHeight: 1.75, fontSize: '0.9375rem' }}>
        {children}
      </div>
    </section>
  )
}
