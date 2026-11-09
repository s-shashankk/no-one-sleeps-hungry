-- Hotel logos shown to the public ("our partners"). Kept in their own table so the vendors table stays light.
-- Only PNG, JPEG and WebP are accepted by the API (checked from the file bytes). version = time of upload, used to refresh caches.
CREATE TABLE vendor_logos (
    vendor_id    BIGINT       NOT NULL PRIMARY KEY,
    content_type VARCHAR(40)  NOT NULL,
    data         MEDIUMBLOB   NOT NULL,
    version      BIGINT       NOT NULL,
    CONSTRAINT fk_vendor_logos_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
