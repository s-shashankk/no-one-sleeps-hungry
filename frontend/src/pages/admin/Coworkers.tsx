import Directory from './Directory'

export default function Coworkers() {
  return (
    <Directory config={{
      title: 'Coworkers',
      singular: 'coworker',
      path: '/api/v1/coworkers',
      nameLabel: 'Name',
      locationLabel: 'Location (area they cover)',
      hasContactPerson: false,
      whatsapp: true,
      emptyText: 'No coworkers yet. Add the people you message to collect and distribute food.',
    }} />
  )
}
