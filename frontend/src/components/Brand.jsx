import AppLink from './AppLink.jsx'

export default function Brand({ onClick }) {
  return <AppLink className="brand-link" href="/" onClick={onClick} aria-label="QamqorMed"><span className="brand-mark" aria-hidden="true">Q</span><span className="brand-name">QamqorMed</span></AppLink>
}
