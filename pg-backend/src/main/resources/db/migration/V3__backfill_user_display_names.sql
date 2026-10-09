-- Old registrations could leave the optional display name empty.  Use the
-- unique login username as a safe default while still allowing a nickname later.
UPDATE user
SET userName = userAccount
WHERE userName IS NULL OR TRIM(userName) = '';
