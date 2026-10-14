import ApiForm, { Field, LocalitySelect, num } from '../Form.jsx'

export default function OfferFood() {
  return (
    <>
      <h1>I have surplus food</h1>
      <p className="lead">Thank you! Tell us what you have and when it must be collected.</p>
      <ApiForm
        endpoint="/api/public/food-offers"
        submitLabel="Offer this food"
        transform={(f) => ({
          vendorName: f.vendorName,
          submitterName: f.submitterName,
          submitterPhone: f.submitterPhone,
          area: f.area,
          localityId: num(f.localityId),
          pickupAddress: f.pickupAddress,
          description: f.description,
          servings: num(f.servings),
          foodType: f.foodType,
          cookedAt: f.cookedAt || null,
          expiresAt: f.expiresAt || null,
          website: f.website,
        })}
      >
        {({ errors }) => (
          <>
            <Field label="Your name *" name="submitterName" errors={errors}>
              <input name="submitterName" required maxLength={120} />
            </Field>
            <Field label="Phone *" name="submitterPhone" errors={errors}>
              <input name="submitterPhone" type="tel" required />
            </Field>
            <Field label="Hotel / restaurant / event (optional)" name="vendorName" errors={errors}>
              <input name="vendorName" maxLength={150} />
            </Field>
            <Field label="Area / locality *" name="area" errors={errors}>
              <input name="area" required maxLength={150} />
            </Field>
            <LocalitySelect errors={errors} />
            <Field label="Pickup address *" name="pickupAddress" errors={errors}>
              <textarea name="pickupAddress" required rows={2} maxLength={500} />
            </Field>
            <Field label="What food is available? *" name="description" errors={errors}>
              <textarea name="description" required rows={3} maxLength={1000}
                placeholder="e.g. Veg biryani, curd rice, sweets" />
            </Field>
            <Field label="Approx. number of servings *" name="servings" errors={errors}>
              <input name="servings" type="number" min="1" required />
            </Field>
            <Field label="Food type *" name="foodType" errors={errors}>
              <select name="foodType" defaultValue="VEG" required>
                <option value="VEG">Vegetarian</option>
                <option value="NON_VEG">Non-vegetarian</option>
                <option value="MIXED">Mixed</option>
              </select>
            </Field>
            <Field label="When was it cooked?" name="cookedAt" errors={errors}>
              <input name="cookedAt" type="datetime-local" />
            </Field>
            <Field label="Safe to eat until *" name="expiresAt" errors={errors}
              hint="After this time we will not collect the food.">
              <input name="expiresAt" type="datetime-local" required />
            </Field>
          </>
        )}
      </ApiForm>
    </>
  )
}
