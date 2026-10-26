using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Text.Json;

namespace KnowToMigrate.Services
{
    /// <summary>
    /// Managed interop bridge binding KnowToMigrate Windows WPF to the unified Rust core (ktm_ffi.dll).
    /// Enforces single-source-of-truth cryptography, framing, chunking, and session pipelines.
    /// </summary>
    public static class KtmNativeBridge
    {
        private const string DllName = "ktm_ffi.dll";

        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void DeviceDiscoveredCallback(IntPtr deviceJsonPtr);

        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void TransferProgressCallback(
            IntPtr sessionIdPtr,
            ulong bytesTransferred,
            ulong totalBytes,
            double speedBps,
            ulong etaSecs
        );

        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate int TransferRequestCallback(IntPtr deviceJsonPtr, IntPtr manifestJsonPtr);

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr ktm_init();

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_destroy(IntPtr handle);

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr ktm_get_version();

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr ktm_get_device_id(IntPtr handle);

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_free_string(IntPtr ptr);

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ktm_start_discovery(IntPtr handle, DeviceDiscoveredCallback callback);

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_stop_discovery(IntPtr handle);

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern IntPtr ktm_send_file(
            IntPtr handle,
            string targetIp,
            ushort targetPort,
            string filePath,
            TransferProgressCallback callback
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern int ktm_receive_start(
            IntPtr handle,
            string receiveDir,
            TransferRequestCallback requestCallback,
            TransferProgressCallback progressCallback
        );

        [DllImport(DllName, CallingConvention = CallingConvention.Cdecl, CharSet = CharSet.Ansi)]
        public static extern void ktm_cancel(IntPtr handle, string sessionId);

        public static string? PtrToStringAndFree(IntPtr ptr)
        {
            if (ptr == IntPtr.Zero) return null;
            try
            {
                return Marshal.PtrToStringUTF8(ptr) ?? Marshal.PtrToStringAnsi(ptr);
            }
            finally
            {
                ktm_free_string(ptr);
            }
        }

        private static IntPtr _nativeHandle = IntPtr.Zero;
        private static readonly object _syncLock = new();
        private static bool _isAvailable = false;
        private static string _coreVersion = "Unavailable";

        public static bool IsAvailable => _isAvailable;
        public static string CoreVersion => _coreVersion;

        public static bool Initialize()
        {
            lock (_syncLock)
            {
                if (_nativeHandle != IntPtr.Zero) return true;

                try
                {
                    _nativeHandle = ktm_init();
                    if (_nativeHandle != IntPtr.Zero)
                    {
                        IntPtr vPtr = ktm_get_version();
                        _coreVersion = PtrToStringAndFree(vPtr) ?? "1.0.0";
                        _isAvailable = true;
                        return true;
                    }
                }
                catch (Exception ex)
                {
                    _isAvailable = false;
                    _coreVersion = $"Unavailable ({ex.Message})";
                }
                return false;
            }
        }

        public static void Shutdown()
        {
            lock (_syncLock)
            {
                if (_nativeHandle != IntPtr.Zero)
                {
                    try
                    {
                        ktm_stop_discovery(_nativeHandle);
                        ktm_destroy(_nativeHandle);
                    }
                    catch { }
                    finally
                    {
                        _nativeHandle = IntPtr.Zero;
                        _isAvailable = false;
                    }
                }
            }
        }
    }
}
