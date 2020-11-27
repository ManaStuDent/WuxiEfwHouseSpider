DROP TABLE IF EXISTS `house`;
CREATE TABLE `house` (
                         `id` int(11) NOT NULL AUTO_INCREMENT,
                         `title_text` varchar(255) DEFAULT NULL,
                         `house_id` varchar(255) DEFAULT NULL,
                         `url_path` varchar(255) DEFAULT NULL,
                         `community_name` varchar(255) DEFAULT NULL,
                         `address_details` varchar(255) DEFAULT NULL,
                         `direction` varchar(255) DEFAULT NULL,
                         `decoration` varchar(255) DEFAULT NULL,
                         `floor` varchar(255) DEFAULT NULL,
                         `structure` varchar(255) DEFAULT NULL,
                         `area` varchar(255) DEFAULT NULL,
                         `nearBackground` varchar(255) DEFAULT NULL,
                         `total_price` decimal(10,2) DEFAULT NULL,
                         `price_single` decimal(10,0) DEFAULT NULL,
                         `create_date` datetime DEFAULT NULL,
                         `version` int(4) DEFAULT NULL,
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;