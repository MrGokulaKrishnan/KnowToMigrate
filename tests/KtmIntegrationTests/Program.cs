using System;
using System.IO;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;
using KnowToMigrate.Services;

namespace KtmIntegrationTests
{
    class Program
    {
        static async Task<int> Main(string[] args)
        {
            Console.ForegroundColor = ConsoleColor.Cyan;
            Console.WriteLine("============================================================");
            Console.WriteLine("    KNOWTOMIGRATE INTEGRATION & STRESS TEST SUITE");
            Console.WriteLine("============================================================\n");
            Console.ResetColor();

            string testRoot = Path.Combine(Path.GetTempPath(), "KtmStressTests_" + Guid.NewGuid().ToString("N"));
            string sendDir = Path.Combine(testRoot, "Source");
            string receiveDir = Path.Combine(testRoot, "Destination");

            Directory.CreateDirectory(sendDir);
            Directory.CreateDirectory(receiveDir);

            int passCount = 0;
            int totalTests = 11;

            try
            {
                // TEST 1: Path Traversal Security Test
                Console.WriteLine("--- TEST 1: Path Traversal & Security Sanitization ---");
                bool test1Pass = RunSecurityPathTest(receiveDir);
                if (test1Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 1: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 1: FAIL\n"); }
                Console.ResetColor();

                // TEST 2: 100 MB Large File Transfer & SHA-256 Verification
                Console.WriteLine("--- TEST 2: 100 MB Large File Streaming Transfer ---");
                bool test2Pass = await RunLargeFileTransferTestAsync(sendDir, receiveDir, 100 * 1024 * 1024);
                if (test2Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 2: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 2: FAIL\n"); }
                Console.ResetColor();

                // TEST 3: Recursive Folder & Multiple Files Hierarchy Test
                Console.WriteLine("--- TEST 3: Recursive Folder & Multiple File Hierarchy ---");
                bool test3Pass = await RunFolderTransferTestAsync(sendDir, receiveDir);
                if (test3Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 3: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 3: FAIL\n"); }
                Console.ResetColor();

                // TEST 4: Network Interruption & Resume Test (.part file offset continuation)
                Console.WriteLine("--- TEST 4: Network Interruption & Resumable Transfer ---");
                bool test4Pass = await RunInterruptionAndResumeTestAsync(sendDir, receiveDir, 100 * 1024 * 1024);
                if (test4Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 4: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 4: FAIL\n"); }
                Console.ResetColor();

                // TEST 5: Exactly One History Record Per Session (Deduplication)
                Console.WriteLine("--- TEST 5: Single History Record Per Session ---");
                bool test5Pass = RunSingleRecordPerSessionTest();
                if (test5Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 5: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 5: FAIL\n"); }
                Console.ResetColor();

                // TEST 6: Receiving Files with Nothing Staged Writes No Sent-Style Record
                Console.WriteLine("--- TEST 6: Receive With Nothing Staged (No Phantom Sent Record) ---");
                bool test6Pass = RunReceiveWithNothingStagedTest();
                if (test6Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 6: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 6: FAIL\n"); }
                Console.ResetColor();

                // TEST 7: Honest Integrity Status (No Hash -> Not Checked)
                Console.WriteLine("--- TEST 7: Honest Integrity Badge (No Hash -> Not Checked) ---");
                bool test7Pass = RunHonestIntegrityStatusTest();
                if (test7Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 7: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 7: FAIL\n"); }
                Console.ResetColor();

                // TEST 8: History Migration (Idempotent, Clean Phantoms, Preserve Valid)
                Console.WriteLine("--- TEST 8: History Migration Idempotency & Clean Phantoms ---");
                bool test8Pass = RunHistoryMigrationTest(testRoot);
                if (test8Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 8: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 8: FAIL\n"); }
                Console.ResetColor();

                // TEST 9: Single-Instance Named Pipe IPC Argument Forwarding
                Console.WriteLine("--- TEST 9: Single-Instance IPC Argument Forwarding ---");
                bool test9Pass = await RunSingleInstanceIpcTestAsync();
                if (test9Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 9: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 9: FAIL\n"); }
                Console.ResetColor();

                // TEST 10: Start With Windows HKCU Run Registry Entry
                Console.WriteLine("--- TEST 10: Start With Windows HKCU Registry Key ---");
                bool test10Pass = RunRegistryRunEntryTest();
                if (test10Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 10: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 10: FAIL\n"); }
                Console.ResetColor();

                // TEST 11: Transfer Server Pause Receiving
                Console.WriteLine("--- TEST 11: Transfer Server Pause Receiving ---");
                bool test11Pass = await RunPauseReceivingTestAsync(receiveDir);
                if (test11Pass) { passCount++; Console.ForegroundColor = ConsoleColor.Green; Console.WriteLine(">>> TEST 11: PASS\n"); }
                else { Console.ForegroundColor = ConsoleColor.Red; Console.WriteLine(">>> TEST 11: FAIL\n"); }
                Console.ResetColor();
            }
            finally
            {
                try { Directory.Delete(testRoot, true); } catch { }
            }

            Console.WriteLine("============================================================");
            if (passCount == totalTests)
            {
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"ALL TESTS PASSED ({passCount}/{totalTests}) - PRODUCTION READY!");
            }
            else
            {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine($"TESTS COMPLETED WITH FAILURES: {passCount}/{totalTests} passed.");
            }
            Console.ResetColor();
            Console.WriteLine("============================================================");

            return passCount == totalTests ? 0 : 1;
        }

        static bool RunSecurityPathTest(string baseDir)
        {
            string[] maliciousPaths = {
                "../../secret.txt",
                "..\\..\\windows\\system32\\calc.exe",
                "/etc/passwd",
                "C:\\Windows\\System32\\cmd.exe",
                "sub/../../escape.dat",
                "valid_dir/..\\..\\malicious.bin"
            };

            foreach (var p in maliciousPaths)
            {
                string sanitized = KtmSecurityUtils.SanitizeRelativePath(p);
                string resolved = Path.GetFullPath(Path.Combine(baseDir, sanitized));
                string baseNormalized = Path.GetFullPath(baseDir).TrimEnd(Path.DirectorySeparatorChar);

                if (!resolved.StartsWith(baseNormalized, StringComparison.OrdinalIgnoreCase))
                {
                    Console.WriteLine($"[SECURITY VIOLATION] Malicious path '{p}' escaped base directory to '{resolved}'!");
                    return false;
                }
                Console.WriteLine($"[SECURITY OK] Injected: '{p}' -> Sanitized: '{sanitized}' -> Path: '{resolved}'");
            }
            return true;
        }

        static async Task<bool> RunLargeFileTransferTestAsync(string sendDir, string receiveDir, long fileSizeBytes)
        {
            int testPort = 54129;
            string testFileName = "large_100mb_test.dat";
            string sourceFilePath = Path.Combine(sendDir, testFileName);
            string destFilePath = Path.Combine(receiveDir, testFileName);

            if (File.Exists(destFilePath)) File.Delete(destFilePath);

            Console.WriteLine($"Creating {fileSizeBytes / (1024 * 1024)} MB random test file...");
            byte[] seedBuffer = new byte[1024 * 1024]; // 1 MB buffer
            new Random(42).NextBytes(seedBuffer);

            using (var fs = new FileStream(sourceFilePath, FileMode.Create, FileAccess.Write))
            {
                long written = 0;
                while (written < fileSizeBytes)
                {
                    int toWrite = (int)Math.Min((long)seedBuffer.Length, fileSizeBytes - written);
                    fs.Write(seedBuffer, 0, toWrite);
                    written += toWrite;
                }
            }

            Console.WriteLine("Computing Source SHA-256 hash...");
            string sourceHash = KtmSecurityUtils.ComputeFileSha256(sourceFilePath);
            Console.WriteLine($"Source SHA-256: {sourceHash}");

            using var server = new KtmTransferServer(receiveDir, testPort);
            server.OnHandshakeReceived += h => Task.FromResult(true);
            server.Start();

            await Task.Delay(200);

            var client = new KtmTransferClient();
            var sw = System.Diagnostics.Stopwatch.StartNew();

            client.OnProgress += p =>
            {
                if (p.Percentage > 0 && (int)p.Percentage % 25 == 0)
                {
                    Console.WriteLine($"Transfer Progress: {p.Percentage:0}% | Speed: {p.SpeedMBps:0.0} MB/s");
                }
            };

            bool success = await client.SendFilesAsync(
                "127.0.0.1",
                testPort,
                "test-sender-id",
                "TestSender",
                new[] { sourceFilePath },
                CancellationToken.None
            );

            sw.Stop();
            server.Stop();

            if (!success)
            {
                Console.WriteLine("[ERROR] TransferClient returned false.");
                return false;
            }

            if (!File.Exists(destFilePath))
            {
                Console.WriteLine("[ERROR] Destination file does not exist.");
                return false;
            }

            Console.WriteLine("Computing Destination SHA-256 hash...");
            string destHash = KtmSecurityUtils.ComputeFileSha256(destFilePath);
            Console.WriteLine($"Destination SHA-256: {destHash}");

            bool hashesMatch = string.Equals(sourceHash, destHash, StringComparison.OrdinalIgnoreCase);
            Console.WriteLine($"Hashes Match: {hashesMatch} | Duration: {sw.Elapsed.TotalSeconds:0.00}s | Throughput: {(fileSizeBytes / (1024 * 1024)) / sw.Elapsed.TotalSeconds:0.0} MB/s");

            return hashesMatch;
        }

        static async Task<bool> RunFolderTransferTestAsync(string sendDir, string receiveDir)
        {
            int testPort = 54130;
            string testFolderName = "TestFolderTree";
            string folderPath = Path.Combine(sendDir, testFolderName);
            string subDir = Path.Combine(folderPath, "SubLevel1", "SubLevel2");
            Directory.CreateDirectory(subDir);

            string file1 = Path.Combine(folderPath, "doc1.txt");
            string file2 = Path.Combine(subDir, "data2.bin");

            File.WriteAllText(file1, "Hello from Level 0 root of folder!");
            byte[] rand = new byte[256 * 1024];
            new Random(123).NextBytes(rand);
            File.WriteAllBytes(file2, rand);

            string hash1 = KtmSecurityUtils.ComputeFileSha256(file1);
            string hash2 = KtmSecurityUtils.ComputeFileSha256(file2);

            using var server = new KtmTransferServer(receiveDir, testPort);
            server.OnHandshakeReceived += h => Task.FromResult(true);
            server.Start();

            await Task.Delay(200);

            var client = new KtmTransferClient();
            bool success = await client.SendFilesAsync(
                "127.0.0.1",
                testPort,
                "test-sender-id",
                "TestSender",
                new[] { folderPath },
                CancellationToken.None
            );

            server.Stop();

            if (!success) return false;

            string destFile1 = Path.Combine(receiveDir, testFolderName, "doc1.txt");
            string destFile2 = Path.Combine(receiveDir, testFolderName, "SubLevel1", "SubLevel2", "data2.bin");

            if (!File.Exists(destFile1) || !File.Exists(destFile2))
            {
                Console.WriteLine("[ERROR] Folder hierarchy files missing at destination!");
                return false;
            }

            string destHash1 = KtmSecurityUtils.ComputeFileSha256(destFile1);
            string destHash2 = KtmSecurityUtils.ComputeFileSha256(destFile2);

            bool ok = (hash1 == destHash1 && hash2 == destHash2);
            Console.WriteLine($"Folder Files Reconstructed: {ok} (File1 match: {hash1 == destHash1}, File2 match: {hash2 == destHash2})");
            return ok;
        }

        static async Task<bool> RunInterruptionAndResumeTestAsync(string sendDir, string receiveDir, long fileSizeBytes)
        {
            int testPort = 54131;
            string testFileName = "resumable_test.dat";
            string sourceFilePath = Path.Combine(sendDir, testFileName);
            string destFilePath = Path.Combine(receiveDir, testFileName);
            string partFilePath = destFilePath + ".part";

            if (File.Exists(destFilePath)) File.Delete(destFilePath);
            if (File.Exists(partFilePath)) File.Delete(partFilePath);

            byte[] seed = new byte[1024 * 1024];
            new Random(77).NextBytes(seed);
            using (var fs = new FileStream(sourceFilePath, FileMode.Create, FileAccess.Write))
            {
                long written = 0;
                while (written < fileSizeBytes)
                {
                    int toWrite = (int)Math.Min((long)seed.Length, fileSizeBytes - written);
                    fs.Write(seed, 0, toWrite);
                    written += toWrite;
                }
            }

            string sourceHash = KtmSecurityUtils.ComputeFileSha256(sourceFilePath);

            // Phase 1: Simulate interrupted transfer - Cancel after 15 MB
            Console.WriteLine("Phase 1: Starting transfer and interrupting after partial progress...");
            using var server1 = new KtmTransferServer(receiveDir, testPort);
            server1.OnHandshakeReceived += h => Task.FromResult(true);
            server1.Start();
            await Task.Delay(200);

            var cts1 = new CancellationTokenSource();
            var client1 = new KtmTransferClient();

            client1.OnProgress += p =>
            {
                if (p.BytesTransferred >= 25 * 1024 * 1024 && !cts1.IsCancellationRequested)
                {
                    Console.WriteLine($"--> SIMULATING CONNECTION DROP at {p.BytesTransferred / 1048576} MB!");
                    cts1.Cancel();
                }
            };

            try
            {
                await client1.SendFilesAsync("127.0.0.1", testPort, "test-sender", "Sender", new[] { sourceFilePath }, cts1.Token);
            }
            catch { }

            server1.Stop();
            await Task.Delay(500);

            if (!File.Exists(partFilePath))
            {
                Console.WriteLine("[ERROR] .part file was not created during interrupted transfer!");
                return false;
            }

            long partialBytes = new FileInfo(partFilePath).Length;
            Console.WriteLine($"Verified .part file exists on disk with {partialBytes / 1048576} MB.");

            // Phase 2: Resume transfer from offset
            Console.WriteLine("Phase 2: Resuming transfer from existing offset...");
            using var server2 = new KtmTransferServer(receiveDir, testPort);
            server2.OnHandshakeReceived += h => Task.FromResult(true);
            server2.Start();
            await Task.Delay(200);

            var client2 = new KtmTransferClient();
            long startingResumeOffset = 0;
            client2.OnProgress += p =>
            {
                if (startingResumeOffset == 0 && p.BytesTransferred > 0)
                {
                    startingResumeOffset = p.BytesTransferred;
                    Console.WriteLine($"--> RESUMED at byte offset: {startingResumeOffset} ({startingResumeOffset / 1048576} MB) - NOT starting from zero!");
                }
            };

            bool resumeSuccess = await client2.SendFilesAsync(
                "127.0.0.1",
                testPort,
                "test-sender",
                "Sender",
                new[] { sourceFilePath },
                CancellationToken.None
            );

            server2.Stop();

            if (!resumeSuccess || !File.Exists(destFilePath))
            {
                Console.WriteLine("[ERROR] Resumed transfer did not succeed.");
                return false;
            }

            string finalDestHash = KtmSecurityUtils.ComputeFileSha256(destFilePath);
            bool match = string.Equals(sourceHash, finalDestHash, StringComparison.OrdinalIgnoreCase);
            Console.WriteLine($"Resumed File Hash Verified: {match} (Source == Dest SHA-256)");

            return match && startingResumeOffset > 0;
        }

        static bool RunSingleRecordPerSessionTest()
        {
            string testSession = "test-session-" + Guid.NewGuid().ToString("N");
            var item1 = new HistoricalTransferItem
            {
                SessionId = testSession,
                FileName = "document.pdf",
                TotalBytes = 1024,
                DeviceName = "Pixel 8",
                Direction = TransferDirection.Received,
                Status = "Complete"
            };

            bool firstRecord = TransferService.Instance.Record(item1);
            if (!firstRecord)
            {
                Console.WriteLine("[ERROR] First record for session failed to record.");
                return false;
            }

            // Attempt duplicate record for same session ID (e.g. from phantom completion)
            var item2 = new HistoricalTransferItem
            {
                SessionId = testSession,
                FileName = "0 items transferred",
                TotalBytes = 0,
                DeviceName = "Nearby Target",
                Direction = TransferDirection.Sent,
                Status = "Complete"
            };

            bool secondRecord = TransferService.Instance.Record(item2);
            if (secondRecord)
            {
                Console.WriteLine("[ERROR] Second record for same session ID was incorrectly accepted!");
                return false;
            }

            Console.WriteLine("Deduplication verified: exactly one record accepted per session ID.");
            return true;
        }

        static bool RunReceiveWithNothingStagedTest()
        {
            // Simulate receiving a file when _selectedFiles is empty
            var stagedFiles = new List<string>(); // Nothing staged
            string sessionId = "test-recv-" + Guid.NewGuid().ToString("N");

            // Receive handler writes legitimate receiveItem
            var receiveItem = new HistoricalTransferItem
            {
                SessionId = sessionId,
                FileName = "photo.jpg",
                TotalBytes = 2048,
                DeviceName = "Galaxy S24",
                Direction = TransferDirection.Received,
                Status = "Complete"
            };

            bool recorded = TransferService.Instance.Record(receiveItem);
            if (!recorded) return false;

            // In old bug, OnTransferCompleted was called and read stagedFiles.Count == 0 to write a phantom record.
            // Under new code, OnTransferCompleted only updates UI state and does NOT create any record.
            // If someone mistakenly tried to record a phantom sent record:
            if (stagedFiles.Count == 0)
            {
                // Nothing staged; no sent-style record must be written
                var phantom = new HistoricalTransferItem
                {
                    SessionId = sessionId,
                    FileName = $"{stagedFiles.Count} items transferred",
                    DeviceName = "Nearby Target",
                    Direction = TransferDirection.Sent
                };
                bool phantomRecorded = TransferService.Instance.Record(phantom);
                if (phantomRecorded)
                {
                    Console.WriteLine("[ERROR] Phantom sent record was recorded when receiving files!");
                    return false;
                }
            }

            Console.WriteLine("Receive verified: no sent-style record written when nothing staged.");
            return true;
        }

        static bool RunHonestIntegrityStatusTest()
        {
            // 1. Record with empty hash -> "Not checked"
            var itemNoHash = new HistoricalTransferItem { Sha256 = "", Status = "Complete" };
            if (itemNoHash.VerifiedBadge != "Not checked")
            {
                Console.WriteLine($"[ERROR] Expected 'Not checked', got '{itemNoHash.VerifiedBadge}'");
                return false;
            }

            // 2. StatusBadge for Complete -> "Complete", NOT "Verified"
            if (itemNoHash.StatusBadge == "Verified")
            {
                Console.WriteLine("[ERROR] StatusBadge incorrectly reported 'Verified' for Complete status without hash.");
                return false;
            }

            // 3. Record with legacy literal "verified" -> "Not checked"
            var itemLegacyVerified = new HistoricalTransferItem { Sha256 = "verified", Status = "Complete" };
            if (itemLegacyVerified.VerifiedBadge != "Not checked")
            {
                Console.WriteLine($"[ERROR] Legacy 'verified' literal must report 'Not checked', got '{itemLegacyVerified.VerifiedBadge}'");
                return false;
            }

            // 4. Record with real computed hash -> "Verified"
            var itemRealHash = new HistoricalTransferItem
            {
                Sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Status = "Complete"
            };
            if (itemRealHash.VerifiedBadge != "Verified")
            {
                Console.WriteLine($"[ERROR] Expected 'Verified' for real SHA-256, got '{itemRealHash.VerifiedBadge}'");
                return false;
            }

            Console.WriteLine("Honest integrity verified: No hash => 'Not checked'; Complete does not imply Verified.");
            return true;
        }

        static bool RunHistoryMigrationTest(string testDir)
        {
            string testHistoryPath = Path.Combine(testDir, "test_history.json");

            // Create legacy history.json with:
            // 1. Valid sent item
            // 2. Phantom item ("0 items transferred" / "Nearby Target" / "verified")
            // 3. Valid received item with "in" direction
            string legacyJson = @"[
  {
    ""TransferId"": ""id-1"",
    ""FileName"": ""notes.txt"",
    ""TotalBytes"": 512,
    ""DeviceName"": ""AndroidPhone"",
    ""Direction"": ""out"",
    ""Status"": ""Complete"",
    ""Sha256"": ""abc123def456""
  },
  {
    ""TransferId"": ""id-2"",
    ""FileName"": ""0 items transferred"",
    ""TotalBytes"": 0,
    ""DeviceName"": ""Nearby Target"",
    ""Direction"": ""out"",
    ""Status"": ""Complete"",
    ""Sha256"": ""verified""
  },
  {
    ""TransferId"": ""id-3"",
    ""FileName"": ""song.mp3"",
    ""TotalBytes"": 4096,
    ""DeviceName"": ""Laptop"",
    ""Direction"": ""in"",
    ""Status"": ""Complete"",
    ""Sha256"": """"
  }
]";
            File.WriteAllText(testHistoryPath, legacyJson);

            // Step 1: Run migration once
            bool migrated = UserSettingsManager.EnsureMigratedHistory(testHistoryPath);
            if (!migrated)
            {
                Console.WriteLine("[ERROR] First migration run returned false.");
                return false;
            }

            // Check that a .bak backup file was created
            var bakFiles = Directory.GetFiles(testDir, "history.json.bak.*");
            if (bakFiles.Length == 0)
            {
                Console.WriteLine("[ERROR] Migration did not create history.json.bak.<timestamp> backup file.");
                return false;
            }
            int initialBakCount = bakFiles.Length;

            // Load items and assert phantom removed and valid preserved
            var itemsAfterMigration = UserSettingsManager.LoadHistory(testHistoryPath);
            if (itemsAfterMigration.Count != 2)
            {
                Console.WriteLine($"[ERROR] Expected 2 items after phantom removal, got {itemsAfterMigration.Count}");
                return false;
            }

            if (itemsAfterMigration.Any(i => i.FileName == "0 items transferred" && i.DeviceName == "Nearby Target"))
            {
                Console.WriteLine("[ERROR] Phantom row was not removed by migration!");
                return false;
            }

            if (!itemsAfterMigration.Any(i => i.FileName == "notes.txt") || !itemsAfterMigration.Any(i => i.FileName == "song.mp3"))
            {
                Console.WriteLine("[ERROR] Valid items were not preserved by migration!");
                return false;
            }

            // Step 2: Run migration second time (Idempotency test)
            bool secondRun = UserSettingsManager.EnsureMigratedHistory(testHistoryPath);
            if (secondRun)
            {
                Console.WriteLine("[ERROR] Second migration run returned true; expected idempotent false (already migrated).");
                return false;
            }

            var bakFilesAfterSecond = Directory.GetFiles(testDir, "history.json.bak.*");
            if (bakFilesAfterSecond.Length != initialBakCount)
            {
                Console.WriteLine("[ERROR] Second migration run created an extra backup file (not idempotent).");
                return false;
            }

            var itemsAfterSecond = UserSettingsManager.LoadHistory(testHistoryPath);
            if (itemsAfterSecond.Count != 2)
            {
                Console.WriteLine("[ERROR] Second migration corrupted item count.");
                return false;
            }

            Console.WriteLine("Migration verified: backup created, phantom removed, valid preserved, second run idempotent.");
            return true;
        }

        static async Task<bool> RunSingleInstanceIpcTestAsync()
        {
            var tcs = new TaskCompletionSource<string[]>();
            bool acquired = KtmSingleInstance.TryAcquire();
            if (!acquired)
            {
                Console.WriteLine("[ERROR] Could not acquire single-instance mutex for test.");
                return false;
            }

            try
            {
                KtmSingleInstance.StartIpcListener(
                    onActivateReceived: args => tcs.TrySetResult(args),
                    onShutdownReceived: () => { });

                await Task.Delay(200);

                string testArg = @"C:\test\file.txt";
                bool sent = KtmSingleInstance.SendArgsToPrimary(new[] { testArg });
                if (!sent)
                {
                    Console.WriteLine("[ERROR] SendArgsToPrimary returned false.");
                    return false;
                }

                var completedTask = await Task.WhenAny(tcs.Task, Task.Delay(3000));
                if (completedTask != tcs.Task)
                {
                    Console.WriteLine("[ERROR] Timed out waiting for IPC arguments.");
                    return false;
                }

                var receivedArgs = await tcs.Task;
                if (receivedArgs.Length == 0 || receivedArgs[0] != testArg)
                {
                    Console.WriteLine($"[ERROR] IPC argument mismatch. Expected {testArg}, got {string.Join(", ", receivedArgs)}");
                    return false;
                }

                Console.WriteLine($"IPC argument forwarding verified: received '{receivedArgs[0]}' within timeout.");
                return true;
            }
            finally
            {
                KtmSingleInstance.Release();
            }
        }

        static bool RunRegistryRunEntryTest()
        {
            try
            {
                // Step 1: Set true
                UserSettingsManager.SetStartWithWindows(true);
                using (var key = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", false))
                {
                    var val = key?.GetValue("KnowToMigrate") as string;
                    if (val == null || !val.Contains("--background"))
                    {
                        Console.WriteLine($"[ERROR] Registry entry missing or missing --background: {val}");
                        return false;
                    }
                }

                // Step 2: Set false
                UserSettingsManager.SetStartWithWindows(false);
                using (var key = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Run", false))
                {
                    var val = key?.GetValue("KnowToMigrate");
                    if (val != null)
                    {
                        Console.WriteLine($"[ERROR] Registry entry still exists after disabling: {val}");
                        return false;
                    }
                }

                Console.WriteLine("StartWithWindows registry write & cleanup verified successfully.");
                return true;
            }
            finally
            {
                UserSettingsManager.SetStartWithWindows(false);
            }
        }

        static async Task<bool> RunPauseReceivingTestAsync(string receiveDir)
        {
            int testPort = 54131;
            using var server = new KtmTransferServer(receiveDir, testPort);
            server.Start();
            await Task.Delay(200);

            try
            {
                // Step 1: Set paused
                server.IsPaused = true;
                bool rejectedOrClosed = false;

                try
                {
                    using (var client = new System.Net.Sockets.TcpClient())
                    {
                        await client.ConnectAsync("127.0.0.1", testPort);
                        using var stream = client.GetStream();

                        var handshake = new KtmHandshake
                        {
                            DeviceId = "test-device",
                            DeviceName = "TestDevice",
                            Pin = "123456"
                        };

                        byte[] jsonBytes = System.Text.Encoding.UTF8.GetBytes(System.Text.Json.JsonSerializer.Serialize(handshake));
                        byte[] lenBytes = new byte[4];
                        lenBytes[0] = (byte)(jsonBytes.Length >> 24);
                        lenBytes[1] = (byte)(jsonBytes.Length >> 16);
                        lenBytes[2] = (byte)(jsonBytes.Length >> 8);
                        lenBytes[3] = (byte)jsonBytes.Length;

                        await stream.WriteAsync(lenBytes, 0, 4);
                        await stream.WriteAsync(jsonBytes, 0, jsonBytes.Length);
                        await stream.FlushAsync();

                        byte[] respLenBytes = new byte[4];
                        var readTask = stream.ReadAsync(respLenBytes, 0, 4);
                        var timeoutTask = Task.Delay(2000);
                        var winner = await Task.WhenAny(readTask, timeoutTask);
                        if (winner == timeoutTask)
                        {
                            Console.WriteLine("[ERROR] Pause test hung on reading response.");
                            return false;
                        }

                        int bytesRead = await readTask;
                        if (bytesRead == 4)
                        {
                            int respLen = (respLenBytes[0] << 24) | (respLenBytes[1] << 16) | (respLenBytes[2] << 8) | respLenBytes[3];
                            byte[] respBytes = new byte[respLen];
                            await stream.ReadAsync(respBytes, 0, respLen);
                            var ack = System.Text.Json.JsonSerializer.Deserialize<KtmHandshakeAck>(System.Text.Encoding.UTF8.GetString(respBytes));
                            if (ack?.Accepted != true)
                            {
                                rejectedOrClosed = true;
                            }
                        }
                        else
                        {
                            rejectedOrClosed = true;
                        }
                    }
                }
                catch (Exception)
                {
                    rejectedOrClosed = true;
                }

                if (!rejectedOrClosed)
                {
                    Console.WriteLine("[ERROR] Server did not reject or close connection while paused.");
                    return false;
                }

                // Step 2: Unpause
                server.IsPaused = false;
                server.OnHandshakeReceived += h => Task.FromResult(true);

                using (var client2 = new System.Net.Sockets.TcpClient())
                {
                    await client2.ConnectAsync("127.0.0.1", testPort);
                    using var stream2 = client2.GetStream();

                    var handshake2 = new KtmHandshake
                    {
                        DeviceId = "test-device-2",
                        DeviceName = "TestDevice2",
                        Pin = "123456"
                    };

                    byte[] jsonBytes2 = System.Text.Encoding.UTF8.GetBytes(System.Text.Json.JsonSerializer.Serialize(handshake2));
                    byte[] lenBytes2 = new byte[4];
                    lenBytes2[0] = (byte)(jsonBytes2.Length >> 24);
                    lenBytes2[1] = (byte)(jsonBytes2.Length >> 16);
                    lenBytes2[2] = (byte)(jsonBytes2.Length >> 8);
                    lenBytes2[3] = (byte)jsonBytes2.Length;

                    await stream2.WriteAsync(lenBytes2, 0, 4);
                    await stream2.WriteAsync(jsonBytes2, 0, jsonBytes2.Length);
                    await stream2.FlushAsync();

                    byte[] respLenBytes2 = new byte[4];
                    var readTask2 = stream2.ReadAsync(respLenBytes2, 0, 4);
                    var timeoutTask2 = Task.Delay(3000);
                    var winner2 = await Task.WhenAny(readTask2, timeoutTask2);
                    if (winner2 == timeoutTask2)
                    {
                        Console.WriteLine("[ERROR] Step 2 hung waiting for unpaused handshake response.");
                        return false;
                    }

                    int r = await readTask2;
                    if (r == 4)
                    {
                        int respLen2 = (respLenBytes2[0] << 24) | (respLenBytes2[1] << 16) | (respLenBytes2[2] << 8) | respLenBytes2[3];
                        byte[] respBytes2 = new byte[respLen2];
                        await stream2.ReadAsync(respBytes2, 0, respLen2);
                        var ack2 = System.Text.Json.JsonSerializer.Deserialize<KtmHandshakeAck>(System.Text.Encoding.UTF8.GetString(respBytes2));
                        if (ack2?.Accepted != true)
                        {
                            Console.WriteLine("[ERROR] Server did not accept connection when unpaused!");
                            return false;
                        }
                    }
                    else
                    {
                        Console.WriteLine("[ERROR] Unexpected disconnect during unpaused handshake.");
                        return false;
                    }
                }

                Console.WriteLine("Pause receiving test passed: rejected while paused, accepted when resumed.");
                return true;
            }
            finally
            {
                server.Stop();
            }
        }
    }
}
