// Optional, public contact details. Leave unset until the operator supplies
// real contacts; never substitute clinic or government contact information.
const email = (import.meta.env.VITE_SUPPORT_EMAIL || '').trim()
const phone = (import.meta.env.VITE_SUPPORT_PHONE || '').trim()
export const platformContacts = {
  email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : '',
  phone: /^\+?[\d\s()-]{7,25}$/.test(phone) && phone.replace(/\D/g, '').length >= 7 ? phone : '',
}

// Official resources verified on 2026-10-15. Government language codes match
// the existing ru / kk / en locale codes. The fund link uses its verified RU URL.
export function officialResources(language) {
  return [
    { key: 'ministry', href: `https://www.gov.kz/memleket/entities/dsm?lang=${language}` },
    { key: 'egov', href: 'https://egov.kz/' },
    { key: 'insuranceFund', href: 'https://msqory.kz/ru/' },
    { key: 'insurance', href: `https://www.gov.kz/memleket/entities/dsm/activities/281?lang=${language}` },
  ]
}
