-- SC-06: the cover image is optional since v1.0.0, but databases created by the original code still
-- have NOT NULL on song.image_url (ddl-auto=update never drops it) -> uploads without a cover failed
-- with 500. On a database created by V1 the column is already nullable and this is a no-op.
alter table song alter column image_url drop not null;
