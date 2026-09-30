/**
 * 与后端 ResponseCryptoService 对齐的 AES-GCM 解密。
 */
const DEFAULT_KEY = 'bVWm2iTKcLbx5HCspnoLFtVsQCFOauJS'

function normalizeKeyBytes(raw) {
  const enc = new TextEncoder()
  const src = enc.encode(raw || DEFAULT_KEY)
  if (src.length === 16 || src.length === 24 || src.length === 32) {
    return src
  }
  const out = new Uint8Array(32)
  out.set(src.subarray(0, Math.min(src.length, 32)))
  return out
}

function b64ToBytes(b64) {
  const bin = atob(b64)
  const out = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i)
  return out
}

let cachedKeyPromise = null

function importKey() {
  if (!cachedKeyPromise) {
    const keyBytes = normalizeKeyBytes(import.meta.env.VITE_API_AES_KEY || DEFAULT_KEY)
    cachedKeyPromise = crypto.subtle.importKey('raw', keyBytes, { name: 'AES-GCM' }, false, ['decrypt'])
  }
  return cachedKeyPromise
}

export async function decryptApiData(cipherB64) {
  if (cipherB64 == null || cipherB64 === '') return null
  const raw = b64ToBytes(cipherB64)
  if (raw.length < 13) throw new Error('密文长度异常')
  const iv = raw.slice(0, 12)
  const data = raw.slice(12)
  const key = await importKey()
  const plainBuf = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, key, data)
  const text = new TextDecoder().decode(plainBuf)
  return JSON.parse(text)
}
