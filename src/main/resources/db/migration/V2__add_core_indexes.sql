create index idx_user_roles_role_id on user_roles (role_id);

create index idx_user_profiles_goal_id on user_profiles (goal_id);

create index idx_products_owner_id on products (owner_id);

create index idx_food_entries_product_id on food_entries (product_id);

create index idx_food_entries_user_id_entry_date on food_entries (user_id, entry_date);
