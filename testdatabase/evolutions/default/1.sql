
# --- !Ups

CREATE TABLE users(
    user_id serial primary key,
    username varchar(60) NOT NULL,
    password varchar(100) NOT NULL,
    firstname varchar(60) NOT NULL,
    lastname varchar(60) NOT NULL,
    is_admin boolean NOT NULL DEFAULT false,
    email varchar(60) NOT NULL
);
CREATE TABLE audit_users(
    id serial primary key,
    username varchar(60) NOT NULL,
    action varchar(30) NOT NULL,
    datetime TIMESTAMP WITH TIME ZONE default current_timestamp,
    modified_by varchar(60) default  NULL
);

INSERT INTO users(username, password, firstname, lastname, is_admin, email)
    values('test1', 'testpwd', 'first1', 'last1', FALSE, 'test1@testing.not');

INSERT INTO users(username, password, firstname, lastname, is_admin, email)
    values('test2', 'testpwd', 'first2', 'last2', FALSE, 'test2@testing.not')

 --- !Downs

drop table users;
drop table audit_userss;

