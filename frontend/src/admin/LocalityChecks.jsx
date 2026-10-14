// A row of checkboxes for picking localities. `value` is an array of ids.
export default function LocalityChecks({ localities, value, onChange }) {
  if (localities.length === 0) return <p className="hint">No localities yet. Add one in the Localities tab.</p>
  const toggle = (id) => onChange(value.includes(id) ? value.filter((x) => x !== id) : [...value, id])
  return (
    <div className="checks">
      {localities.map((l) => (
        <label key={l.id} className="check">
          <input type="checkbox" checked={value.includes(l.id)} onChange={() => toggle(l.id)} />
          {l.name}, {l.city}
        </label>
      ))}
    </div>
  )
}
