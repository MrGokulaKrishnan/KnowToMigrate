/**
 * KnowToMigrate - Binary & JSON Framed Packet Codec
 */

import { KTM_MAGIC, KTM_PROTOCOL_VERSION, MessageType, KTMHeader } from '@knowtomigrate/protocol-types';
import * as crypto from 'node:crypto';

export const HEADER_SIZE = 50;

export class PacketCodec {
  /**
   * Fast CRC32 computation for packet header & payload integrity
   */
  public static crc32(buffer: Buffer): number {
    let crc = 0 ^ (-1);
    for (let i = 0; i < buffer.length; i++) {
      crc = (crc >>> 8) ^ PacketCodec.crcTable[(crc ^ buffer[i]) & 0xFF];
    }
    return (crc ^ (-1)) >>> 0;
  }

  private static crcTable: Uint32Array = (() => {
    const table = new Uint32Array(256);
    for (let i = 0; i < 256; i++) {
      let c = i;
      for (let k = 0; k < 8; k++) {
        c = ((c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1));
      }
      table[i] = c >>> 0;
    }
    return table;
  })();

  /**
   * Encodes header and payload into a single contiguous Buffer
   */
  public static encodePacket(
    type: MessageType,
    seq: number,
    sessionId: string,
    payload: Buffer
  ): Buffer {
    const header = Buffer.alloc(HEADER_SIZE);
    
    // 1. Magic (4 bytes)
    header.write(KTM_MAGIC, 0, 4, 'ascii');
    // 2. Version (1 byte)
    header.writeUInt8(KTM_PROTOCOL_VERSION, 4);
    // 3. Type (1 byte)
    header.writeUInt8(type, 5);
    // 4. Sequence Number (4 bytes)
    header.writeUInt32BE(seq, 6);
    // 5. Session ID (32 chars hex, fixed width)
    const cleanSessionId = sessionId.padEnd(32, '0').slice(0, 32);
    header.write(cleanSessionId, 10, 32, 'ascii');
    // 6. Payload Length (4 bytes)
    header.writeUInt32BE(payload.length, 42);
    // 7. CRC32 of payload (4 bytes)
    const payloadCrc = PacketCodec.crc32(payload);
    header.writeUInt32BE(payloadCrc, 46);

    return Buffer.concat([header, payload]);
  }

  /**
   * Decodes a header from raw buffer
   */
  public static decodeHeader(headerBuffer: Buffer): KTMHeader {
    if (headerBuffer.length < HEADER_SIZE) {
      throw new Error(`Buffer too short for KTM header: ${headerBuffer.length} < ${HEADER_SIZE}`);
    }

    const magic = headerBuffer.toString('ascii', 0, 4);
    if (magic !== KTM_MAGIC) {
      throw new Error(`Invalid KTM magic bytes: "${magic}" (expected "${KTM_MAGIC}")`);
    }

    const version = headerBuffer.readUInt8(4);
    const type = headerBuffer.readUInt8(5) as MessageType;
    const seq = headerBuffer.readUInt32BE(6);
    const sessionId = headerBuffer.toString('ascii', 10, 42);
    const payloadLength = headerBuffer.readUInt32BE(42);
    const crc32 = headerBuffer.readUInt32BE(46);

    return {
      magic,
      version,
      type,
      seq,
      sessionId,
      payloadLength,
      crc32
    };
  }
}

/**
 * Streaming packet accumulator to handle TCP chunk fragmentation
 */
export class StreamPacketDecoder {
  private buffer: Buffer = Buffer.alloc(0);

  public feed(chunk: Buffer): { header: KTMHeader; payload: Buffer }[] {
    this.buffer = Buffer.concat([this.buffer, chunk]);
    const packets: { header: KTMHeader; payload: Buffer }[] = [];

    while (this.buffer.length >= HEADER_SIZE) {
      const header = PacketCodec.decodeHeader(this.buffer.subarray(0, HEADER_SIZE));
      const totalPacketSize = HEADER_SIZE + header.payloadLength;

      if (this.buffer.length < totalPacketSize) {
        // Incomplete packet in stream; wait for more data
        break;
      }

      const payload = this.buffer.subarray(HEADER_SIZE, totalPacketSize);
      
      // Verify payload CRC32
      const computedCrc = PacketCodec.crc32(payload);
      if (computedCrc !== header.crc32) {
        throw new Error(`CRC32 mismatch on packet type 0x${header.type.toString(16)}: expected ${header.crc32}, got ${computedCrc}`);
      }

      packets.push({ header, payload });
      this.buffer = this.buffer.subarray(totalPacketSize);
    }

    return packets;
  }
}
