# QamqorMed medical catalog sources

The geography snapshot follows the Bureau of National Statistics publication
“Administrative-territorial units of the Republic of Kazakhstan as of 1 July 2026”
and the current KATO classifier. The publication reports 17 regions, 3 cities of
republican significance, and 90 cities.

The medical organization import contract follows the public eGov.kz dataset
“Medical organizations” (`medical_organizations`, weekly update, active/deleted
status; API: `https://data.egov.kz/api/v4/medical_organizations/v1`).
`clinics.json` is a curated official-name snapshot used for local development;
the optional external importer accepts an eGov-compatible export with `id`,
`name`/`title`/`orgName`, `address`, city and status fields and excludes deleted
rows. Names of clinicians are never read from the source. The service stores
`source`, `externalSourceId`, `isReal`, and `active` on every imported organization.

Clinic-to-specialty links in this educational snapshot are explicitly marked
`demo=true` because the organization dataset does not publish service-level
specialties. Doctors and nurses are generated demo accounts and are not claimed
to be employees of the real organizations.

`FULL CLINIC IMPORT NOT COMPLETED`: the complete eGov register cannot be
retrieved from this repository without an eGov API key, so the 31-row local
snapshot must not be presented as the full Kazakhstan clinic register. Set
`QAMQORMED_EGOV_CLINICS_FILE` to an eGov-compatible JSON export to import the
available full dataset. The dev importer is idempotent, skips inactive/deleted
rows, and then generates the complete active `Clinic × Specialty` Cartesian
catalog with one DEMO doctor, one DEMO nurse, and one assignment per pair.
