package com.mealup.vendors;

/** Published when a hotel's logo is saved or removed, so cached public pages can refresh themselves. */
public record LogoChangedEvent(long vendorId) { }
