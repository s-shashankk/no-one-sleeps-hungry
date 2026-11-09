-- Words on the public pages that the admin can change. A row only exists for text the admin has edited;
-- the website falls back to its built-in wording for everything else.
CREATE TABLE site_texts (
  text_key    VARCHAR(80)   NOT NULL PRIMARY KEY,
  text_value  VARCHAR(1500) NOT NULL,
  updated_at  DATETIME(6)   NOT NULL
);
