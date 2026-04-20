import { serve } from '@hono/node-server'
import { Hono } from 'hono'

import { pool } from './db.js'

const app = new Hono()

const ansi = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  green: '\x1b[32m',
  cyan: '\x1b[36m',
  yellow: '\x1b[33m',
}

const emphasis = (text: string, color: string) =>
  `${ansi.bold}${color}${text}${ansi.reset}`

app.get('/', (c) => {
  return c.text('Hello Hono!')
})

serve(
  {
    fetch: app.fetch,
    port: 3000,
  },
  async (info) => {
    await pool.query('select 1')

    console.log(
      `🚀 ${emphasis('Server is running', ansi.green)} on ${emphasis(`http://localhost:${info.port}`, ansi.cyan)}`,
    )
    console.log(
      `🗄️  ${emphasis('Database connection is ready', ansi.yellow)}`,
    )
  },
)
