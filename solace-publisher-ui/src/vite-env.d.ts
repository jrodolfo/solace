/**
 * Type definitions for Vite's client-side features.
 */
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_BROKER_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
