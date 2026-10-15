import { RequestStatus, STATUS_LABEL } from '../types'

/** Text first, colour second: the label always says the status. */
export default function StatusBadge({ status }: { status: RequestStatus }) {
  const tone = status === 'COMPLETED' || status === 'ACCEPTED' ? 'on' : status === 'REJECTED' ? 'off' : ''
  return <em className={'badge ' + tone}>{STATUS_LABEL[status]}</em>
}
