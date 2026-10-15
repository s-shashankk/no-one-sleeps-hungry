import Directory from './Directory'

export default function Vendors() {
  return (
    <Directory config={{
      title: 'Vendors',
      singular: 'vendor',
      path: '/api/v1/vendors',
      nameLabel: 'Hotel / vendor name',
      locationLabel: 'Location (address or area)',
      hasContactPerson: true,
      whatsapp: false,
      emptyText: 'No vendors yet. Add the hotels and caterers you call for surplus food.',
    }} />
  )
}
