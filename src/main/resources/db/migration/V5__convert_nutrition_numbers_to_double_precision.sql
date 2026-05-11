alter table goals
    alter column calories_modifier type double precision using calories_modifier::double precision,
    alter column proteins_ratio type double precision using proteins_ratio::double precision,
    alter column fats_ratio type double precision using fats_ratio::double precision,
    alter column carbohydrates_ratio type double precision using carbohydrates_ratio::double precision;

alter table user_profiles
    alter column weight_kg type double precision using weight_kg::double precision;

alter table products
    alter column calories_per_100g type double precision using calories_per_100g::double precision,
    alter column proteins_per_100g type double precision using proteins_per_100g::double precision,
    alter column fats_per_100g type double precision using fats_per_100g::double precision,
    alter column carbohydrates_per_100g type double precision using carbohydrates_per_100g::double precision;

alter table food_entries
    alter column grams type double precision using grams::double precision,
    alter column calories type double precision using calories::double precision,
    alter column proteins type double precision using proteins::double precision,
    alter column fats type double precision using fats::double precision,
    alter column carbohydrates type double precision using carbohydrates::double precision;
