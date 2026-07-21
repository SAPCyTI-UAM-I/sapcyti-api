-- HU-59/HU-60: observations are per student (col AB of the official PCyTI Excel),
-- not per group — col Y (OBS) is always empty in the real files.
ALTER TABLE trimestral_plan_group_students ADD COLUMN obs VARCHAR(255);
ALTER TABLE trimestral_plan_groups DROP COLUMN obs;
