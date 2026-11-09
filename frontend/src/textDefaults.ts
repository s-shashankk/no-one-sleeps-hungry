/**
 * Every sentence on the public pages that the admin may change (Admin > Site text).
 * The text written here is what visitors see until the admin edits it. An edited text is stored in the database
 * under the same key, and clearing it brings this wording back. Sizes and layout never change, only the words.
 */
export interface TextField {
  key: string
  label: string
  text: string
  long?: boolean
}

export interface TextGroup {
  title: string
  hint?: string
  fields: TextField[]
}

export const TEXT_GROUPS: TextGroup[] = [
  {
    title: 'Home page: the top',
    hint: 'The first thing every visitor reads.',
    fields: [
      { key: 'home.hero.title', label: 'Main heading', text: "Somewhere in Bengaluru, a child is waiting for tonight's meal." },
      { key: 'home.hero.text', label: 'Paragraph under the heading', long: true,
        text: 'A few streets away, good food is about to be thrown out. You can bring the two together: carry one warm plate from a kind hotel to someone who has nothing, and ask for nothing but a blessing.' },
      { key: 'home.hero.feed', label: 'Yellow button', text: 'I want to feed someone' },
      { key: 'home.hero.hungry', label: 'Outline button', text: 'Someone is hungry' },
      { key: 'home.hero.note', label: 'Small line under the buttons', text: 'Starting in Indiranagar, with love. Always free for those who cannot afford a meal.' },
    ],
  },
  {
    title: 'Home page: who we are for',
    hint: 'The dark blue section with the three drawings.',
    fields: [
      { key: 'home.who.title', label: 'Heading', text: 'For the ones who have no one to ask.' },
      { key: 'home.who.lead', label: 'Paragraph', long: true,
        text: 'The child who sleeps with an empty stomach. The grandmother who waits by the roadside. The family that shared one roti between four. helpsomeone.in exists for them, so that good food reaches their hands instead of the bin.' },
      { key: 'home.story1.title', label: 'First drawing: title', text: 'A child who waits' },
      { key: 'home.story1.text', label: 'First drawing: text', long: true, text: 'Some children in our city go to sleep without a proper meal. For them, one plate is the whole day.' },
      { key: 'home.story2.title', label: 'Second drawing: title', text: 'An elder with no one' },
      { key: 'home.story2.text', label: 'Second drawing: text', long: true, text: "Many older people live alone on the streets. They wait for a stranger's kindness, and often for a long time." },
      { key: 'home.story3.title', label: 'Third drawing: title', text: 'A family under a flyover' },
      { key: 'home.story3.text', label: 'Third drawing: text', long: true, text: 'Daily-wage families sometimes must choose between a roof and dinner. A warm meal lets them sleep without fear.' },
      { key: 'home.who.foot', label: 'Small line at the bottom', text: 'These are drawings, out of respect. We never photograph anyone without their blessing.' },
    ],
  },
  {
    title: 'Home page: three ways to help',
    fields: [
      { key: 'home.paths.title', label: 'Section heading', text: 'Pick the way your heart wants to help' },
      { key: 'home.path1.title', label: 'First card: title', text: 'Carry a meal to someone' },
      { key: 'home.path1.text', label: 'First card: text', long: true, text: 'Choose a kind hotel near you. We will tell them you are coming, and you bring the food to those waiting.' },
      { key: 'home.path1.go', label: 'First card: link', text: 'Book a pickup' },
      { key: 'home.path2.title', label: 'Second card: title', text: 'Tell us who is hungry' },
      { key: 'home.path2.text', label: 'Second card: text', long: true, text: 'Have you seen someone who has not eaten and cannot pay for a meal? Tell us where, and we will not let them be forgotten.' },
      { key: 'home.path2.go', label: 'Second card: link', text: 'Report someone hungry' },
      { key: 'home.path3.title', label: 'Third card: title', text: 'Share what you have left' },
      { key: 'home.path3.text', label: 'Third card: text', long: true, text: 'Cooked too much, or have food left after an event? Let it feed someone instead of the bin.' },
      { key: 'home.path3.go', label: 'Third card: link', text: 'Share your food' },
    ],
  },
  {
    title: 'Home page: how it works',
    fields: [
      { key: 'home.how.title', label: 'Section heading', text: 'How one plate becomes a smile' },
      { key: 'home.how1.title', label: 'Step 1: title', text: 'A kitchen sets food aside' },
      { key: 'home.how1.text', label: 'Step 1: text', long: true, text: 'Our hotel friends keep good, fresh food for those who need it.' },
      { key: 'home.how2.title', label: 'Step 2: title', text: 'A kind person goes to collect it' },
      { key: 'home.how2.text', label: 'Step 2: text', long: true, text: 'You pick the hotel and tell us how many people are waiting. We let the hotel know and call you.' },
      { key: 'home.how3.title', label: 'Step 3: title', text: 'Someone eats, and smiles' },
      { key: 'home.how3.text', label: 'Step 3: text', long: true, text: 'You hand over the food. We count every plate, because every plate was a life touched.' },
    ],
  },
  {
    title: 'Home page: hotels, areas and the invitation',
    fields: [
      { key: 'home.partners.title', label: 'Hotels slider: heading', text: 'Kitchens with big hearts' },
      { key: 'home.partners.lead', label: 'Hotels slider: paragraph', long: true,
        text: 'These hotels quietly set aside food that would have been thrown away, so someone else can eat. Thank you, every one of you.' },
      { key: 'home.areas.title', label: 'Areas: heading', text: 'Where we are walking together' },
      { key: 'home.areas.lead', label: 'Areas: paragraph', text: 'More streets of Bengaluru will open as more kind hearts join us.' },
      { key: 'home.areas.link', label: 'Areas: link to the numbers', text: 'See the full numbers' },
      { key: 'home.band.title', label: 'Green invitation: heading', text: 'No one should have to beg for a meal.' },
      { key: 'home.band.text', label: 'Green invitation: paragraph', long: true,
        text: 'Own a kitchen? Have a free evening each week? Or simply a heart that wants to help? Tell us your name and we will call you. We do not ask for anything in return. Their blessings are enough.' },
      { key: 'home.band.button', label: 'Green invitation: button', text: 'Walk with us' },
    ],
  },
  {
    title: 'About page',
    fields: [
      { key: 'about.intro', label: 'Opening paragraph', long: true,
        text: "helpsomeone.in began with a simple, painful thought: good food is thrown away in one street, while a child sleeps hungry in the next. So we ask kind hotels to set aside what they cannot serve, and kind people like you to carry it to those who have nothing. We stay in touch by phone and WhatsApp, and we count every plate, because every plate is someone's evening made lighter." },
      { key: 'about.start', label: 'Where we start', long: true, text: 'We are starting in Indiranagar, and with your help we will reach Vijayanagar, Domlur and more of Bengaluru.' },
      { key: 'about.serve', label: 'Who we serve', long: true,
        text: 'Children, elders, daily-wage workers and families who cannot afford a meal. helpsomeone.in is for them, with dignity and without questions. We look at each report with care so that food reaches those who truly need it.' },
      { key: 'about.safety', label: 'Food safety', long: true,
        text: 'We only move food that is fresh and was kept properly. Hotels decide what they hand over, and volunteers should deliver it quickly. Donated surplus food is handled on a best-effort basis.' },
      { key: 'about.privacy', label: 'Privacy', long: true,
        text: 'Your name, number and place are kept safe. Only the helpsomeone.in team (and the hotel you are going to) can see them. We never show them to the public.' },
    ],
  },
  {
    title: 'Bottom of every page',
    fields: [
      { key: 'footer.tagline', label: 'Footer line', text: 'Good food, shared before it is wasted. Starting in Indiranagar, Bengaluru.' },
    ],
  },
]

export const DEFAULT_TEXT: Record<string, string> = Object.fromEntries(
  TEXT_GROUPS.flatMap((g) => g.fields.map((f) => [f.key, f.text] as const)),
)
