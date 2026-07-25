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
      <div className="min-h-screen bg-lavender-bg flex items-center justify-center">
        <p className="text-[11px] text-ink-mute">Loading...</p>
      </div>
    );
  }

  const isConfigured = config !== null;
  const lastSyncText = config?.lastSyncAt ? formatRelativeTime(config.lastSyncAt) : 'Never';

  return (
    <div className="min-h-screen bg-lavender-bg">
      <div className="max-w-2xl mx-auto p-4">
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="text-ink-soft text-[13px] hover:text-ink-1 transition-colors"
          >
            ← Back
          </button>
          <h1 className="text-sm font-bold text-ink-soft uppercase tracking-wide">Sync Settings</h1>
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
                className={`px-6 py-3 rounded-card text-[11px] font-medium shadow-card ${
                  toast.type === 'success'
                    ? 'bg-green-bg text-green-1'
                    : 'bg-coral-bg text-coral-1'
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
            className="p-6 rounded-card shadow-card bg-[var(--surface)]"
          >
            <div className="flex items-center gap-3 mb-2">
              <span
                className="w-3 h-3 rounded-full"
                style={{
                  backgroundColor: isConfigured
                    ? 'var(--green)'
                    : 'var(--ink-mute)',
                }}
              />
              <span className="text-sm font-bold text-ink-1">
                {isConfigured ? 'Sync is active' : 'Sync not configured'}
              </span>
            </div>
            <p className="text-[11px] text-ink-mute">
              Last synced: {lastSyncText}
            </p>
          </motion.section>

          {/* Setup Section (not configured) */}
          {!isConfigured && (
            <motion.section
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05 }}
              className="p-6 rounded-card shadow-card bg-[var(--surface)]"
            >
              <h2 className="text-sm font-bold text-ink-soft uppercase tracking-wide mb-4">Setup Sync</h2>

              <label className="block text-[11px] font-medium text-ink-mute mb-1.5">
                Server URL
              </label>
              <input
                type="text"
                placeholder="https://sync.nudge.app"
                value={serverUrl}
                onChange={(e) => setServerUrl(e.target.value)}
                className="w-full p-3 bg-lavender-bg rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute mb-4"
              />

              {/* Register */}
              <div className="p-5 rounded-card bg-purple-bg/40 mb-4">
                <h3 className="text-[13px] font-semibold text-ink-1 mb-1">
                  Register New Device
                </h3>
                <p className="text-[11px] text-ink-mute mb-4">
                  Create a new sync identity and get a pairing code to share with other devices.
                </p>
                <input
                  type="text"
                  placeholder="Device name (e.g. Chrome Desktop)"
                  value={deviceName}
                  onChange={(e) => setDeviceName(e.target.value)}
                  className="w-full p-2.5 bg-lavender-bg rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute mb-3"
                />
                <button
                  onClick={handleRegister}
                  disabled={registering || !serverUrl.trim() || !deviceName.trim()}
                  className="w-full py-2.5 bg-gradient-to-r from-purple-1 to-purple-2 text-white rounded-pill text-[13px] font-semibold disabled:opacity-50 transition-opacity"
                >
                  {registering ? 'Registering...' : 'Register New Device'}
                </button>
              </div>

              {/* Pair */}
              <div className="p-5 rounded-card bg-purple-bg/40">
                <h3 className="text-[13px] font-semibold text-ink-1 mb-1">
                  Pair with Existing Device
                </h3>
                <p className="text-[11px] text-ink-mute mb-4">
                  Enter the pairing code from another device to link them.
                </p>
                <label className="block text-[10px] font-medium text-ink-mute mb-1">
                  Pairing Code
                </label>
                <input
                  type="text"
                  placeholder="ABC-123"
                  value={formatPairingCode(pairingInput)}
                  onChange={handlePairingCodeChange}
                  maxLength={7}
                  className="w-full p-2.5 bg-lavender-bg rounded-chip outline-none text-[13px] tracking-[0.3em] font-mono text-ink-1 placeholder:text-ink-mute mb-3 text-center"
                />
                <label className="block text-[10px] font-medium text-ink-mute mb-1">
                  Device Name
                </label>
                <input
                  type="text"
                  placeholder="My Phone"
                  value={pairDeviceName}
                  onChange={(e) => setPairDeviceName(e.target.value)}
                  className="w-full p-2.5 bg-lavender-bg rounded-chip outline-none text-[13px] text-ink-1 placeholder:text-ink-mute mb-3"
                />
                <button
                  onClick={handlePair}
                  disabled={
                    pairing ||
                    !serverUrl.trim() ||
                    pairingInput.replace(/[^a-zA-Z0-9]/g, '').length !== 6 ||
                    !pairDeviceName.trim()
                  }
                  className="w-full py-2.5 bg-gradient-to-r from-purple-1 to-purple-2 text-white rounded-pill text-[13px] font-semibold disabled:opacity-50 transition-opacity"
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
              className="p-6 rounded-card shadow-card bg-[var(--surface)]"
            >
              <h2 className="text-sm font-bold text-ink-soft uppercase tracking-wide mb-4">
                Connection Details
              </h2>

              <div className="space-y-4">
                {/* Server URL */}
                <div>
                  <label className="text-[10px] font-medium text-ink-mute block mb-1">
                    Server URL
                  </label>
                  {editingServerUrl ? (
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={editedServerUrl}
                        onChange={(e) => setEditedServerUrl(e.target.value)}
                        className="flex-1 p-2 bg-lavender-bg rounded-chip outline-none text-[13px] text-ink-1"
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') handleSaveServer();
                        }}
                      />
                      <button
                        onClick={handleSaveServer}
                        className="px-3 py-2 bg-gradient-to-r from-purple-1 to-purple-2 text-white rounded-chip text-[13px] font-semibold"
                      >
                        Save
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between">
                      <span className="text-[13px] text-ink-1 font-mono">
                        {config.serverUrl}
                      </span>
                      <button
                        onClick={handleEditServer}
                        className="text-[11px] text-purple-1 hover:text-purple-2"
                      >
                        Edit
                      </button>
                    </div>
                  )}
                </div>

                {/* Device ID */}
                <div>
                  <label className="text-[10px] font-medium text-ink-mute block mb-1">
                    Device ID
                  </label>
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] text-ink-1 font-mono">
                      {config.deviceId.slice(0, 16)}...
                    </span>
                    <button
                      onClick={() => {
                        copyToClipboard(config.deviceId, () => {
                          setCopied(true);
                          showToast('Device ID copied', 'success');
                        });
                      }}
                      className="text-[11px] text-purple-1 hover:text-purple-2"
                    >
                      {copied ? 'Copied!' : 'Copy'}
                    </button>
                  </div>
                </div>

                {/* Pairing Code */}
                <div>
                  <label className="text-[10px] font-medium text-ink-mute block mb-1">
                    Pairing Code
                  </label>
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-ink-1 font-mono tracking-[0.3em]">
                      {formatPairingCode(config.pairingCode)}
                    </span>
                    <button
                      onClick={() => {
                        copyToClipboard(config.pairingCode, () => {
                          setCopied(true);
                          showToast('Pairing code copied', 'success');
                        });
                      }}
                      className="text-[11px] text-purple-1 hover:text-purple-2"
                    >
                      {copied ? 'Copied!' : 'Copy'}
                    </button>
                  </div>
                </div>

                {/* Paired Devices */}
                <div>
                  <label className="text-[10px] font-medium text-ink-mute block mb-2">
                    Paired Devices
                  </label>
                  {statusLoading && (
                    <p className="text-[11px] text-ink-mute">Loading...</p>
                  )}
                  {!statusLoading && statusInfo?.devices && statusInfo.devices.length > 0 ? (
                    <div className="space-y-2">
                      {statusInfo.devices.map((device) => (
                        <div
                          key={device.device_id}
                          className="flex items-center justify-between p-3 rounded-card bg-purple-bg/40"
                        >
                          <div>
                            <p className="text-[13px] font-medium text-ink-1">
                              {device.device_name || 'Unknown Device'}
                              {device.device_id === config.deviceId && (
                                <span className="ml-2 text-[10px] text-purple-1">
                                  (this device)
                                </span>
                              )}
                            </p>
                            <p className="text-[10px] text-ink-mute">
                              Last seen: {formatRelativeTime(device.last_seen * 1000)}
                            </p>
                          </div>
                          <span
                            className="w-2 h-2 rounded-full"
                            style={{
                              backgroundColor:
                                device.last_seen * 1000 > Date.now() - 300_000
                                  ? 'var(--green)'
                                  : 'var(--ink-mute)',
                            }}
                          />
                        </div>
                      ))}
                    </div>
                  ) : !statusLoading ? (
                    <p className="text-[11px] text-ink-mute">
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
              className="p-6 rounded-card shadow-card bg-[var(--surface)]"
            >
              <h2 className="text-sm font-bold text-ink-soft uppercase tracking-wide mb-4">Actions</h2>

              <div className="space-y-4">
                {/* Sync Now */}
                <div>
                  <button
                    onClick={handleSyncNow}
                    disabled={syncing}
                    className="w-full py-3 bg-gradient-to-r from-purple-1 to-purple-2 text-white rounded-pill text-[13px] font-semibold disabled:opacity-50 transition-opacity"
                  >
                    {syncing ? 'Syncing...' : 'Sync Now'}
                  </button>
                  {syncResult && (
                    <p className="text-center text-[10px] text-ink-mute mt-2">
                      {syncResult}
                    </p>
                  )}
                </div>

                {/* Auto-sync toggle */}
                <div className="flex items-center justify-between p-3 rounded-card bg-purple-bg/40">
                  <div>
                    <p className="text-[13px] font-medium text-ink-1">Auto-sync</p>
                    <p className="text-[10px] text-ink-mute">Sync every 30 seconds</p>
                  </div>
                  <button
                    onClick={handleToggleAutoSync}
                    className={`relative w-12 h-7 rounded-full transition-colors ${
                      autoSync ? 'bg-purple-1' : 'bg-ink-mute'
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
                <div className="p-4 rounded-card bg-coral-bg border border-coral-1/20">
                  <p className="text-[11px] text-coral-1 mb-3">
                    Disconnect this device from sync. You can reconnect later.
                  </p>
                  <button
                    onClick={handleDisconnect}
                    className="w-full py-3 rounded-pill text-[13px] font-semibold bg-coral-1 text-white transition-opacity hover:opacity-90"
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
            className="p-6 rounded-card shadow-card bg-[var(--surface)]"
          >
            <button
              onClick={() => setShowLog(!showLog)}
              className="flex items-center justify-between w-full"
            >
              <h2 className="text-sm font-bold text-ink-soft uppercase tracking-wide">Sync Log</h2>
              <span className="text-[11px] text-ink-mute">
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
                      <p className="text-[11px] text-ink-mute text-center py-2">
                        No sync events yet
                      </p>
                    )}
                    {syncLog.slice(0, 5).map((entry, i) => (
                      <div
                        key={i}
                        className="flex items-center justify-between p-3 rounded-card bg-purple-bg/40"
                      >
                        <div className="flex items-center gap-3">
                          <span
                            className="text-[10px] font-semibold uppercase w-10"
                            style={{
                              color:
                                entry.type === 'push'
                                  ? 'var(--purple)'
                                  : entry.type === 'pull'
                                  ? 'var(--green)'
                                  : 'var(--coral)',
                            }}
                          >
                            {entry.type}
                          </span>
                          <span className="text-[11px] text-ink-soft">
                            {entry.recordCount} records
                          </span>
                        </div>
                        <span className="text-[10px] text-ink-mute">
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
