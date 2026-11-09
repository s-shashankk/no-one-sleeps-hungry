import Directory from './Directory'

export default function Vendors() {
  return (
    <Directory config={{
      title: 'Hotels',
      singular: 'hotel',
      path: '/api/v1/vendors',
      nameLabel: 'Hotel / vendor name',
      locationLabel: 'Address (shown to volunteers)',
      hasContactPerson: true,
      hasArea: true,
      whatsapp: true,
      emptyText: 'No hotels yet. Add the hotels and caterers you call for surplus food, and choose their area so volunteers can find them.',
    }} />
  )
}
