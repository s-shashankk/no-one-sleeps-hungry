const CONTACT = import.meta.env.VITE_CONTACT_EMAIL as string | undefined

export default function Privacy() {
  return (
    <div className="card">
      <h1>Privacy notice</h1>
      <p className="muted">
        Draft text for the pilot. The organisation running helpsomeone.in should review and adapt it before launch.
      </p>
      <h2>What we collect</h2>
      <p>
        When you offer surplus food, report people who need food, volunteer to collect food from a hotel, or
        ask to join the community, we collect what you type: a name, a phone number, a place and, where
        asked, an email address or how much food. If you create a volunteer account we also keep your email and a password (stored only in scrambled form, never readable) and a record of your pickups: which hotel, how many people you said you would feed, and how many plates were served. If you volunteer for a pickup, we share your name and phone
        number with that hotel so the pickup can be arranged. Our team also keeps a list of hotels and
        coworkers with their names, phone numbers and locations.
      </p>
      <h2>Why</h2>
      <p>
        Only to check and coordinate food donations and deliveries. We do not sell data or use it for
        advertising.
      </p>
      <h2>Who can see it</h2>
      <p>Only the helpsomeone.in team, after they sign in (and the staff of the hotel you volunteer to collect from, who see your name, phone number, the number of people and your booking code). The public site shows counts such as meals served, and the names and addresses of partner hotels, but never your details.</p>
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
