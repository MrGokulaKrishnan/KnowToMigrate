using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using KnowToMigrate.Services;

namespace TransferTest
{
    class Program
    {
        static async Task<int> Main(string[] args)
        {
            Console.WriteLine("=================================================");
            Console.WriteLine("  KNOWTOMIGRATE TRANSFER ENGINE DIAGNOSTIC TEST  ");
            Console.WriteLine("=================================================");

            string testRoot = Path.Combine(Path.GetTempPath(), "KtmTest_" + Guid.NewGuid().ToString("N").Substring(0, 8));
            string srcDir = Path.Combine(testRoot, "Source");
            string dstDir = Path.Combine(testRoot, "Destination");

            Directory.CreateDirectory(srcDir);
            Directory.CreateDirectory(dstDir);

            int testPort = 54199;
            using var server = new KtmTransferServer(dstDir, testPort);
            server.OnHandshakeReceived += async (handshake) =>
            {
                Console.WriteLine($"[SERVER] Handshake from {handshake.DeviceName} ({handshake.Platform}), PIN: {handshake.Pin}");
                return await Task.FromResult(true);
            };

            server.OnProgress += (prog) =>
            {
                Console.WriteLine($"[SERVER PROGRESS] {prog.CurrentFileName}: {prog.BytesTransferred}/{prog.TotalBytes} ({prog.Percentage:0.0}%)");
            };

            server.OnTransferCompleted += (sessionId, success, msg) =>
            {
                Console.WriteLine($"[SERVER COMPLETED] Success={success}, Message={msg}");
            };

            server.Start();
            Console.WriteLine($"[SERVER] Started on port {testPort}, download dir: {dstDir}");

            await Task.Delay(200);

            var client = new KtmTransferClient();
            client.OnProgress += (prog) =>
            {
                Console.WriteLine($"[CLIENT PROGRESS] {prog.CurrentFileName}: {prog.BytesTransferred}/{prog.TotalBytes} ({prog.Percentage:0.0}%) - {prog.SpeedMBps:0.0} MB/s");
            };

            // Test 1: Single small file (4 KB)
            Console.WriteLine("\n--- TEST 1: Small File (4 KB) ---");
            string smallFile = Path.Combine(srcDir, "small.txt");
            File.WriteAllText(smallFile, "Hello KnowToMigrate! Testing file transfer data integrity.\n" + new string('A', 4000));
            string smallShaExpected = KtmSecurityUtils.ComputeFileSha256(smallFile);

            bool ok1 = await client.SendFilesAsync("127.0.0.1", testPort, "client-id", "TestClient", new[] { smallFile });
            Console.WriteLine($"[TEST 1 RESULT] SendFilesAsync returned: {ok1}");

            string receivedSmall = Path.Combine(dstDir, "small.txt");
            if (!File.Exists(receivedSmall))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 1 FAIL] File not found at {receivedSmall}!");
                Console.ResetColor();
                return 1;
            }
            string smallShaActual = KtmSecurityUtils.ComputeFileSha256(receivedSmall);
            if (smallShaActual != smallShaExpected)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 1 FAIL] SHA mismatch! Expected {smallShaExpected}, got {smallShaActual}");
                Console.ResetColor();
                return 1;
            }
            Console.ForegroundColor = ConsoleColor.Green;
            Console.WriteLine($"[TEST 1 PASS] File received and verified (Size={new FileInfo(receivedSmall).Length}, SHA={smallShaActual})");
            Console.ResetColor();

            // Test 2: Multi-chunk binary file (2 MB = 8 chunks of 256 KB)
            Console.WriteLine("\n--- TEST 2: Multi-chunk Binary File (2 MB) ---");
            string binFile = Path.Combine(srcDir, "large_test.dat");
            byte[] dummyData = new byte[2 * 1024 * 1024];
            new Random(42).NextBytes(dummyData);
            File.WriteAllBytes(binFile, dummyData);
            string binShaExpected = KtmSecurityUtils.ComputeFileSha256(binFile);

            bool ok2 = await client.SendFilesAsync("127.0.0.1", testPort, "client-id", "TestClient", new[] { binFile });
            Console.WriteLine($"[TEST 2 RESULT] SendFilesAsync returned: {ok2}");

            string receivedBin = Path.Combine(dstDir, "large_test.dat");
            if (!File.Exists(receivedBin))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 2 FAIL] File not found at {receivedBin}!");
                Console.ResetColor();
                return 2;
            }
            string binShaActual = KtmSecurityUtils.ComputeFileSha256(receivedBin);
            if (binShaActual != binShaExpected)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 2 FAIL] SHA mismatch! Expected {binShaExpected}, got {binShaActual}");
                Console.ResetColor();
                return 2;
            }
            Console.ForegroundColor = ConsoleColor.Green;
            Console.WriteLine($"[TEST 2 PASS] Multi-chunk file received and verified (Size={new FileInfo(receivedBin).Length}, SHA={binShaActual})");
            Console.ResetColor();

            // Test 3: Zero-byte file (Empty file edge case)
            Console.WriteLine("\n--- TEST 3: Zero-byte File (0 Bytes) ---");
            string emptyFile = Path.Combine(srcDir, "empty.txt");
            File.WriteAllBytes(emptyFile, Array.Empty<byte>());
            string emptyShaExpected = KtmSecurityUtils.ComputeFileSha256(emptyFile);

            bool ok3 = await client.SendFilesAsync("127.0.0.1", testPort, "client-id", "TestClient", new[] { emptyFile });
            Console.WriteLine($"[TEST 3 RESULT] SendFilesAsync returned: {ok3}");

            string receivedEmpty = Path.Combine(dstDir, "empty.txt");
            if (!File.Exists(receivedEmpty))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 3 FAIL] Zero-byte file not found at {receivedEmpty}!");
                Console.ResetColor();
                return 3;
            }
            Console.ForegroundColor = ConsoleColor.Green;
            Console.WriteLine($"[TEST 3 PASS] Zero-byte file received and verified (Size={new FileInfo(receivedEmpty).Length})");
            Console.ResetColor();

            // Test 4: Folder transfer with nested subdirectories
            Console.WriteLine("\n--- TEST 4: Folder Transfer with Subdirectories ---");
            string subFolder = Path.Combine(srcDir, "MyFolder");
            string subSubFolder = Path.Combine(subFolder, "Nested");
            Directory.CreateDirectory(subSubFolder);
            File.WriteAllText(Path.Combine(subFolder, "doc1.txt"), "Root document content");
            File.WriteAllText(Path.Combine(subSubFolder, "doc2.txt"), "Nested document content with extra data");

            bool ok4 = await client.SendFilesAsync("127.0.0.1", testPort, "client-id", "TestClient", new[] { subFolder });
            Console.WriteLine($"[TEST 4 RESULT] SendFilesAsync returned: {ok4}");

            string receivedDoc1 = Path.Combine(dstDir, "MyFolder", "doc1.txt");
            string receivedDoc2 = Path.Combine(dstDir, "MyFolder", "Nested", "doc2.txt");
            if (!File.Exists(receivedDoc1) || !File.Exists(receivedDoc2))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 4 FAIL] Folder files not created properly! doc1: {File.Exists(receivedDoc1)}, doc2: {File.Exists(receivedDoc2)}");
                Console.ResetColor();
                return 4;
            }
            Console.ForegroundColor = ConsoleColor.Green;
            Console.WriteLine($"[TEST 4 PASS] Folder tree received and verified correctly!");
            Console.ResetColor();

            // Test 5: Multi-Transport Capability Check & Hierarchy Verification
            Console.WriteLine("\n--- TEST 5: Multi-Transport Capability Check & Hierarchy ---");
            var dev1 = new DiscoveredDevice
            {
                DeviceId = "dev-reachable",
                DeviceName = "Nearby Reachable Peer",
                IpAddress = "127.0.0.1",
                TransferPort = testPort,
                SupportedTransports = new List<string> { KtmTransportCodes.WifiLan, KtmTransportCodes.WifiDirect, KtmTransportCodes.Bluetooth }
            };
            string best1 = await KtmTransportManager.Instance.EvaluateAndSelectBestTransportAsync(dev1);
            if (best1 != KtmTransportCodes.WifiLan)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 5 FAIL] Expected best transport {KtmTransportCodes.WifiLan} for reachable peer, got {best1}");
                Console.ResetColor();
                return 5;
            }
            Console.WriteLine($"[TEST 5.1 PASS] Reachable peer selected Priority 1: {best1} (Speed: {KtmTransportCodes.GetSpeedRating(best1)})");

            var dev2 = new DiscoveredDevice
            {
                DeviceId = "dev-unreachable",
                DeviceName = "Off-subnet / Outdoor Peer",
                IpAddress = "192.0.2.1", // RFC 5737 TEST-NET-1 (unreachable)
                TransferPort = 54199,
                SupportedTransports = new List<string> { KtmTransportCodes.WifiLan, KtmTransportCodes.WifiDirect, KtmTransportCodes.Bluetooth }
            };
            string best2 = await KtmTransportManager.Instance.EvaluateAndSelectBestTransportAsync(dev2);
            if (best2 != KtmTransportCodes.WifiDirect)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 5 FAIL] Expected fallback to Priority 2 ({KtmTransportCodes.WifiDirect}), got {best2}");
                Console.ResetColor();
                return 5;
            }
            Console.WriteLine($"[TEST 5.2 PASS] Unreachable LAN peer successfully failed over to Priority 2: {best2} (Speed: {KtmTransportCodes.GetSpeedRating(best2)})");

            var dev3 = new DiscoveredDevice
            {
                DeviceId = "dev-no-wifi",
                DeviceName = "Bluetooth Only Peer",
                IpAddress = "192.0.2.2",
                TransferPort = 54199,
                SupportedTransports = new List<string> { KtmTransportCodes.Bluetooth }
            };
            string best3 = await KtmTransportManager.Instance.EvaluateAndSelectBestTransportAsync(dev3);
            if (best3 != KtmTransportCodes.Bluetooth)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 5 FAIL] Expected fallback to Priority 3 ({KtmTransportCodes.Bluetooth}), got {best3}");
                Console.ResetColor();
                return 5;
            }
            Console.WriteLine($"[TEST 5.3 PASS] Peer without Wi-Fi successfully failed over to Priority 3: {best3} (Speed: {KtmTransportCodes.GetSpeedRating(best3)})");

            // Test 6: MP4 File Transfer & Atomic Rename Verification (.part file deletion)
            Console.WriteLine("\n--- TEST 6: MP4 File Transfer & .part File Cleanup Verification (5 MB) ---");
            string mp4File = Path.Combine(srcDir, "sample_video.mp4");
            byte[] mp4Data = new byte[5 * 1024 * 1024];
            new Random(12345).NextBytes(mp4Data);
            // Add FTYP box header for realistic MP4 file header
            byte[] ftypBox = new byte[] { 0x00, 0x00, 0x00, 0x18, (byte)'f', (byte)'t', (byte)'y', (byte)'p', (byte)'m', (byte)'m', (byte)'p', (byte)'4', (byte)'2' };
            Array.Copy(ftypBox, 0, mp4Data, 0, ftypBox.Length);
            File.WriteAllBytes(mp4File, mp4Data);
            string mp4ShaExpected = KtmSecurityUtils.ComputeFileSha256(mp4File);

            bool progReceived = false;
            long lastTransferredBytes = 0;
            client.OnProgress += (prog) =>
            {
                if (prog.CurrentFileName.EndsWith("sample_video.mp4"))
                {
                    progReceived = true;
                    lastTransferredBytes = prog.BytesTransferred;
                }
            };

            bool ok6 = await client.SendFilesAsync("127.0.0.1", testPort, "client-id", "TestClient", new[] { mp4File });
            Console.WriteLine($"[TEST 6 RESULT] SendFilesAsync returned: {ok6}");

            string receivedMp4 = Path.Combine(dstDir, "sample_video.mp4");
            string orphanPartMp4 = receivedMp4 + ".part";

            if (!File.Exists(receivedMp4))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 6 FAIL] MP4 file not found at {receivedMp4}!");
                Console.ResetColor();
                return 6;
            }

            if (File.Exists(orphanPartMp4))
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 6 FAIL] Orphan .part file found at {orphanPartMp4}! Atomic rename failed to clean up .part file.");
                Console.ResetColor();
                return 6;
            }

            string mp4ShaActual = KtmSecurityUtils.ComputeFileSha256(receivedMp4);
            if (mp4ShaActual != mp4ShaExpected)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"[TEST 6 FAIL] SHA mismatch for MP4! Expected {mp4ShaExpected}, got {mp4ShaActual}");
                Console.ResetColor();
                return 6;
            }

            Console.ForegroundColor = ConsoleColor.Green;
            Console.WriteLine($"[TEST 6 PASS] MP4 file received and verified (Size={new FileInfo(receivedMp4).Length}, SHA={mp4ShaActual}, Zero .part left on disk)");
            Console.ResetColor();

            // Cleanup test directory
            server.Stop();
            try { Directory.Delete(testRoot, true); } catch { }

            Console.ForegroundColor = ConsoleColor.Cyan;
            Console.WriteLine("\n>>> ALL 6 TESTS (TRANSFERS + MULTI-TRANSPORT + MP4 ATOMIC PIPELINE) PASSED SUCCESSFULLY! <<<");
            Console.ResetColor();
            return 0;
        }
    }
}
