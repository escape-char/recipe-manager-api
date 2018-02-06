
# --- !Ups

CREATE TYPE recipe_difficulty AS ENUM ('Easy',
                                    'Medium',
                                        'Difficult');
CREATE TYPE ingredient_unit_type AS ENUM ('Weight', 'Volume', 'Length', 'Quantity');
CREATE TYPE ingredient_unit_prefix AS ENUM(
                                    'Pound (lb)',
                                       'Ounce (oz)',
                                       'Milligram (mg)',
                                       'Gram (g)',
                                       'Kilogram (kg)',
                                       'Teaspoon (t or tsp)',
                                       'Tablespoon (T,tbl., tbs, or tbsp)',
                                       'Fluid Ounce (fl oz)',
                                       'Gill (1/2 cup)',
                                       'Cup (c)',
                                       'Pint (p, pt, or fl pt)',
                                       'Quart (q, qt, fl qt)',
                                       'Gallon (g or gal)',
                                       'Millileter (ml, cc, mL)',
                                       'Liter (l, L)',
                                       'Decileter (dL)',
                                        'Millimeter (lb)',
                                       'Centimeter (cm)',
                                       'Meter (m)',
                                       'Inch (in)');

CREATE TABLE users(
    user_id serial primary key,
    username varchar(60) NOT NULL UNIQUE,
    password varchar(100) NOT NULL,
    firstname varchar(60) NOT NULL,
    lastname varchar(60) NOT NULL,
    is_admin boolean NOT NULL DEFAULT false,
    email varchar(60) NOT NULL UNIQUE
);
CREATE TABLE audit_users(
    id serial primary key,
    username varchar(60) NOT NULL,
    action varchar(30) NOT NULL,
    datetime TIMESTAMP WITH TIME ZONE default current_timestamp,
    modified_by varchar(60) default  NULL
);
CREATE TABLE categories(
    category_id serial primary key,
    name varchar(60) NOT NULL UNIQUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    user_id INTEGER NOT NULL references users(user_id) ON DELETE CASCADE
);
CREATE TABLE recipes(
    recipe_id serial primary key,
    title varchar(60) NOT NULL UNIQUE,
    description text NOT NULL,
    source varchar(90) DEFAULT NULL,
    url text DEFAULT NULL,
    difficulty recipe_difficulty,
    servings integer,
    notes text DEFAULT NULL,
    prep_time varchar(15) NOT NULL,
    cook_time varchar(15) NOT NULL,
    image text,
    created_by integer references users(user_id) ON DELETE SET NULL,
    create_datetime TIMESTAMP WITH TIME ZONE default current_timestamp
);
CREATE TABLE ingredients(
    ingredient_id serial primary key,
    item varchar(90) NOT NULL,
    amount integer NOT NULL,
    unit_prefix ingredient_unit_prefix default NULL,
    unit_type ingredient_unit_type NOT NULL,
    recipe_id INTEGER NOT NULL references  recipes(recipe_id) ON DELETE CASCADE
);
CREATE TABLE directions(
    direction_id serial primary key,
    step INTEGER NOT NULL,
    description text NOT NULL,
    recipe_id INTEGER references recipes(recipe_id) ON DELETE CASCADE
);
CREATE TABLE category_recipe(
    cat_rec_id serial primary key,
    category_id INTEGER NOT NULL references  categories(category_id) ON DELETE CASCADE,
    recipe_id INTEGER  NOT NULL references  recipes(recipe_id) ON DELETE CASCADE
);

INSERT INTO users(username, password, firstname, lastname, is_admin, email)
    values('admin', '$2a$10$p4qj.4QStvXIihFtGgX9UeAQBvoTG3i87o6Rvlpkdjp2Cx8V.HNwO', 'a', 'chef',  TRUE, 'api3@testing.not');


INSERT INTO categories(name, is_default, user_id) values ('All', true, 1);
INSERT INTO categories(name, is_default, user_id) values ('Favorites', true, 1);


INSERT INTO recipes(title,
                    description,
                    source,
                    url,
                    difficulty,
                    servings,
                    notes,
                    prep_time,
                    cook_time,
                    image,
                    created_by)
                        values('test recipe',
                            'test description',
                            '',
                            '',
                            'Easy',
                            1,
                            '',
                            '0hr 5m',
                            '0hr 15m',
                            '',
                            1);


INSERT INTO category_recipe(category_id, recipe_id) values(1, 1);
INSERT INTO category_recipe(category_id, recipe_id) values(2, 1);


INSERT INTO ingredients(item,
                amount,
                unit_prefix,
                unit_type,
                recipe_id)
                values ('eggs',
                        2,
                        null,
                        'Quantity',
                            1);
INSERT INTO ingredients(item,
                amount,
                unit_prefix,
                unit_type,
                recipe_id)
                values ('Suger',
                        2,
                        'Pint (p, pt, or fl pt)',
                        'Weight',
                         1);


INSERT INTO directions(step, description, recipe_id) values(1, 'preheat over to 400 degrees fahrenheit', 1);
INSERT INTO directions(step, description, recipe_id) values(2, 'Put cake in the oven for 15 minutes', 1);
INSERT INTO directions(step, description, recipe_id) values(3, 'Take the cake out of the oven and let it set for 5 minutes', 1);


# --- !Downs
drop table category_recipe;
drop table audit_users;
drop table categories;
drop table directions;
drop table ingredients;
drop table recipes;
drop table users;

drop type recipe_difficulty;
drop type ingredient_unit_type;
drop type ingredient_unit_prefix;
