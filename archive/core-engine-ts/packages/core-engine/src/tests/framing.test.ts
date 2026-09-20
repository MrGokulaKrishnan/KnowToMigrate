import { test } from 'node:test';
import * as assert from 'node:assert';
import { PacketCodec, StreamPacketDecoder } from '../protocol/framing.js';
import { MessageType } from '@knowtomigrate/protocol-types';

test('PacketCodec - Encodes and decodes valid KTM header with CRC32', () => {
  const payload = Buffer.from(JSON.stringify({ greeting: 'Hello KnowToMigrate' }), 'utf-8');
  const sessionId = '1234567890abcdef1234567890abcdef';

  const packet = PacketCodec.encodePacket(MessageType.HELLO, 42, sessionId, payload);
  const decoder = new StreamPacketDecoder();
  const decodedPackets = decoder.feed(packet);

  assert.strictEqual(decodedPackets.length, 1);
  const decoded = decodedPackets[0];
  assert.strictEqual(decoded.header.type, MessageType.HELLO);
  assert.strictEqual(decoded.header.seq, 42);
  assert.strictEqual(decoded.payload.toString('utf-8'), payload.toString('utf-8'));
});

test('PacketCodec - Handles TCP fragmented packet chunks correctly', () => {
  const payload = Buffer.alloc(1024, 0xAA);
  const sessionId = 'abcdef1234567890abcdef1234567890';
  const packet = PacketCodec.encodePacket(MessageType.CHUNK_DATA, 1, sessionId, payload);

  const decoder = new StreamPacketDecoder();
  // Split into 3 arbitrary slices
  const slice1 = packet.subarray(0, 20);
  const slice2 = packet.subarray(20, 300);
  const slice3 = packet.subarray(300);

  let result = decoder.feed(slice1);
  assert.strictEqual(result.length, 0); // Still incomplete

  result = decoder.feed(slice2);
  assert.strictEqual(result.length, 0); // Still incomplete

  result = decoder.feed(slice3);
  assert.strictEqual(result.length, 1); // Fully assembled!
  assert.strictEqual(result[0].payload.length, 1024);
});
