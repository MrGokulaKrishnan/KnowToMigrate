using System;
using System.Collections.Generic;
using System.Text;
using System.Windows.Media;
using System.Windows.Media.Imaging;

namespace KnowToMigrate.Services
{
    public static class KtmQrCodeGenerator
    {
        public static BitmapSource GenerateQrCode(string content, int targetPixelSize = 256)
        {
            if (string.IsNullOrEmpty(content))
            {
                content = "https://knowtomigrate.web.app";
            }

            byte[] textBytes = Encoding.UTF8.GetBytes(content);

            int version;
            int totalCodewords;
            int ecCodewords;

            if (textBytes.Length <= 14)
            {
                version = 1;
                totalCodewords = 26;
                ecCodewords = 10;
            }
            else if (textBytes.Length <= 26)
            {
                version = 2;
                totalCodewords = 44;
                ecCodewords = 16;
            }
            else if (textBytes.Length <= 42)
            {
                version = 3;
                totalCodewords = 70;
                ecCodewords = 26;
            }
            else
            {
                version = 4;
                totalCodewords = 100;
                ecCodewords = 36;
            }

            int dataCodewords = totalCodewords - ecCodewords;

            var bitBuffer = new List<bool>();
            AddBits(bitBuffer, 0x4, 4);
            AddBits(bitBuffer, textBytes.Length, 8);
            foreach (byte b in textBytes)
            {
                AddBits(bitBuffer, b, 8);
            }

            int padBits = (dataCodewords * 8) - bitBuffer.Count;
            if (padBits > 0)
            {
                int terminatorLen = Math.Min(4, padBits);
                AddBits(bitBuffer, 0, terminatorLen);
            }

            while (bitBuffer.Count % 8 != 0)
            {
                bitBuffer.Add(false);
            }

            var dataBytes = new List<byte>();
            for (int i = 0; i < bitBuffer.Count; i += 8)
            {
                int val = 0;
                for (int b = 0; b < 8; b++)
                {
                    if (bitBuffer[i + b]) val |= (1 << (7 - b));
                }
                dataBytes.Add((byte)val);
            }

            byte[] padBytes = { 0xEC, 0x11 };
            int padIdx = 0;
            while (dataBytes.Count < dataCodewords)
            {
                dataBytes.Add(padBytes[padIdx % 2]);
                padIdx++;
            }

            byte[] ecBytes = ComputeReedSolomon(dataBytes.ToArray(), ecCodewords);

            var allCodewords = new List<byte>(dataBytes);
            allCodewords.AddRange(ecBytes);

            int moduleCount = 17 + 4 * version;
            var matrix = new bool[moduleCount, moduleCount];
            var isFunction = new bool[moduleCount, moduleCount];

            PlaceFinderPattern(matrix, isFunction, 0, 0);
            PlaceFinderPattern(matrix, isFunction, moduleCount - 7, 0);
            PlaceFinderPattern(matrix, isFunction, 0, moduleCount - 7);

            if (version >= 2)
            {
                int alignPos = version switch
                {
                    2 => 18,
                    3 => 22,
                    _ => 26
                };
                PlaceAlignmentPattern(matrix, isFunction, alignPos, alignPos);
            }

            for (int i = 8; i < moduleCount - 8; i++)
            {
                bool timingVal = (i % 2 == 0);
                if (!isFunction[6, i])
                {
                    matrix[6, i] = timingVal;
                    isFunction[6, i] = true;
                }
                if (!isFunction[i, 6])
                {
                    matrix[i, 6] = timingVal;
                    isFunction[i, 6] = true;
                }
            }

            matrix[4 * version + 9, 8] = true;
            isFunction[4 * version + 9, 8] = true;

            ReserveFormatInfo(isFunction, moduleCount);

            var totalBits = new List<bool>();
            foreach (byte b in allCodewords)
            {
                for (int bit = 7; bit >= 0; bit--)
                {
                    totalBits.Add(((b >> bit) & 1) == 1);
                }
            }

            int bitIndex = 0;
            int right = moduleCount - 1;
            bool goingUp = true;

            while (right > 0)
            {
                if (right == 6) right--;

                int col1 = right;
                int col2 = right - 1;

                int rowStart = goingUp ? moduleCount - 1 : 0;
                int rowEnd = goingUp ? -1 : moduleCount;
                int rowStep = goingUp ? -1 : 1;

                for (int row = rowStart; row != rowEnd; row += rowStep)
                {
                    for (int c = 0; c < 2; c++)
                    {
                        int col = (c == 0) ? col1 : col2;
                        if (!isFunction[row, col])
                        {
                            bool bitVal = bitIndex < totalBits.Count && totalBits[bitIndex++];
                            if ((row + col) % 2 == 0)
                            {
                                bitVal = !bitVal;
                            }
                            matrix[row, col] = bitVal;
                        }
                    }
                }

                right -= 2;
                goingUp = !goingUp;
            }

            PlaceFormatInfo(matrix, moduleCount, 0x5412);

            const int quietZone = 4;
            int totalGrid = moduleCount + (quietZone * 2);
            int scale = Math.Max(4, targetPixelSize / totalGrid);
            int finalWidth = totalGrid * scale;
            int finalHeight = totalGrid * scale;

            int stride = finalWidth * 4;
            byte[] pixelData = new byte[finalHeight * stride];

            for (int i = 0; i < pixelData.Length; i += 4)
            {
                pixelData[i] = 255;
                pixelData[i + 1] = 255;
                pixelData[i + 2] = 255;
                pixelData[i + 3] = 255;
            }

            for (int r = 0; r < moduleCount; r++)
            {
                for (int c = 0; c < moduleCount; c++)
                {
                    if (matrix[r, c])
                    {
                        int startX = (c + quietZone) * scale;
                        int startY = (r + quietZone) * scale;

                        for (int py = 0; py < scale; py++)
                        {
                            int yPos = startY + py;
                            for (int px = 0; px < scale; px++)
                            {
                                int xPos = startX + px;
                                int idx = (yPos * stride) + (xPos * 4);
                                pixelData[idx] = 0;
                                pixelData[idx + 1] = 0;
                                pixelData[idx + 2] = 0;
                                pixelData[idx + 3] = 255;
                            }
                        }
                    }
                }
            }

            var bmp = BitmapSource.Create(
                finalWidth,
                finalHeight,
                96,
                96,
                PixelFormats.Bgra32,
                null,
                pixelData,
                stride
            );
            bmp.Freeze();
            return bmp;
        }

