import { writeFileSync, mkdirSync } from 'fs'
import { dirname, join } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const outDir = join(__dirname, '../src/static/tab')
mkdirSync(outDir, { recursive: true })

// Minimal valid 48x48 PNG (grey / brand purple)
const icons = {
  'record.png': 'iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAMElEQVRoge3OMQEAAAjDMMC/5y0g7iQK4XZmZmZmZmZmZmZmZmZmZmZmZmZmZmZm5Dw0wAAE0nBqGAAAAAElFTkSuQmCC',
  'record-active.png': 'iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAMElEQVRoge3OMQEAAAjDMMC/5y0g7iQK4XZmZmZmZmZmZmZmZmZmZmZmZmZmZmZm5Dw0wAAE0nBqGAAAAAElFTkSuQmCC',
  'report.png': 'iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAMElEQVRoge3OMQEAAAjDMMC/5y0g7iQK4XZmZmZmZmZmZmZmZmZmZmZmZmZmZmZm5Dw0wAAE0nBqGAAAAAElFTkSuQmCC',
  'report-active.png': 'iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAMElEQVRoge3OMQEAAAjDMMC/5y0g7iQK4XZmZmZmZmZmZmZmZmZmZmZmZmZmZmZm5Dw0wAAE0nBqGAAAAAElFTkSuQmCC',
  'profile.png': 'iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAMElEQVRoge3OMQEAAAjDMMC/5y0g7iQK4XZmZmZmZmZmZmZmZmZmZmZmZmZmZmZm5Dw0wAAE0nBqGAAAAAElFTkSuQmCC',
  'profile-active.png': 'iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAMElEQVRoge3OMQEAAAjDMMC/5y0g7iQK4XZmZmZmZmZmZmZmZmZmZmZmZmZmZmZm5Dw0wAAE0nBqGAAAAAElFTkSuQmCC',
}

for (const [name, b64] of Object.entries(icons)) {
  writeFileSync(join(outDir, name), Buffer.from(b64, 'base64'))
}

console.log('Tab icons created in src/static/tab/')
