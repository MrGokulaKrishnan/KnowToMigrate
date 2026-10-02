using System;
using System.IO;
using System.Media;
using System.Threading.Tasks;

namespace KnowToMigrate.Services
{
    public static class KtmSoundService
    {
        public static bool IsEnabled { get; set; } = true;

        public static void PlayTransferStart()
        {
            if (!IsEnabled) return;
            Task.Run(() =>
            {
                try
                {
                    var tone = GenerateSineChime(new[] { (523.25, 0.10), (659.25, 0.14) }, 0.28);
                    using var ms = new MemoryStream(tone);
                    using var player = new SoundPlayer(ms);
                    player.PlaySync();
                }
                catch { }
            });
        }

        public static void PlayTransferSuccess()
        {
            if (!IsEnabled) return;
            Task.Run(() =>
            {
                try
                {
                    var tone = GenerateSineChime(new[] { (523.25, 0.08), (659.25, 0.08), (783.99, 0.22) }, 0.35);
                    using var ms = new MemoryStream(tone);
                    using var player = new SoundPlayer(ms);
                    player.PlaySync();
                }
                catch { }
            });
        }

        public static void PlayTransferError()
        {
            if (!IsEnabled) return;
            Task.Run(() =>
            {
                try
                {
                    var tone = GenerateSineChime(new[] { (440.0, 0.12), (349.23, 0.18) }, 0.35);
                    using var ms = new MemoryStream(tone);
                    using var player = new SoundPlayer(ms);
                    player.PlaySync();
                }
                catch { }
            });
        }

        private static byte[] GenerateSineChime((double freq, double durationSec)[] notes, double volume)
        {
            const int sampleRate = 44100;
            const short bitsPerSample = 16;
            const short channels = 1;

            int totalSamples = 0;
            foreach (var note in notes)
            {
                totalSamples += (int)(sampleRate * note.durationSec);
            }

            short[] samples = new short[totalSamples];
            int currentOffset = 0;

            foreach (var (freq, durationSec) in notes)
            {
                int count = (int)(sampleRate * durationSec);
                int attackSamples = Math.Min(sampleRate / 100, count / 4);
                int releaseSamples = Math.Min(sampleRate / 50, count / 3);

                for (int i = 0; i < count; i++)
                {
                    double t = (double)i / sampleRate;
                    double env = 1.0;

                    if (i < attackSamples)
                    {
                        env = (double)i / attackSamples;
                    }
                    else if (i > count - releaseSamples)
                    {
                        env = (double)(count - i) / releaseSamples;
                    }

                    double angle = 2.0 * Math.PI * freq * t;
                    double sample = Math.Sin(angle) * volume * env;
                    samples[currentOffset + i] = (short)(sample * short.MaxValue);
                }
                currentOffset += count;
            }

            using var ms = new MemoryStream();
            using var writer = new BinaryWriter(ms);

            int subChunk2Size = totalSamples * channels * (bitsPerSample / 8);
            int chunkSize = 36 + subChunk2Size;

            writer.Write(new char[4] { 'R', 'I', 'F', 'F' });
            writer.Write(chunkSize);
            writer.Write(new char[4] { 'W', 'A', 'V', 'E' });

            writer.Write(new char[4] { 'f', 'm', 't', ' ' });
            writer.Write(16);
            writer.Write((short)1);
            writer.Write(channels);
            writer.Write(sampleRate);
            writer.Write(sampleRate * channels * (bitsPerSample / 8));
            writer.Write((short)(channels * (bitsPerSample / 8)));
            writer.Write(bitsPerSample);

            writer.Write(new char[4] { 'd', 'a', 't', 'a' });
            writer.Write(subChunk2Size);

            for (int i = 0; i < totalSamples; i++)
            {
                writer.Write(samples[i]);
            }

            writer.Flush();
            return ms.ToArray();
        }
    }
}