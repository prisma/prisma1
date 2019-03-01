SELECT `id`, `unique`
FROM `User`
WHERE (
    `User` IN
        (SELECT `A`
        FROM `FilterSpec_S`.`_UserToVehicle`
        INNER JOIN `FilterSpec_S`.`User`
        ON `Vehicle`.`Vehicle`.`id` = ?
        WHERE `brand` LIKE ?
        LIMIT -1)
     AND
        1=1
)
ORDER BY `FilterSpec_S`.`User`.`id` ASC
