import { useLayoutEffect, useRef, useState } from 'react'
import { playMotion } from './motion.js'

// Replay visual changes on the same DOM node, preserving forms, focus and keys.
export function useMotion(ref, replayKey, variant = 'fade') {
  useLayoutEffect(() => {
    const animation = playMotion(ref.current, variant)
    return () => animation?.cancel()
  }, [ref, replayKey, variant])
}

export function MotionRegion({ children, replayKey, variant = 'fade', className = '', ...props }) {
  const ref = useRef(null)
  useMotion(ref, replayKey, variant)
  return <div ref={ref} className={`motion-region ${className}`} {...props}>{children}</div>
}

// Retain only the visual panel during its short exit, never the action/request.
// Reopening cancels the old exit and retains the same content and input values.
export function usePresence(open, ref, variant = 'panel') {
  const [retained, setRetained] = useState(open)
  const present = open || retained
  useLayoutEffect(() => {
    if (!present) return undefined
    let active = true
    if (open) setRetained(true)
    const animation = playMotion(ref.current, variant, { exiting: !open })
    if (!open) {
      if (animation) animation.finished.then(() => { if (active) setRetained(false) })
      else setRetained(false)
    }
    return () => { active = false; animation?.cancel() }
  }, [open, present, ref, variant])
  return present
}
