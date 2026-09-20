using System;
using System.Runtime.InteropServices;

namespace KnowToMigrate.Interop
{
    /// <summary>
    /// P/Invoke bindings to the KnowToMigrate Rust core (ktm.dll)
    /// </summary>
    internal static class KtmInterop
    {
        private const string KtmDll = "ktm_ffi";

        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void DeviceDiscoveredCallback([MarshalAs(UnmanagedType.LPStr)] string deviceJson);

        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void TransferProgressCallback(
            [MarshalAs(UnmanagedType.LPStr)] string sessionId,
            long bytesSent,
            long totalBytes,
            double speedBps,
            double etaSecs
        );

        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void TransferRequestCallback(
            [MarshalAs(UnmanagedType.LPStr)] string deviceJson,
            [MarshalAs(UnmanagedType.LPStr)] string manifestJson
        );

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr ktm_init();

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_destroy(IntPtr handle);

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ktm_start_discovery(IntPtr handle, DeviceDiscoveredCallback callback);

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_stop_discovery(IntPtr handle);

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern IntPtr ktm_send_file(
            IntPtr handle,
            [MarshalAs(UnmanagedType.LPStr)] string targetIp,
            ushort targetPort,
            [MarshalAs(UnmanagedType.LPStr)] string filePath,
            TransferProgressCallback progressCallback
        );

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern int ktm_receive_start(
            IntPtr handle,
            [MarshalAs(UnmanagedType.LPStr)] string receiveDir,
            TransferRequestCallback requestCallback,
            TransferProgressCallback progressCallback
        );

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_cancel(
            IntPtr handle,
            [MarshalAs(UnmanagedType.LPStr)] string sessionId
        );

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPStr)]
        public static extern string? ktm_get_device_id(IntPtr handle);

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        [return: MarshalAs(UnmanagedType.LPStr)]
        public static extern string? ktm_get_version();

        [DllImport(KtmDll, CallingConvention = CallingConvention.Cdecl)]
        public static extern void ktm_free_string(IntPtr ptr);
    }
}

