use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use std::path::Path;
use std::sync::Arc;
use tokio::sync::Mutex;

/// Callback type for transfer progress updates
pub type ProgressCallback = Arc<dyn Fn(ProgressEvent) + Send + Sync>;

#[derive(Debug, Clone)]
pub struct ProgressEvent {
    pub session_id: String,
    pub file_index: usize,
    pub chunk_index: u64,
    pub bytes_received: u64,
    pub total_bytes: u64,
    pub speed_bps: f64,
    pub eta_secs: f64,
}

/// Incoming transfer request — caller must call `accept()` or `reject()`
#[derive(Debug, Clone)]
pub struct TransferRequest {
    pub session_id: String,
    pub sender_device_id: String,
    pub sender_name: String,
    pub manifest_json: String,
    pub total_files: u64,
    pub total_bytes: u64,
}

pub struct TransferServer {
    port: u16,
    receive_dir: String,
    on_request: Arc<dyn Fn(TransferRequest) -> bool + Send + Sync>,
    on_progress: ProgressCallback,
}

impl TransferServer {
    pub fn new(
        port: u16,
        receive_dir: impl Into<String>,
        on_request: impl Fn(TransferRequest) -> bool + Send + Sync + 'static,
        on_progress: impl Fn(ProgressEvent) + Send + Sync + 'static,
    ) -> Self {
        Self {
            port,
            receive_dir: receive_dir.into(),
            on_request: Arc::new(on_request),
            on_progress: Arc::new(on_progress),
        }
    }

    /// Start listening for incoming connections. Runs until cancelled.
    pub async fn listen(&self) -> std::io::Result<()> {
        let addr = format!("0.0.0.0:{}", self.port);
        let listener = TcpListener::bind(&addr).await?;
        tracing::info!("KTM Transfer Server listening on {}", addr);

        loop {
            let (stream, peer_addr) = listener.accept().await?;
            tracing::info!("Incoming connection from {}", peer_addr);

            let receive_dir = self.receive_dir.clone();
            let on_request = Arc::clone(&self.on_request);
            let on_progress = Arc::clone(&self.on_progress);

            tokio::spawn(async move {
                if let Err(e) = handle_connection(stream, receive_dir, on_request, on_progress).await {
                    tracing::error!("Connection error: {}", e);
                }
            });
        }
    }
}

const CHUNK_SIZE: usize = 8 * 1024 * 1024; // 8MB
const HEADER_MAGIC: &[u8; 4] = b"KTM2";

