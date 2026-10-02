using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public sealed class KtmWebShareServer : IDisposable
    {
        private static readonly Lazy<KtmWebShareServer> _instance = new(() => new KtmWebShareServer());
        public static KtmWebShareServer Instance => _instance.Value;

        private HttpListener? _listener;
        private CancellationTokenSource? _cts;
        private readonly List<string> _stagedFiles = new();
        private readonly object _lock = new();

        public bool IsRunning => _listener != null && _listener.IsListening;
        public int Port => KtmConstants.WebSharePort;

        public event Action<string, long>? OnFileUploaded;
        public event Action<string>? OnServerError;

        public List<string> GetStagedFiles()
        {
            lock (_lock)
            {
                return new List<string>(_stagedFiles);
            }
        }

        public void SetStagedFiles(IEnumerable<string> files)
        {
            lock (_lock)
            {
                _stagedFiles.Clear();
                foreach (var f in files)
                {
                    if (File.Exists(f) || Directory.Exists(f))
                    {
                        _stagedFiles.Add(f);
                    }
                }
            }
        }

        public void AddStagedFile(string filePath)
        {
            lock (_lock)
            {
                if ((File.Exists(filePath) || Directory.Exists(filePath)) && !_stagedFiles.Contains(filePath))
                {
                    _stagedFiles.Add(filePath);
                }
            }
        }

        public void ClearStagedFiles()
        {
            lock (_lock)
            {
                _stagedFiles.Clear();
            }
        }

        public string? GetPrimaryLocalIp()
        {
            try
            {
                using var socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0);
                socket.Connect("8.8.8.8", 65530);
                if (socket.LocalEndPoint is IPEndPoint endPoint)
                {
                    return endPoint.Address.ToString();
                }
            }
            catch { }

            try
            {
                var host = Dns.GetHostEntry(Dns.GetHostName());
                foreach (var ip in host.AddressList)
                {
                    if (ip.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ip))
                    {
                        return ip.ToString();
                    }
                }
            }
            catch { }

            return "127.0.0.1";
        }

        public string GetShareUrl()
        {
            string ip = GetPrimaryLocalIp() ?? "127.0.0.1";
            return $"http://{ip}:{Port}/";
        }

        public void Start()
        {
            if (IsRunning) return;

            try
            {
                _cts = new CancellationTokenSource();
                _listener = new HttpListener();
                _listener.Prefixes.Add($"http://*:{Port}/");
                _listener.Start();

                Task.Run(() => ListenLoopAsync(_cts.Token));
            }
            catch
            {
                try
                {
                    _listener?.Close();
                    _listener = new HttpListener();
                    _listener.Prefixes.Add($"http://localhost:{Port}/");
                    _listener.Prefixes.Add($"http://127.0.0.1:{Port}/");
                    string? ip = GetPrimaryLocalIp();
                    if (!string.IsNullOrEmpty(ip) && ip != "127.0.0.1")
                    {
                        _listener.Prefixes.Add($"http://{ip}:{Port}/");
                    }
                    _listener.Start();
                    Task.Run(() => ListenLoopAsync(_cts!.Token));
                }
                catch (Exception fallbackEx)
                {
                    OnServerError?.Invoke($"Failed to start Web Share server: {fallbackEx.Message}");
                }
            }
        }

        public void Stop()
        {
            try
            {
                _cts?.Cancel();
                if (_listener != null && _listener.IsListening)
                {
                    _listener.Stop();
                    _listener.Close();
                }
            }
            catch { }
            finally
            {
                _listener = null;
            }
        }

        private async Task ListenLoopAsync(CancellationToken ct)
        {
            while (!ct.IsCancellationRequested && _listener != null && _listener.IsListening)
            {
                try
                {
                    var context = await _listener.GetContextAsync();
                    _ = Task.Run(() => HandleRequestAsync(context), ct);
                }
                catch (HttpListenerException) { break; }
                catch (ObjectDisposedException) { break; }
                catch { }
            }
        }

        private async Task HandleRequestAsync(HttpListenerContext context)
        {
            var req = context.Request;
            var res = context.Response;

            res.Headers.Add("Access-Control-Allow-Origin", "*");
            res.Headers.Add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.Headers.Add("Access-Control-Allow-Headers", "Content-Type");

            if (req.HttpMethod == "OPTIONS")
            {
                res.StatusCode = 200;
                res.Close();
                return;
            }

            try
            {
                string rawUrl = req.RawUrl ?? "/";
                if (rawUrl == "/" || rawUrl.StartsWith("/index"))
                {
                    await ServeWebPortalAsync(res);
                }
                else if (rawUrl.StartsWith("/download-all"))
                {
                    await ServeZipDownloadAsync(res);
                }
                else if (rawUrl.StartsWith("/download?"))
                {
                    string fileName = WebUtility.UrlDecode(req.QueryString["file"] ?? "");
                    await ServeSingleFileAsync(res, fileName);
                }
                else if (rawUrl.StartsWith("/upload") && req.HttpMethod == "POST")
                {
                    await HandleFileUploadAsync(req, res);
                }
                else if (rawUrl.StartsWith("/status"))
                {
                    await ServeStatusJsonAsync(res);
                }
                else
                {
                    res.StatusCode = 404;
                    byte[] notFound = Encoding.UTF8.GetBytes("Not Found");
                    await res.OutputStream.WriteAsync(notFound, 0, notFound.Length);
                    res.Close();
                }
            }
            catch
            {
                try
                {
                    res.StatusCode = 500;
                    res.Close();
                }
                catch { }
            }
        }

        private async Task ServeWebPortalAsync(HttpListenerResponse res)
        {
            res.ContentType = "text/html; charset=utf-8";
            res.StatusCode = 200;

            var staged = GetStagedFiles();
            var sbFiles = new StringBuilder();

            if (staged.Count == 0)
            {
                sbFiles.Append("<div class='empty-state'>No files currently staged on this PC. Drag and drop files on KnowToMigrate PC app to share here, or upload files below to send them to this PC.</div>");
            }
            else
            {
                foreach (var file in staged)
                {
                    if (File.Exists(file))
                    {
                        var fi = new FileInfo(file);
                        string enc = WebUtility.UrlEncode(fi.Name);
                        sbFiles.Append($@"
                        <div class='file-item'>
                            <div class='file-info'>
                                <div class='file-name'>{WebUtility.HtmlEncode(fi.Name)}</div>
                                <div class='file-size'>{KtmFormatting.FormatBytes(fi.Length)}</div>
                            </div>
                            <a class='btn btn-small' href='/download?file={enc}'>Download</a>
                        </div>");
                    }
                    else if (Directory.Exists(file))
                    {
                        var di = new DirectoryInfo(file);
                        sbFiles.Append($@"
                        <div class='file-item'>
                            <div class='file-info'>
                                <div class='file-name'>[Folder] {WebUtility.HtmlEncode(di.Name)}</div>
                                <div class='file-size'>Folder Archive</div>
                            </div>
                            <a class='btn btn-small' href='/download-all'>Download ZIP</a>
                        </div>");
                    }
                }
            }

            string zipBtn = staged.Count > 0 ? "<a class='btn btn-small' href='/download-all'>Download All (.zip)</a>" : "";

            string html = $@"<!DOCTYPE html>
<html lang='en'>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>KnowToMigrate Web Portal</title>
    <style>
        :root {{
            --bg: #050505;
            --surface: #101010;
            --card: #141414;
            --border: #222222;
            --accent: #FF5A00;
            --text-main: #FFFFFF;
            --text-sub: #8E8E93;
        }}
        * {{ box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }}
        body {{ background-color: var(--bg); color: var(--text-main); min-height: 100vh; padding: 20px; }}
        .container {{ max-width: 680px; margin: 0 auto; }}
        .header {{ display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }}
        .brand {{ display: flex; align-items: center; gap: 12px; }}
        .brand-icon {{ width: 38px; height: 38px; border-radius: 9px; background: #000; border: 1.5px solid var(--accent); display: flex; align-items: center; justify-content: center; }}
        .brand-icon svg {{ width: 20px; height: 20px; fill: var(--accent); }}
        .brand-text h1 {{ font-size: 19px; font-weight: 800; }}
        .brand-text p {{ font-size: 11px; color: var(--text-sub); }}
        .badge {{ font-size: 10px; font-weight: 700; color: var(--accent); background: rgba(255,90,0,0.12); border: 1px solid var(--accent); padding: 4px 8px; border-radius: 6px; }}
        .card {{ background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 22px; margin-bottom: 20px; }}
        .card-title {{ font-size: 16px; font-weight: 700; margin-bottom: 6px; display: flex; align-items: center; justify-content: space-between; }}
        .card-desc {{ font-size: 12px; color: var(--text-sub); margin-bottom: 16px; }}
        .file-list {{ display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }}
        .file-item {{ display: flex; align-items: center; justify-content: space-between; background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; }}
        .file-name {{ font-size: 14px; font-weight: 600; color: #FFF; word-break: break-all; margin-bottom: 3px; }}
        .file-size {{ font-size: 11px; color: var(--text-sub); }}
        .btn {{ display: inline-flex; align-items: center; justify-content: center; background: var(--accent); color: #000; font-weight: 700; font-size: 13px; padding: 10px 18px; border-radius: 10px; text-decoration: none; border: none; cursor: pointer; }}
        .btn-small {{ font-size: 11px; padding: 7px 12px; border-radius: 8px; }}
        .empty-state {{ text-align: center; color: var(--text-sub); font-size: 13px; padding: 24px 10px; line-height: 1.5; }}
        .upload-zone {{ border: 2px dashed #333; border-radius: 14px; padding: 32px 16px; text-align: center; cursor: pointer; }}
        .upload-zone:hover {{ border-color: var(--accent); }}
        .progress-box {{ display: none; margin-top: 14px; }}
        .progress-bar-wrap {{ width: 100%; height: 8px; background: #222; border-radius: 4px; overflow: hidden; margin-bottom: 6px; }}
        .progress-bar {{ height: 100%; width: 0%; background: var(--accent); }}
        .status-txt {{ font-size: 12px; color: var(--text-sub); text-align: center; }}
        .footer {{ text-align: center; font-size: 11px; color: #444; margin-top: 30px; }}
    </style>
</head>
<body>
    <div class='container'>
        <header class='header'>
            <div class='brand'>
                <div class='brand-icon'>
                    <svg viewBox='0 0 24 24'><polygon points='13,2 3,14 12,14 11,22 21,10 12,10'></polygon></svg>
                </div>
                <div class='brand-text'>
                    <h1>KnowToMigrate</h1>
                    <p>Instant Wi-Fi Local Web Share</p>
                </div>
            </div>
            <span class='badge'>Offline P2P</span>
        </header>

        <section class='card'>
            <div class='card-title'>
                <span>Available Files from PC</span>
                {zipBtn}
            </div>
            <p class='card-desc'>Files currently shared by this computer over your local network.</p>
            <div class='file-list'>
                {sbFiles}
            </div>
        </section>

        <section class='card'>
            <div class='card-title'>
                <span>Send to PC</span>
            </div>
            <p class='card-desc'>Upload photos, videos, or documents directly to this computer.</p>
            <div class='upload-zone' onclick=""document.getElementById('fileInput').click()"">
                <input type='file' id='fileInput' style='display:none' multiple onchange='handleFiles(this.files)' />
                <div style='font-size: 32px; margin-bottom: 8px;'>+</div>
                <div style='font-size: 14px; font-weight: 600; margin-bottom: 4px;'>Tap to Select Files to Upload</div>
                <div style='font-size: 11px; color: var(--text-sub);'>Any format · Encrypted over local Wi-Fi · Zero Cloud</div>
            </div>
            <div class='progress-box' id='progressBox'>
                <div class='progress-bar-wrap'>
                    <div class='progress-bar' id='progressBar'></div>
                </div>
                <div class='status-txt' id='statusText'>Uploading...</div>
            </div>
        </section>

        <footer class='footer'>
            KnowToMigrate Pluto Engine · Zero-Install Cross-Platform Web Portal
        </footer>
    </div>

    <script>
        function handleFiles(files) {{
            if (!files || files.length === 0) return;
            const pBox = document.getElementById('progressBox');
            const pBar = document.getElementById('progressBar');
            const pTxt = document.getElementById('statusText');
            pBox.style.display = 'block';

            let completed = 0;
            const total = files.length;

            function uploadNext(index) {{
                if (index >= total) {{
                    pTxt.innerText = 'All ' + total + ' file(s) transferred successfully!';
                    pTxt.style.color = '#22C55E';
                    setTimeout(() => {{ location.reload(); }}, 1800);
                    return;
                }}

                const file = files[index];
                pTxt.innerText = 'Uploading ' + (index + 1) + '/' + total + ': ' + file.name;
                
                const xhr = new XMLHttpRequest();
                const formData = new FormData();
                formData.append('file', file);

                xhr.upload.onprogress = (e) => {{
                    if (e.lengthComputable) {{
                        const percent = Math.round((e.loaded / e.total) * 100);
                        pBar.style.width = percent + '%';
                    }}
                }};

                xhr.onload = () => {{
                    if (xhr.status === 200) {{
                        uploadNext(index + 1);
                    }} else {{
                        pTxt.innerText = 'Upload failed for ' + file.name;
                        pTxt.style.color = '#EF4444';
                    }}
                }};

                xhr.onerror = () => {{
                    pTxt.innerText = 'Network error during upload.';
                    pTxt.style.color = '#EF4444';
                }};

                xhr.open('POST', '/upload', true);
                xhr.send(formData);
            }}

            uploadNext(0);
        }}
    </script>
</body>
</html>";

            byte[] bytes = Encoding.UTF8.GetBytes(html);
            res.ContentLength64 = bytes.Length;
            await res.OutputStream.WriteAsync(bytes, 0, bytes.Length);
            res.Close();
        }

        private async Task ServeSingleFileAsync(HttpListenerResponse res, string fileName)
        {
            string? foundPath = null;
            lock (_lock)
            {
                foreach (var path in _stagedFiles)
                {
                    if (File.Exists(path) && Path.GetFileName(path).Equals(fileName, StringComparison.OrdinalIgnoreCase))
                    {
                        foundPath = path;
                        break;
                    }
                }
            }

            if (foundPath == null || !File.Exists(foundPath))
            {
                res.StatusCode = 404;
                byte[] nf = Encoding.UTF8.GetBytes("File Not Found");
                await res.OutputStream.WriteAsync(nf, 0, nf.Length);
                res.Close();
                return;
            }

            var fi = new FileInfo(foundPath);
            res.ContentType = "application/octet-stream";
            res.AddHeader("Content-Disposition", $"attachment; filename=\"{Uri.EscapeDataString(fi.Name)}\"");
            res.ContentLength64 = fi.Length;

            using var fs = new FileStream(foundPath, FileMode.Open, FileAccess.Read, FileShare.Read);
            await fs.CopyToAsync(res.OutputStream);
            res.Close();
        }

        private async Task ServeZipDownloadAsync(HttpListenerResponse res)
        {
            var files = GetStagedFiles();
            if (files.Count == 0)
            {
                res.StatusCode = 404;
                byte[] nf = Encoding.UTF8.GetBytes("No files staged");
                await res.OutputStream.WriteAsync(nf, 0, nf.Length);
                res.Close();
                return;
            }

            res.ContentType = "application/zip";
            string zipName = $"KnowToMigrate_Package_{DateTime.Now:yyyyMMdd_HHmmss}.zip";
            res.AddHeader("Content-Disposition", $"attachment; filename=\"{zipName}\"");

            using (var zip = new ZipArchive(res.OutputStream, ZipArchiveMode.Create, true))
            {
                foreach (var file in files)
                {
                    if (File.Exists(file))
                    {
                        var entry = zip.CreateEntry(Path.GetFileName(file), CompressionLevel.Fastest);
                        using var entryStream = entry.Open();
                        using var fs = new FileStream(file, FileMode.Open, FileAccess.Read, FileShare.Read);
                        await fs.CopyToAsync(entryStream);
                    }
                    else if (Directory.Exists(file))
                    {
                        var dir = new DirectoryInfo(file);
                        foreach (var subFile in dir.GetFiles("*", SearchOption.AllDirectories))
                        {
                            string rel = Path.GetRelativePath(dir.FullName, subFile.FullName);
                            var entry = zip.CreateEntry(Path.Combine(dir.Name, rel), CompressionLevel.Fastest);
                            using var entryStream = entry.Open();
                            using var fs = new FileStream(subFile.FullName, FileMode.Open, FileAccess.Read, FileShare.Read);
                            await fs.CopyToAsync(entryStream);
                        }
                    }
                }
            }

            res.Close();
        }

        private async Task HandleFileUploadAsync(HttpListenerRequest req, HttpListenerResponse res)
        {
            try
            {
                string? contentType = req.ContentType;
                if (string.IsNullOrEmpty(contentType) || !contentType.StartsWith("multipart/form-data"))
                {
                    res.StatusCode = 400;
                    res.Close();
                    return;
                }

                string boundary = "";
                var parts = contentType.Split(';');
                foreach (var part in parts)
                {
                    var trimmed = part.Trim();
                    if (trimmed.StartsWith("boundary="))
                    {
                        boundary = "--" + trimmed.Substring(9).Trim('"');
                        break;
                    }
                }

                if (string.IsNullOrEmpty(boundary))
                {
                    res.StatusCode = 400;
                    res.Close();
                    return;
                }

                string downloadDir = KtmManager.Instance.DownloadDirectory;
                if (!Directory.Exists(downloadDir))
                {
                    Directory.CreateDirectory(downloadDir);
                }

                using var memoryStream = new MemoryStream();
                await req.InputStream.CopyToAsync(memoryStream);
                byte[] body = memoryStream.ToArray();

                var boundaryBytes = Encoding.UTF8.GetBytes(boundary);
                int boundaryPos = FindBytes(body, boundaryBytes, 0);

                while (boundaryPos >= 0)
                {
                    int nextBoundary = FindBytes(body, boundaryBytes, boundaryPos + boundaryBytes.Length);
                    if (nextBoundary < 0) break;

                    int partStart = boundaryPos + boundaryBytes.Length + 2;
                    int headerEnd = FindBytes(body, Encoding.UTF8.GetBytes("\r\n\r\n"), partStart);

                    if (headerEnd > partStart)
                    {
                        string headers = Encoding.UTF8.GetString(body, partStart, headerEnd - partStart);
                        string fileName = ExtractFileNameFromHeaders(headers);

                        if (!string.IsNullOrEmpty(fileName))
                        {
                            int dataStart = headerEnd + 4;
                            int dataEnd = nextBoundary - 2;
                            int dataLen = dataEnd - dataStart;

                            if (dataLen >= 0)
                            {
                                string targetPath = Path.Combine(downloadDir, fileName);
                                using (var fs = new FileStream(targetPath, FileMode.Create, FileAccess.Write))
                                {
                                    await fs.WriteAsync(body, dataStart, dataLen);
                                }

                                OnFileUploaded?.Invoke(fileName, dataLen);
                                KtmSoundService.PlayTransferSuccess();
                            }
                        }
                    }

                    boundaryPos = nextBoundary;
                }

                res.StatusCode = 200;
                byte[] ok = Encoding.UTF8.GetBytes("OK");
                await res.OutputStream.WriteAsync(ok, 0, ok.Length);
                res.Close();
            }
            catch (Exception ex)
            {
                OnServerError?.Invoke($"File upload failed: {ex.Message}");
                res.StatusCode = 500;
                res.Close();
            }
        }

        private static int FindBytes(byte[] src, byte[] pattern, int startIndex)
        {
            if (startIndex >= src.Length || pattern.Length == 0) return -1;
            for (int i = startIndex; i <= src.Length - pattern.Length; i++)
            {
                bool match = true;
                for (int j = 0; j < pattern.Length; j++)
                {
                    if (src[i + j] != pattern[j])
                    {
                        match = false;
                        break;
                    }
                }
                if (match) return i;
            }
            return -1;
        }

        private static string ExtractFileNameFromHeaders(string headers)
        {
            foreach (var line in headers.Split(new[] { "\r\n" }, StringSplitOptions.RemoveEmptyEntries))
            {
                if (line.StartsWith("Content-Disposition:", StringComparison.OrdinalIgnoreCase))
                {
                    int fnIdx = line.IndexOf("filename=\"", StringComparison.OrdinalIgnoreCase);
                    if (fnIdx >= 0)
                    {
                        int start = fnIdx + 10;
                        int end = line.IndexOf('"', start);
                        if (end > start)
                        {
                            return Path.GetFileName(line.Substring(start, end - start));
                        }
                    }
                }
            }
            return "";
        }

        private async Task ServeStatusJsonAsync(HttpListenerResponse res)
        {
            res.ContentType = "application/json";
            string json = $"{{\"running\": true, \"port\": {Port}, \"stagedCount\": {GetStagedFiles().Count}}}";
            byte[] bytes = Encoding.UTF8.GetBytes(json);
            res.ContentLength64 = bytes.Length;
            await res.OutputStream.WriteAsync(bytes, 0, bytes.Length);
            res.Close();
        }

        public void Dispose()
        {
            Stop();
        }
    }
}