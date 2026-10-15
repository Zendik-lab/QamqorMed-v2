import { Component } from 'react'
import { ErrorState } from './ui/Feedback.jsx'

// Do not log exception payloads: page errors can contain medical data.
export default class RouteErrorBoundary extends Component {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  render() {
    return this.state.failed ? <ErrorState onRetry={() => window.location.reload()} /> : this.props.children
  }
}
