import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  loadSyncConfig,
  saveSyncConfig,
  registerDevice,
  pairDevice,
  getSyncStatus,
  pushChanges,
  pullChanges,
  fullSync,
  getSyncEventLog,
  disconnectSync,
} from '../lib/sync';
import type { SyncConfig } from '../lib/sync';

interface Props {
  onBack: () => void;
}

interface ToastState {
  message: string;
  type: 'success' | 'error';
}

interface DeviceInfo {
  device_id: string;
  device_name: string;
  last_seen: number;
}

function formatRelativeTime(epoch: number): string {
  if (!epoch) return 'Never';
  const diffMs = Date.now() - epoch;
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function formatPairingCode(raw: string): string {
  const cleaned = raw.replace(/[^a-zA-Z0-9]/g, '').slice(0, 6).toUpperCase();
  if (cleaned.length <= 3) return cleaned;
  return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 6)}`;
}

function copyToClipboard(text: string, onSuccess: () => void): void {
  navigator.clipboard.writeText(text).then(onSuccess).catch(() => {});
}

export default function SyncSettingsScreen({ onBack }: Props) {
  const [config, setConfig] = useState<SyncConfig | null>(null);
  const [loading, setLoading] = useState(true);

  const [serverUrl, setServerUrl] = useState('https://sync.nudge.app');
  const [deviceName, setDeviceName] = useState('');
  const [pairingInput, setPairingInput] = useState('');
  const [pairDeviceName, setPairDeviceName] = useState('');
  const [registering, setRegistering] = useState(false);
  const [pairing, setPairing] = useState(false);

  const [editingServerUrl, setEditingServerUrl] = useState(false);
  const [editedServerUrl, setEditedServerUrl] = useState('');

  const [statusInfo, setStatusInfo] = useState<{ devices: DeviceInfo[] } | null>(null);
  const [statusLoading, setStatusLoading] = useState(false);

  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<string | null>(null);
  const [autoSync, setAutoSync] = useState(false);

  const [showLog, setShowLog] = useState(false);
  const [syncLog, setSyncLog] = useState(getSyncEventLog());

  const [toast, setToast] = useState<ToastState | null>(null);
  const [copied, setCopied] = useState(false);

  const autoSyncRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const showToast = (message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const refreshConfig = useCallback(() => {
    const cfg = loadSyncConfig();
    setConfig(cfg);
    setAutoSync(cfg?.isEnabled ?? false);
    if (cfg) {
      setServerUrl(cfg.serverUrl);
    }
  }, []);

  const refreshStatus = useCallback(async () => {
    const cfg = loadSyncConfig();
    if (!cfg) return;
    setStatusLoading(true);
    try {
      const info = await getSyncStatus(cfg);
      setStatusInfo(info);
    } catch {
      // server may be unreachable
    } finally {
      setStatusLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshConfig();
    setLoading(false);
  }, [refreshConfig]);

  useEffect(() => {
    if (config) {
      refreshStatus();
    }
  }, [config, refreshStatus]);

  useEffect(() => {
    if (autoSync && config) {
      autoSyncRef.current = setInterval(async () => {
        try {
          await fullSync(config);
          refreshConfig();
          setSyncLog(getSyncEventLog());
        } catch {
          // silent background failure
        }
      }, 30_000);
    }
    return () => {
      if (autoSyncRef.current) clearInterval(autoSyncRef.current);
    };
  }, [autoSync, config, refreshConfig]);

  useEffect(() => {
    if (copied) {
      const t = setTimeout(() => setCopied(false), 2000);
      return () => clearTimeout(t);
    }
  }, [copied]);

  const handleRegister = async () => {
    if (!serverUrl.trim() || !deviceName.trim()) {
      showToast('Server URL and device name are required', 'error');
      return;
    }
    setRegistering(true);
    try {
      const newConfig = await registerDevice(serverUrl.trim(), deviceName.trim());
      setConfig(newConfig);
      setDeviceName('');
      showToast('Device registered — sharing code created', 'success');
    } catch (e: any) {
      showToast(e.message || 'Registration failed', 'error');
    } finally {
      setRegistering(false);
    }
  };

  const handlePair = async () => {
    const code = pairingInput.replace(/[^a-zA-Z0-9]/g, '');
    if (code.length !== 6 || !pairDeviceName.trim()) {
      showToast('Enter a valid 6-character pairing code and device name', 'error');
      return;
    }
    setPairing(true);
    try {
      const newConfig = await pairDevice(serverUrl.trim(), code, pairDeviceName.trim());
      setConfig(newConfig);
      setPairingInput('');
      setPairDeviceName('');
      showToast('Paired successfully', 'success');
    } catch (e: any) {
      showToast(e.message || 'Pairing failed', 'error');
    } finally {
      setPairing(false);
    }
  };

  const handleSyncNow = async () => {
    if (!config) return;
    setSyncing(true);
    setSyncResult(null);
    try {
      const result = await fullSync(config);
      refreshConfig();
      setSyncLog(getSyncEventLog());
      setSyncResult(`Pushed ${result.pushed}, Pulled ${result.pulled}`);
      showToast(`Sync complete — ${result.pushed} up, ${result.pulled} down`, 'success');
    } catch (e: any) {
      showToast(e.message || 'Sync failed', 'error');
    } finally {
      setSyncing(false);
    }
  };

  const handleToggleAutoSync = () => {
    const next = !autoSync;
    setAutoSync(next);
    if (config) {
      const updated = { ...config, isEnabled: next };
      saveSyncConfig(updated);
      setConfig(updated);
    }
  };

  const handleEditServer = () => {
    setEditedServerUrl(config?.serverUrl || serverUrl);
    setEditingServerUrl(true);
  };

  const handleSaveServer = () => {
    if (config && editedServerUrl.trim()) {
      const updated = { ...config, serverUrl: editedServerUrl.trim() };
      saveSyncConfig(updated);
      setConfig(updated);
    }
    setEditingServerUrl(false);
    showToast('Server URL updated', 'success');
  };

  const handleDisconnect = () => {
    disconnectSync();
    setConfig(null);
    setStatusInfo(null);
    setAutoSync(false);
    showToast('Sync disconnected', 'success');
  };

  const handlePairingCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.replace(/[^a-zA-Z0-9]/g, '').slice(0, 6).toUpperCase();
    setPairingInput(raw);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-surface-base flex items-center justify-center">
        <p className="text-caption text-content-tertiary">Loading...</p>
      </div>
    );
  }

  const isConfigured = config !== null;
  const lastSyncText = config?.lastSyncAt ? formatRelativeTime(config.lastSyncAt) : 'Never';

  return (
    <div className="min-h-screen bg-surface-base">
      <div className="max-w-2xl mx-auto p-4">
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-content-secondary hover:text-content-primary text-body"
          >
            ← Back
          </button>
          <h1 className="text-title font-bold text-content-primary">Sync Settings</h1>
          <div className="w-14" />
        </div>

        <AnimatePresence>
          {toast && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="fixed top-4 left-1/2 -translate-x-1/2 z-50"
            >
              <div
                className={`px-6 py-3 rounded-xl text-caption font-medium shadow-lg ${
                  toast.type === 'success'
                    ? 'bg-positive text-white'
                    : 'bg-negative text-white'
                }`}
              >
                {toast.message}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="space-y-6">
          {/* Sync Status Indicator */}
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="p-6 bg-surface-raised rounded-xl"
            style={{ boxShadow: 'var(--shadow-sm)' }}
          >
            <div className="flex items-center gap-3 mb-2">
              <span
                className="w-3 h-3 rounded-full"
                style={{
                  backgroundColor: isConfigured
                    ? 'var(--color-positive)'
                    : 'var(--color-content-tertiary)',
                }}
              />
              <span className="text-heading font-bold text-content-primary">
                {isConfigured ? 'Sync is active' : 'Sync not configured'}
              </span>
            </div>
            <p className="text-caption text-content-secondary">
              Last synced: {lastSyncText}
            </p>
          </motion.section>

          {/* Setup Section (not configured) */}
          {!isConfigured && (
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05 }}
              className="p-6 bg-surface-raised rounded-xl"
              style={{ boxShadow: 'var(--shadow-sm)' }}
            >
              <h2 className="text-heading font-bold text-content-primary mb-4">Setup Sync</h2>

              <label className="block text-caption font-medium text-content-secondary mb-1.5">
                Server URL
              </label>
              <input
                type="text"
                placeholder="https://sync.nudge.app"
                value={serverUrl}
                onChange={(e) => setServerUrl(e.target.value)}
                className="w-full p-3 bg-surface-base rounded-md outline-none text-body text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20 mb-4"
              />

              {/* Register */}
              <div
                className="p-5 rounded-xl border mb-4"
                style={{
                  borderColor: 'var(--color-content-tertiary)',
                  backgroundColor: 'var(--color-surface-base)',
                }}
              >
                <h3 className="text-body font-semibold text-content-primary mb-1">
                  Register New Device
                </h3>
                <p className="text-caption text-content-secondary mb-4">
                  Create a new sync identity and get a pairing code to share with other devices.
                </p>
                <input
                  type="text"
                  placeholder="Device name (e.g. Chrome Desktop)"
                  value={deviceName}
                  onChange={(e) => setDeviceName(e.target.value)}
                  className="w-full p-2.5 bg-surface-base rounded-md outline-none text-caption text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20 mb-3"
                />
                <button
                  onClick={handleRegister}
                  disabled={registering || !serverUrl.trim() || !deviceName.trim()}
                  className="w-full py-2.5 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                >
                  {registering ? 'Registering...' : 'Register New Device'}
                </button>
              </div>

              {/* Pair */}
              <div
                className="p-5 rounded-xl border"
                style={{
                  borderColor: 'var(--color-content-tertiary)',
                  backgroundColor: 'var(--color-surface-base)',
                }}
              >
                <h3 className="text-body font-semibold text-content-primary mb-1">
                  Pair with Existing Device
                </h3>
                <p className="text-caption text-content-secondary mb-4">
                  Enter the pairing code from another device to link them.
                </p>
                <label className="block text-micro font-medium text-content-secondary mb-1">
                  Pairing Code
                </label>
                <input
                  type="text"
                  placeholder="ABC-123"
                  value={formatPairingCode(pairingInput)}
                  onChange={handlePairingCodeChange}
                  maxLength={7}
                  className="w-full p-2.5 bg-surface-base rounded-md outline-none text-caption tracking-[0.3em] font-mono text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20 mb-3 text-center"
                />
                <label className="block text-micro font-medium text-content-secondary mb-1">
                  Device Name
                </label>
                <input
                  type="text"
                  placeholder="My Phone"
                  value={pairDeviceName}
                  onChange={(e) => setPairDeviceName(e.target.value)}
                  className="w-full p-2.5 bg-surface-base rounded-md outline-none text-caption text-content-primary placeholder:text-content-tertiary border border-content-tertiary/20 mb-3"
                />
                <button
                  onClick={handlePair}
                  disabled={
                    pairing ||
                    !serverUrl.trim() ||
                    pairingInput.replace(/[^a-zA-Z0-9]/g, '').length !== 6 ||
                    !pairDeviceName.trim()
                  }
                  className="w-full py-2.5 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                >
                  {pairing ? 'Pairing...' : 'Pair'}
                </button>
              </div>
            </motion.section>
          )}

          {/* Status Section (configured) */}
          {isConfigured && config && (
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05 }}
              className="p-6 bg-surface-raised rounded-xl"
              style={{ boxShadow: 'var(--shadow-sm)' }}
            >
              <h2 className="text-heading font-bold text-content-primary mb-4">
                Connection Details
              </h2>

              <div className="space-y-4">
                {/* Server URL */}
                <div>
                  <label className="text-micro font-medium text-content-tertiary block mb-1">
                    Server URL
                  </label>
                  {editingServerUrl ? (
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={editedServerUrl}
                        onChange={(e) => setEditedServerUrl(e.target.value)}
                        className="flex-1 p-2 bg-surface-base rounded-md outline-none text-caption text-content-primary border border-content-tertiary/20"
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleSaveServer();
                        }}
                      />
                      <button
                        onClick={handleSaveServer}
                        className="px-3 py-2 bg-accent-primary text-white rounded-md text-caption font-semibold hover:bg-accent-primary/90 transition-colors"
                      >
                        Save
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between">
                      <span className="text-caption text-content-primary font-mono">
                        {config.serverUrl}
                      </span>
                      <button
                        onClick={handleEditServer}
                        className="text-caption text-accent-primary hover:text-accent-primary/80"
                      >
                        Edit
                      </button>
                    </div>
                  )}
                </div>

                {/* Device ID */}
                <div>
                  <label className="text-micro font-medium text-content-tertiary block mb-1">
                    Device ID
                  </label>
                  <div className="flex items-center justify-between">
                    <span className="text-caption text-content-primary font-mono" style={{ fontSize: '11px' }}>
                      {config.deviceId.slice(0, 16)}...
                    </span>
                    <button
                      onClick={() => {
                        copyToClipboard(config.deviceId, () => {
                          setCopied(true);
                          showToast('Device ID copied', 'success');
                        });
                      }}
                      className="text-caption text-accent-primary hover:text-accent-primary/80"
                    >
                      {copied ? 'Copied!' : 'Copy'}
                    </button>
                  </div>
                </div>

                {/* Pairing Code */}
                <div>
                  <label className="text-micro font-medium text-content-tertiary block mb-1">
                    Pairing Code
                  </label>
                  <div className="flex items-center justify-between">
                    <span className="text-heading font-bold text-content-primary font-mono tracking-[0.3em]">
                      {formatPairingCode(config.pairingCode)}
                    </span>
                    <button
                      onClick={() => {
                        copyToClipboard(config.pairingCode, () => {
                          setCopied(true);
                          showToast('Pairing code copied', 'success');
                        });
                      }}
                      className="text-caption text-accent-primary hover:text-accent-primary/80"
                    >
                      {copied ? 'Copied!' : 'Copy'}
                    </button>
                  </div>
                </div>

                {/* Paired Devices */}
                <div>
                  <label className="text-micro font-medium text-content-tertiary block mb-2">
                    Paired Devices
                  </label>
                  {statusLoading && (
                    <p className="text-caption text-content-tertiary">Loading...</p>
                  )}
                  {!statusLoading && statusInfo?.devices && statusInfo.devices.length > 0 ? (
                    <div className="space-y-2">
                      {statusInfo.devices.map((device) => (
                        <div
                          key={device.device_id}
                          className="flex items-center justify-between p-3 rounded-lg"
                          style={{ backgroundColor: 'var(--color-surface-base)' }}
                        >
                          <div>
                            <p className="text-caption font-medium text-content-primary">
                              {device.device_name || 'Unknown Device'}
                              {device.device_id === config.deviceId && (
                                <span className="ml-2 text-micro text-accent-primary">
                                  (this device)
                                </span>
                              )}
                            </p>
                            <p className="text-micro text-content-tertiary">
                              Last seen: {formatRelativeTime(device.last_seen * 1000)}
                            </p>
                          </div>
                          <span
                            className="w-2 h-2 rounded-full"
                            style={{
                              backgroundColor:
                                device.last_seen * 1000 > Date.now() - 300_000
                                  ? 'var(--color-positive)'
                                  : 'var(--color-content-tertiary)',
                            }}
                          />
                        </div>
                      ))}
                    </div>
                  ) : !statusLoading ? (
                    <p className="text-caption text-content-tertiary">
                      No paired devices found
                    </p>
                  ) : null}
                </div>
              </div>
            </motion.section>
          )}

          {/* Actions */}
          {isConfigured && config && (
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="p-6 bg-surface-raised rounded-xl"
              style={{ boxShadow: 'var(--shadow-sm)' }}
            >
              <h2 className="text-heading font-bold text-content-primary mb-4">Actions</h2>

              <div className="space-y-4">
                {/* Sync Now */}
                <div>
                  <button
                    onClick={handleSyncNow}
                    disabled={syncing}
                    className="w-full py-3 bg-accent-primary text-white rounded-pill text-caption font-semibold hover:bg-accent-primary/90 disabled:opacity-50 transition-colors"
                  >
                    {syncing ? 'Syncing...' : 'Sync Now'}
                  </button>
                  {syncResult && (
                    <p className="text-center text-micro text-content-secondary mt-2">
                      {syncResult}
                    </p>
                  )}
                </div>

                {/* Auto-sync toggle */}
                <div className="flex items-center justify-between p-3 rounded-lg" style={{ backgroundColor: 'var(--color-surface-base)' }}>
                  <div>
                    <p className="text-caption font-medium text-content-primary">Auto-sync</p>
                    <p className="text-micro text-content-tertiary">Sync every 30 seconds</p>
                  </div>
                  <button
                    onClick={handleToggleAutoSync}
                    className={`relative w-12 h-7 rounded-full transition-colors ${
                      autoSync ? 'bg-accent-primary' : 'bg-content-tertiary'
                    }`}
                  >
                    <motion.span
                      className="absolute top-0.5 w-6 h-6 rounded-full bg-white shadow"
                      animate={{ left: autoSync ? 22 : 2 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                    />
                  </button>
                </div>

                {/* Disconnect */}
                <div className="pt-2">
                  <button
                    onClick={handleDisconnect}
                    className="w-full py-3 rounded-pill text-caption font-semibold transition-colors hover:opacity-90"
                    style={{
                      backgroundColor: 'var(--color-negative)',
                      color: 'white',
                    }}
                  >
                    Disconnect
                  </button>
                </div>
              </div>
            </motion.section>
          )}

          {/* Sync Log */}
          <motion.section
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }}
            className="p-6 bg-surface-raised rounded-xl"
            style={{ boxShadow: 'var(--shadow-sm)' }}
          >
            <button
              onClick={() => setShowLog(!showLog)}
              className="flex items-center justify-between w-full"
            >
              <h2 className="text-heading font-bold text-content-primary">Sync Log</h2>
              <span className="text-caption text-content-tertiary">
                {showLog ? '▼' : '▶'}
              </span>
            </button>

            <AnimatePresence>
              {showLog && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="overflow-hidden"
                >
                  <div className="mt-4 space-y-2">
                    {syncLog.length === 0 && (
                      <p className="text-caption text-content-tertiary text-center py-2">
                        No sync events yet
                      </p>
                    )}
                    {syncLog.slice(0, 5).map((entry, i) => (
                      <div
                        key={i}
                        className="flex items-center justify-between p-3 rounded-lg"
                        style={{ backgroundColor: 'var(--color-surface-base)' }}
                      >
                        <div className="flex items-center gap-3">
                          <span
                            className="text-micro font-semibold uppercase w-10"
                            style={{
                              color:
                                entry.type === 'push'
                                  ? 'var(--color-accent-primary)'
                                  : entry.type === 'pull'
                                  ? 'var(--color-positive)'
                                  : 'var(--color-warning)',
                            }}
                          >
                            {entry.type}
                          </span>
                          <span className="text-caption text-content-secondary">
                            {entry.recordCount} records
                          </span>
                        </div>
                        <span className="text-micro text-content-tertiary">
                          {formatRelativeTime(entry.timestamp)}
                        </span>
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.section>
        </div>
      </div>
    </div>
  );
}