async fn handle_connection(
    mut stream: TcpStream,
    receive_dir: String,
    on_request: Arc<dyn Fn(TransferRequest) -> bool + Send + Sync>,
    on_progress: ProgressCallback,
) -> std::io::Result<()> {
    use crate::pathguard::PathGuard;

    // Read handshake: 4 magic + 4 manifest_len + manifest JSON
    let mut magic = [0u8; 4];
    stream.read_exact(&mut magic).await?;
    if &magic != HEADER_MAGIC {
        return Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "Bad magic bytes"));
    }

    let mut manifest_len_buf = [0u8; 4];
    stream.read_exact(&mut manifest_len_buf).await?;
    let manifest_len = u32::from_be_bytes(manifest_len_buf) as usize;
    if manifest_len > 1_000_000 {
        return Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "Manifest too large"));
    }

    let mut manifest_bytes = vec![0u8; manifest_len];
    stream.read_exact(&mut manifest_bytes).await?;
    let manifest_json = String::from_utf8(manifest_bytes)
        .map_err(|_| std::io::Error::new(std::io::ErrorKind::InvalidData, "Invalid manifest UTF-8"))?;

    // Parse basic manifest fields
    let manifest: serde_json::Value = serde_json::from_str(&manifest_json)
        .map_err(|_| std::io::Error::new(std::io::ErrorKind::InvalidData, "Invalid manifest JSON"))?;

    let session_id = manifest["session_id"].as_str().unwrap_or("unknown").to_string();
    let total_files = manifest["total_files"].as_u64().unwrap_or(0);
    let total_bytes = manifest["total_size"].as_u64().unwrap_or(0);
    let sender_device_id = manifest["device_id"].as_str().unwrap_or("").to_string();
    let sender_name = manifest["device_name"].as_str().unwrap_or("Unknown Device").to_string();

    let request = TransferRequest {
        session_id: session_id.clone(),
        sender_device_id,
        sender_name,
        manifest_json: manifest_json.clone(),
        total_files,
        total_bytes,
    };

    let accepted = on_request(request);

    // Send accept/reject byte
    stream.write_all(&[if accepted { 0x01 } else { 0x00 }]).await?;

    if !accepted {
        return Ok(());
    }

    let guard = PathGuard::new(&receive_dir);
    let start = std::time::Instant::now();
    let mut total_received = 0u64;

    // Receive file chunks: [1B type][4B file_idx][8B chunk_idx][8B chunk_len][data...]
    loop {
        let mut type_buf = [0u8; 1];
        match stream.read_exact(&mut type_buf).await {
            Ok(_) => {}
            Err(e) if e.kind() == std::io::ErrorKind::UnexpectedEof => break,
            Err(e) => return Err(e),
        }

        match type_buf[0] {
            0x02 => {
                // CHUNK_DATA packet
                let mut header = [0u8; 20]; // file_idx(4) + chunk_idx(8) + chunk_len(8)
                stream.read_exact(&mut header).await?;
                let file_idx = u32::from_be_bytes(header[0..4].try_into().unwrap()) as usize;
                let _chunk_idx = u64::from_be_bytes(header[4..12].try_into().unwrap());
                let chunk_len = u64::from_be_bytes(header[12..20].try_into().unwrap()) as usize;

                if chunk_len > CHUNK_SIZE * 2 {
                    return Err(std::io::Error::new(std::io::ErrorKind::InvalidData, "Chunk too large"));
                }

                let file_path_len_buf = {
                    let mut b = [0u8; 2];
                    stream.read_exact(&mut b).await?;
                    u16::from_be_bytes(b) as usize
                };
                let mut file_path_bytes = vec![0u8; file_path_len_buf];
                stream.read_exact(&mut file_path_bytes).await?;
                let relative_path = String::from_utf8(file_path_bytes)
                    .map_err(|_| std::io::Error::new(std::io::ErrorKind::InvalidData, "Bad path UTF-8"))?;

                let safe_path = guard.resolve(&relative_path)
                    .map_err(|e| std::io::Error::new(std::io::ErrorKind::PermissionDenied, e))?;

                // Ensure parent dir exists
                if let Some(parent) = safe_path.parent() {
                    tokio::fs::create_dir_all(parent).await?;
                }

                let mut chunk_data = vec![0u8; chunk_len];
                stream.read_exact(&mut chunk_data).await?;

                // Append/write chunk to file
                use tokio::fs::OpenOptions;
                let mut file = OpenOptions::new()
                    .create(true)
                    .write(true)
                    .append(true)
                    .open(&safe_path)
                    .await?;
                file.write_all(&chunk_data).await?;

                total_received += chunk_len as u64;

                // Send ACK
                stream.write_all(&[0x03]).await?; // CHUNK_ACK

                // Progress callback
                let elapsed = start.elapsed().as_secs_f64();
                let speed = if elapsed > 0.0 { total_received as f64 / elapsed } else { 0.0 };
                let eta = if speed > 0.0 { (total_bytes - total_received) as f64 / speed } else { 0.0 };

                on_progress(ProgressEvent {
                    session_id: session_id.clone(),
                    file_index: file_idx,
                    chunk_index: _chunk_idx,
                    bytes_received: total_received,
                    total_bytes,
                    speed_bps: speed,
                    eta_secs: eta,
                });
            }
            0x04 => {
                // TRANSFER_COMPLETE
                tracing::info!("Transfer {} complete. {} bytes received.", session_id, total_received);
                break;
            }
            _ => {
                tracing::warn!("Unknown packet type: {:#x}", type_buf[0]);
            }
        }
    }

    Ok(())
}
