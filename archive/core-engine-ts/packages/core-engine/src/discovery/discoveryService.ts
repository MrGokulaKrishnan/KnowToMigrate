/**
 * KnowToMigrate - UDP Broadcast & Multicast Discovery Service
 */

import * as dgram from 'node:dgram';
import { EventEmitter } from 'node:events';
import { DeviceInfo, VisibilityMode } from '@knowtomigrate/protocol-types';

export const DISCOVERY_PORT = 54123;
export const BROADCAST_ADDR = '255.255.255.255';

export interface DiscoveryBeacon {
  magic: 'KTM_BEACON';
  version: 2;
  deviceId: string;
  name: string;
  platform: string;
  deviceType: string;
  port: number;
  publicKey: string;
  capabilities: string[];
  visibility: VisibilityMode;
  timestamp: number;
}

export class DiscoveryService extends EventEmitter {
  private socket: dgram.Socket | null = null;
  private localDevice: DeviceInfo;
  private beaconTimer: NodeJS.Timeout | null = null;
  private pruneTimer: NodeJS.Timeout | null = null;
  private discoveredDevices: Map<string, DeviceInfo> = new Map();
  private isRunning: boolean = false;

  constructor(localDevice: DeviceInfo) {
    super();
    this.localDevice = localDevice;
  }

  public async start(): Promise<void> {
    if (this.isRunning) return;

    this.socket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

    this.socket.on('message', (msg, rinfo) => {
      this.handleIncomingBeacon(msg, rinfo.address);
    });

    this.socket.on('error', (err) => {
      this.emit('error', err);
    });

    await new Promise<void>((resolve, reject) => {
      this.socket!.bind(DISCOVERY_PORT, () => {
        try {
          this.socket!.setBroadcast(true);
          resolve();
        } catch (e) {
          reject(e);
        }
      });
    });

    this.isRunning = true;

    // Start periodic beacon broadcasting every 2.5s
    this.beaconTimer = setInterval(() => {
      this.broadcastBeacon();
    }, 2500);

    // Prune devices not seen in 8s
    this.pruneTimer = setInterval(() => {
      this.pruneExpiredDevices();
    }, 3000);

    // Initial immediate broadcast
    this.broadcastBeacon();
  }

  public stop(): void {
    if (!this.isRunning) return;
    if (this.beaconTimer) clearInterval(this.beaconTimer);
    if (this.pruneTimer) clearInterval(this.pruneTimer);
    if (this.socket) {
      try { this.socket.close(); } catch {}
      this.socket = null;
    }
    this.isRunning = false;
  }

  public getDiscoveredDevices(): DeviceInfo[] {
    return Array.from(this.discoveredDevices.values());
  }

  public updateLocalVisibility(visibility: VisibilityMode): void {
    this.localDevice.visibility = visibility;
    this.broadcastBeacon();
  }

  private broadcastBeacon(): void {
    if (!this.socket || !this.isRunning) return;
    if (this.localDevice.visibility === 'nobody') return;

    const beacon: DiscoveryBeacon = {
      magic: 'KTM_BEACON',
      version: 2,
      deviceId: this.localDevice.id,
      name: this.localDevice.name,
      platform: this.localDevice.platform,
      deviceType: this.localDevice.deviceType,
      port: this.localDevice.port || 54124,
      publicKey: this.localDevice.publicKey,
      capabilities: this.localDevice.capabilities,
      visibility: this.localDevice.visibility,
      timestamp: Date.now()
    };

    const payload = Buffer.from(JSON.stringify(beacon), 'utf-8');
    this.socket.send(payload, 0, payload.length, DISCOVERY_PORT, BROADCAST_ADDR, () => {
      // Ignored
    });
  }

  private handleIncomingBeacon(msg: Buffer, senderAddress: string): void {
    try {
      const beacon: DiscoveryBeacon = JSON.parse(msg.toString('utf-8'));
      if (beacon.magic !== 'KTM_BEACON' || beacon.deviceId === this.localDevice.id) {
        return; // Ignore own beacons and alien packets
      }

      const existing = this.discoveredDevices.get(beacon.deviceId);
      const updated: DeviceInfo = {
        id: beacon.deviceId,
        name: beacon.name,
        platform: beacon.platform as any,
        deviceType: beacon.deviceType as any,
        publicKey: beacon.publicKey,
        address: senderAddress,
        port: beacon.port,
        status: 'discovered',
        connectionQuality: 1.0,
        isTrusted: existing ? existing.isTrusted : false,
        autoAccept: existing ? existing.autoAccept : false,
        visibility: beacon.visibility,
        lastSeen: Date.now(),
        capabilities: beacon.capabilities
      };

      const isNew = !this.discoveredDevices.has(beacon.deviceId);
      this.discoveredDevices.set(beacon.deviceId, updated);

      if (isNew) {
        this.emit('deviceFound', updated);
      } else {
        this.emit('deviceUpdated', updated);
      }
    } catch {
      // Ignore invalid JSON / corrupted packets
    }
  }

  private pruneExpiredDevices(): void {
    const now = Date.now();
    for (const [id, device] of this.discoveredDevices.entries()) {
      if (now - device.lastSeen > 8000) {
        this.discoveredDevices.delete(id);
        this.emit('deviceLost', device);
      }
    }
  }
}
