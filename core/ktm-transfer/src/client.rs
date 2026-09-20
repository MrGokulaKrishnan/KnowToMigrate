use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use std::path::Path;
use std::time::Instant;
use crate::chunker::Chunker;

const HEADER_MAGIC: &[u8; 4] = b"KTM2";

#[derive(Debug, Clone)]
pub struct TransferClientProgress {
    pub session_id: String,
    pub bytes_sent: u64,
    pub total_bytes: u64,
    pub speed_bps: f64,
    pub eta_secs: f64,
    pub current_file: String,
}

pub struct TransferClient {
    target_ip: String,
    target_port: u16,
    session_id: String,
    device_id: String,
    device_name: String,
}

impl TransferClient {
    pub fn new(
        target_ip: impl Into<String>,
        target_port: u16,
        session_id: impl Into<String>,
        device_id: impl Into<String>,
        device_name: impl Into<String>,
    ) -> Self {
        Self {
            target_ip: target_ip.into(),
            target_port,
            session_id: session_id.into(),
            device_id: device_id.into(),
            device_name: device_name.into(),
        }
    }

    /// Send a list of files to the remote server.
    /// `on_progress` is called after each chunk.
    pub async fn send_files(
        &self,
        files: &[impl AsRef<Path>],
        on_progress: impl Fn(TransferClientProgress),
    ) -> std::io::Result<()> {
        let addr = format!("{}:{}", self.target_ip, self.target_port);
        let mut stream = TcpStream::connect(&addr).await?;

        // Build manifest
        let mut file_entries = Vec::new();
        let mut total_bytes = 0u64;
        for (i, file) in files.iter().enumerate() {
            let path = file.as_ref();
            let meta = tokio::fs::metadata(path).await?;
            total_bytes += meta.len();
            file_entries.push(serde_json::json!({
                "index": i,
                "path": path.file_name().and_then(|n| n.to_str()).unwrap_or("file"),
                "size": meta.len(),
            }));
        }

        let manifest = serde_json::json!({
            "session_id": self.session_id,
            "device_id": self.device_id,
            "device_name": self.device_name,
            "total_files": files.len(),
            "total_size": total_bytes,
            "files": file_entries,
        });
        let manifest_bytes = serde_json::to_vec(&manifest).unwrap();

        // Send handshake
        stream.write_all(HEADER_MAGIC).await?;
        stream.write_all(&(manifest_bytes.len() as u32).to_be_bytes()).await?;
        stream.write_all(&manifest_bytes).await?;

        // Read accept/reject
        let mut accept_buf = [0u8; 1];
        stream.read_exact(&mut accept_buf).await?;
        if accept_buf[0] != 0x01 {
            return Err(std::io::Error::new(
                std::io::ErrorKind::ConnectionRefused,
                "Transfer rejected by remote device",
            ));
        }

        let start = Instant::now();
        let mut total_sent = 0u64;

        // Stream each file in 8MB chunks
        for (file_idx, file_path) in files.iter().enumerate() {
            let path = file_path.as_ref();
            let file_name = path.file_name()
                .and_then(|n| n.to_str())
                .unwrap_or("file")
                .to_string();

            let chunker = Chunker::open(path).await?;
            let mut chunk_stream = chunker.into_stream();

            let mut chunk_idx: u64 = 0;
            while let Some(chunk_result) = {
                use tokio_stream::StreamExt;
                chunk_stream.next().await
            } {
                let chunk = chunk_result?;

                let path_bytes = file_name.as_bytes();
                let path_len = path_bytes.len() as u16;

                // Write CHUNK_DATA packet
                stream.write_all(&[0x02]).await?; // type
                stream.write_all(&(file_idx as u32).to_be_bytes()).await?;
                stream.write_all(&chunk_idx.to_be_bytes()).await?;
                stream.write_all(&(chunk.data.len() as u64).to_be_bytes()).await?;
                stream.write_all(&path_len.to_be_bytes()).await?;
                stream.write_all(path_bytes).await?;
                stream.write_all(&chunk.data).await?;

                // Wait for ACK
                let mut ack = [0u8; 1];
                stream.read_exact(&mut ack).await?;

                total_sent += chunk.data.len() as u64;
                chunk_idx += 1;

                let elapsed = start.elapsed().as_secs_f64();
                let speed = if elapsed > 0.0 { total_sent as f64 / elapsed } else { 0.0 };
                let eta = if speed > 0.0 { (total_bytes - total_sent) as f64 / speed } else { 0.0 };

                on_progress(TransferClientProgress {
                    session_id: self.session_id.clone(),
                    bytes_sent: total_sent,
                    total_bytes,
                    speed_bps: speed,
                    eta_secs: eta,
                    current_file: file_name.clone(),
                });
            }
        }

        // Send TRANSFER_COMPLETE
        stream.write_all(&[0x04]).await?;
        stream.flush().await?;

        Ok(())
    }
}
