import ApiForm, { Field, LocalitySelect, num } from '../Form.jsx'

export default function Join() {
  return (
    <>
      <h1>Become a volunteer</h1>
      <p className="lead">Help coordinate food in your locality. We will call you before approving.</p>
      <ApiForm
        endpoint="/api/public/volunteer-applications"
        submitLabel="Apply"
        transform={(f) => ({ ...f, localityId: num(f.localityId) })}
      >
        {({ errors }) => (
          <>
            <Field label="Name *" name="name" errors={errors}>
              <input name="name" required maxLength={120} />
            </Field>
            <Field label="Phone *" name="phone" errors={errors}>
              <input name="phone" type="tel" required />
            </Field>
            <Field label="Email" name="email" errors={errors}>
              <input name="email" type="email" maxLength={150} />
            </Field>
            <Field label="Your area / locality *" name="area" errors={errors}>
              <input name="area" required maxLength={150} />
            </Field>
            <LocalitySelect errors={errors} />
            <Field label="College / occupation" name="collegeOrOccupation" errors={errors}>
              <input name="collegeOrOccupation" maxLength={200} />
            </Field>
            <Field label="Anything you'd like to tell us?" name="message" errors={errors}>
              <textarea name="message" rows={3} maxLength={1000} />
            </Field>
          </>
        )}
      </ApiForm>
    </>
  )
}
