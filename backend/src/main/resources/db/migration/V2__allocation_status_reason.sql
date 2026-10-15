-- Step 6: an allocation can be cancelled or failed, and the reason must be kept (history is never deleted).
ALTER TABLE food_allocations ADD COLUMN status_reason VARCHAR(500) NULL;
