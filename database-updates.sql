
ALTER TABLE `user` ADD COLUMN `profile_image` varchar(255) ;
ALTER TABLE `user` ADD COLUMN `email_verified` BOOLEAN DEFAULT FALSE AFTER `profile_image`;
UPDATE `user` SET `email_verified` = TRUE WHERE `email_verified` IS NULL OR `email_verified` = FALSE;