        private static void AddBits(List<bool> buffer, int value, int bitCount)
        {
            for (int i = bitCount - 1; i >= 0; i--)
            {
                buffer.Add(((value >> i) & 1) == 1);
            }
        }

        private static void PlaceFinderPattern(bool[,] matrix, bool[,] isFunc, int startRow, int startCol)
        {
            for (int r = 0; r < 7; r++)
            {
                for (int c = 0; c < 7; c++)
                {
                    bool isBlack = (r == 0 || r == 6 || c == 0 || c == 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4));
                    matrix[startRow + r, startCol + c] = isBlack;
                    isFunc[startRow + r, startCol + c] = true;
                }
            }

            for (int r = -1; r <= 7; r++)
            {
                for (int c = -1; c <= 7; c++)
                {
                    int row = startRow + r;
                    int col = startCol + c;
                    if (row >= 0 && row < matrix.GetLength(0) && col >= 0 && col < matrix.GetLength(1))
                    {
                        if (!isFunc[row, col])
                        {
                            matrix[row, col] = false;
                            isFunc[row, col] = true;
                        }
                    }
                }
            }
        }

        private static void PlaceAlignmentPattern(bool[,] matrix, bool[,] isFunc, int centerRow, int centerCol)
        {
            for (int r = -2; r <= 2; r++)
            {
                for (int c = -2; c <= 2; c++)
                {
                    int row = centerRow + r;
                    int col = centerCol + c;
                    if (!isFunc[row, col])
                    {
                        bool isBlack = (Math.Abs(r) == 2 || Math.Abs(c) == 2 || (r == 0 && c == 0));
                        matrix[row, col] = isBlack;
                        isFunc[row, col] = true;
                    }
                }
            }
        }

        private static void ReserveFormatInfo(bool[,] isFunc, int size)
        {
            for (int i = 0; i <= 8; i++)
            {
                isFunc[8, i] = true;
                isFunc[i, 8] = true;
                isFunc[8, size - 1 - i] = true;
                isFunc[size - 1 - i, 8] = true;
            }
        }

        private static void PlaceFormatInfo(bool[,] matrix, int size, int formatBits)
        {
            int[] rowMap1 = { 8, 8, 8, 8, 8, 8, 8, 8, 7, 5, 4, 3, 2, 1, 0 };
            int[] colMap1 = { 0, 1, 2, 3, 4, 5, 7, 8, 8, 8, 8, 8, 8, 8, 8 };

            for (int i = 0; i < 15; i++)
            {
                bool bit = ((formatBits >> (14 - i)) & 1) == 1;
                matrix[rowMap1[i], colMap1[i]] = bit;
            }

            int[] rowMap2 = { size - 1, size - 2, size - 3, size - 4, size - 5, size - 6, size - 7, 8, 8, 8, 8, 8, 8, 8, 8 };
            int[] colMap2 = { 8, 8, 8, 8, 8, 8, 8, size - 8, size - 7, size - 6, size - 5, size - 4, size - 3, size - 2, size - 1 };

            for (int i = 0; i < 15; i++)
            {
                bool bit = ((formatBits >> (14 - i)) & 1) == 1;
                matrix[rowMap2[i], colMap2[i]] = bit;
            }
        }

        private static readonly byte[] GfExp = new byte[512];
        private static readonly byte[] GfLog = new byte[256];

        static KtmQrCodeGenerator()
        {
            int x = 1;
            for (int i = 0; i < 255; i++)
            {
                GfExp[i] = (byte)x;
                GfExp[i + 255] = (byte)x;
                GfLog[x] = (byte)i;
                x <<= 1;
                if ((x & 0x100) != 0)
                {
                    x ^= 0x11D;
                }
            }
        }

        private static byte GfMultiply(byte a, byte b)
        {
            if (a == 0 || b == 0) return 0;
            return GfExp[GfLog[a] + GfLog[b]];
        }

        private static byte[] ComputeReedSolomon(byte[] data, int ecCount)
        {
            byte[] gen = { 1 };
            for (int i = 0; i < ecCount; i++)
            {
                byte factor = GfExp[i];
                byte[] next = new byte[gen.Length + 1];
                for (int j = 0; j < gen.Length; j++)
                {
                    next[j] ^= GfMultiply(gen[j], factor);
                    next[j + 1] ^= gen[j];
                }
                gen = next;
            }

            byte[] remainder = new byte[ecCount];
            foreach (byte b in data)
            {
                byte factor = (byte)(b ^ remainder[0]);
                for (int j = 0; j < ecCount - 1; j++)
                {
                    remainder[j] = (byte)(remainder[j + 1] ^ GfMultiply(gen[ecCount - 1 - j], factor));
                }
                remainder[ecCount - 1] = GfMultiply(gen[0], factor);
            }

            return remainder;
        }
    }
}