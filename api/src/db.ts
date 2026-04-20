import { config } from 'dotenv'
import { drizzle } from 'drizzle-orm/node-postgres'
import { Pool } from 'pg'

import { singleton } from './singleton.js'

config({ path: '.env.local' })
config()

const databaseUrl = process.env.DATABASE_URL

if (!databaseUrl) {
  throw new Error('DATABASE_URL is not set. Add it to .env.local')
}

const pool = singleton(
  'pg-pool',
  () =>
    new Pool({
      connectionString: databaseUrl,
    }),
)

export const db = drizzle({ client: pool })
export { pool }
