use nowbot;

create table if not exists `binding`(
    `qq` int primary key ,
    `oauthkey` varchar (782)
) CHARSET=utf8;