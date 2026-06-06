/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_AUTH_MODE: 'local' | 'cloud'
  readonly VITE_API_BASE: string
  readonly VITE_WX_APPID: string
  readonly VITE_SUBSCRIBE_RECORD_TMPL: string
  readonly VITE_AI_MODE: 'local' | 'cloud'
  readonly VITE_ALLIANCE_MODE: 'local' | 'cloud'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}
