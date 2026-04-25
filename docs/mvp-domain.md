# Flotti MVP Domain

Flotti starts as a simple invoicing tool for solo workers in Austria.

## Product Focus

**Goal:** create customers and send simple invoices fast.

```text
Customers. Invoices. Done.
```

## MVP Scope

### In

- Manage own business profile
- Create customers
- Create draft invoices
- Add invoice line items
- Issue invoices with invoice numbers
- Generate/download invoice PDF
- Mark invoices as paid

### Out

- Projects
- Quotes/offers
- Time tracking
- Expenses
- Reminders/dunning
- Product catalog
- Recurring invoices
- Multi-currency

## Core Flow

```text
Organization -> Customer -> Invoice -> InvoiceItem
```

1. User creates organization profile
2. User creates customer
3. User creates draft invoice
4. User adds invoice items
5. User issues invoice
6. System assigns invoice number
7. User downloads PDF
8. User marks invoice as paid

## Entities

### Organization

Own business profile.

Fields:

- `id`
- `user_id`
- `name`
- `legal_name`
- `address_line1`
- `address_line2`
- `postal_code`
- `city`
- `country` default `AT`
- `vat_id`
- `tax_number`
- `iban`
- `bic`
- `default_currency` default `EUR`
- `default_tax_rate` default `20`
- `invoice_number_prefix`
- `next_invoice_number`
- `created_at`
- `updated_at`

### Customer

Invoice recipient.

Fields:

- `id`
- `organization_id`
- `name`
- `company_name`
- `email`
- `phone`
- `address_line1`
- `address_line2`
- `postal_code`
- `city`
- `country`
- `vat_id`
- `notes`
- `created_at`
- `updated_at`

### Invoice

Customer invoice.

Fields:

- `id`
- `organization_id`
- `customer_id`
- `invoice_number`
- `status`
- `subject`
- `issue_date`
- `due_date`
- `service_period_start`
- `service_period_end`
- `paid_at`
- `currency`
- `subtotal_amount`
- `tax_amount`
- `total_amount`
- `payment_terms`
- `notes`
- `created_at`
- `updated_at`

Statuses:

```text
draft -> issued -> paid
              -> cancelled
```

### InvoiceItem

Invoice line item.

Fields:

- `id`
- `invoice_id`
- `position`
- `description`
- `quantity`
- `unit`
- `unit_price`
- `tax_rate`
- `subtotal_amount`
- `tax_amount`
- `total_amount`
- `created_at`
- `updated_at`

## Project Decision

Do **not** build `Project` yet.

Current real workflow:

```text
Customer -> Invoice
```

Use `invoice.subject` for lightweight project-like context, e.g.:

- `Website relaunch`
- `Consulting April 2026`
- `Repair work bathroom`

Add real projects later only when the workflow actually needs them.

## Austria Notes

Invoices should support:

- own business name/address
- customer name/address
- invoice date
- unique invoice number
- service description
- service date or service period
- net amount
- VAT rate
- VAT amount
- gross amount
- VAT ID/tax number where needed
- small-business notice later if required
