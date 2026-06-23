-- Correct BCrypt hash for seed password 'password' (V5 used an invalid hash)
UPDATE users
SET password_hash = '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6'
WHERE email = 'coordinator@uam.mx';
