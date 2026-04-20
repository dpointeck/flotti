# Flotti Domain Design

## Goal

`flotti` is a simple invoicing app for artisans or freelancers.

Primary workflow:
- create an invoice for a customer

Secondary workflow:
- track time on projects
- optionally pull time entries into invoice lines
- still allow fully manual invoices with no time tracking

## Scope Decision

The app should support users logging into multiple businesses.

Recommended approach:
- model multi-business memberships now
- do not build full tenancy infrastructure yet
- scope records in Rails through the current business
- avoid subdomains, separate databases, and tenant gems in v1

This keeps the domain correct without adding early operational complexity.

## Top-Level Entities

- `User`
- `Business`
- `Membership`

Meaning:
- a `User` can belong to many `Businesses`
- a `Business` can have many `Users` through `Memberships`
- a `Business` owns all business data like customers, projects, invoices, and payments

## Core Business Entities

- `Customer`
- `Project`
- `TimeEntry`
- `Invoice`
- `InvoiceLine`
- `Payment`

## Relationships

- `User has_many :memberships`
- `Business has_many :memberships`
- `Business has_many :customers`
- `Business has_many :projects`
- `Business has_many :invoices`
- `Business has_many :payments`
- `Customer belongs_to :business`
- `Project belongs_to :business`
- `Project belongs_to :customer, optional: true`
- `TimeEntry belongs_to :project`
- `TimeEntry belongs_to :invoice_line, optional: true`
- `Invoice belongs_to :business`
- `Invoice belongs_to :customer`
- `Invoice belongs_to :project, optional: true`
- `Invoice has_many :invoice_lines`
- `Invoice has_many :payments`

## Billing Model

Recommended model:
- invoices are the source of truth
- time tracking is optional input
- invoice lines can be manual or generated from selected time entries
- once time entries are pulled into an invoice, the invoice keeps its own snapshot
- editing or deleting later time entries must not rewrite historical invoices

This gives maximum flexibility:
- manual invoices remain simple
- project-based billing is supported
- time tracking can be ignored when not needed

## Important Rules

- issued invoices should become effectively immutable
- invoice totals should not be derived live from projects or time entries after issuing
- business data should be scoped by the current business in the application layer

## Important Concerns Often Forgotten

- invoice numbering per business
- invoice statuses such as `draft`, `issued`, `paid`, `overdue`, `void`
- sender legal details on the invoice
- customer and business snapshots on invoices
- due dates and payment terms
- taxes or VAT handling
- separate payment records instead of only a paid flag
- PDF or rendered document output
- notes and internal notes
- currency support
- reminder state
- credit notes or cancellation flow

## Recommended v1 Shape

- keep `Business` as both tenant and issuer profile
- do not introduce a separate `BusinessProfile` model yet
- allow invoice lines to optionally reference imported time entries
- keep the invoice as the historical record once it is issued

## Quotes And Estimates

Current workflow:
- quotes and estimates are usually created in Google Docs
- they are sent by email outside the app

Decision for v1:
- do not model quotes or estimates yet
- keep the invoicing scope focused

Possible later evolution:
- add `Quote` only when the external workflow becomes painful enough to replace
