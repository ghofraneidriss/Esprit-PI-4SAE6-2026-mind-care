-- One-shot: if posts appear in the admin "Recent Communications" but the public /forum is empty,
-- rows were often saved as DRAFT. Run against forum_db after backup:
--   mysql -u root -p forum_db < publish-existing-drafts.sql
UPDATE post
SET status = 'PUBLISHED'
WHERE UPPER(TRIM(COALESCE(status, ''))) = 'DRAFT'
  AND (inactive = 0 OR inactive IS NULL);
