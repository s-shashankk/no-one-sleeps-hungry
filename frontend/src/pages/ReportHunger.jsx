import { useRef } from 'react'
import ApiForm, { Field, LocalitySelect, num } from '../Form.jsx'

export default function ReportHunger() {
  const lat = useRef(null)
  const lng = useRef(null)

  function useMyLocation() {
    if (!navigator.geolocation) return alert('Location is not available on this device.')
    navigator.geolocation.getCurrentPosition(
      (p) => {
        lat.current.value = p.coords.latitude.toFixed(6)
        lng.current.value = p.coords.longitude.toFixed(6)
      },
      () => alert('Could not get your location. You can still describe the place below.')
    )
  }

  return (
    <>
      <h1>Someone needs food</h1>
      <p className="lead">Tell us where people are hungry. We will try to reach them.</p>
      <ApiForm
        endpoint="/api/public/hunger-reports"
        submitLabel="Send report"
        transform={(f) => ({
          area: f.area,
          localityId: num(f.localityId),
          locationText: f.locationText,
          latitude: num(f.latitude),
          longitude: num(f.longitude),
          peopleCount: num(f.peopleCount),
          notes: f.notes,
          reporterName: f.reporterName,
          reporterPhone: f.reporterPhone,
          website: f.website,
        })}
      >
        {({ errors }) => (
          <>
            <Field label="Area / locality *" name="area" errors={errors}>
              <input name="area" required maxLength={150} />
            </Field>
            <LocalitySelect errors={errors} />
            <Field label="Where are they? *" name="locationText" errors={errors}
              hint="Landmark, street, bus stop, temple, etc.">
              <textarea name="locationText" required rows={2} maxLength={500} />
            </Field>
            <button type="button" className="btn" onClick={useMyLocation}>📍 Use my current location</button>
            <input ref={lat} name="latitude" type="hidden" />
            <input ref={lng} name="longitude" type="hidden" />
            <Field label="Approx. number of people *" name="peopleCount" errors={errors}>
              <input name="peopleCount" type="number" min="1" required />
            </Field>
            <Field label="Anything else we should know?" name="notes" errors={errors}>
              <textarea name="notes" rows={3} maxLength={1000}
                placeholder="Children, elderly, best time to reach them…" />
            </Field>
            <Field label="Your name (optional)" name="reporterName" errors={errors}>
              <input name="reporterName" maxLength={120} />
            </Field>
            <Field label="Your phone (optional)" name="reporterPhone" errors={errors}
              hint="Only coordinators see this, so they can confirm the details.">
              <input name="reporterPhone" type="tel" />
            </Field>
          </>
        )}
      </ApiForm>
    </>
  )
}
