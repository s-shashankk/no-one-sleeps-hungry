const CONTACT = import.meta.env.VITE_CONTACT_EMAIL as string | undefined

export default function Privacy() {
  return (
    <div className="card">
      <h1>Privacy notice</h1>
      <p className="muted">
        Draft text for the pilot. The organisation running MealUp should review and adapt it before launch.
      </p>
      <h2>What we collect</h2>
      <p>
        When you offer surplus food or report people who need food, we collect what you type: a name, a
        phone number, a place and how much food or how many people. Our team also keeps a list of vendors
        and coworkers with their names, phone numbers and locations.
      </p>
      <h2>Why</h2>
      <p>
        Only to check and coordinate food donations and deliveries. We do not sell data or use it for
        advertising.
      </p>
      <h2>Who can see it</h2>
      <p>Only the MealUp team, after they sign in. Nothing you send is shown publicly.</p>
      <h2>How long we keep it</h2>
      <p>
        Records are kept while the initiative is running. Ask us to correct or delete your details at any
        time.
      </p>
      <h2>Contact</h2>
      <p>
        {CONTACT
          ? <>Email <a href={`mailto:${CONTACT}`}>{CONTACT}</a>.</>
          : 'Please contact the team that shared this link with you.'}
      </p>
    </div>
  )
}
