# Agent Instructions

- Use forward-only database migrations.
- Do not create `.down.sql` migration files unless explicitly requested.
- Tests create temporary databases named `flotti-test-{timestamp}` and run migrations against them.
- Temporary test databases must be dropped after each test run, including when tests fail normally.
