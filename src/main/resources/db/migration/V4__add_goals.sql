insert into goals (
    name,
    calories_modifier,
    proteins_ratio,
    fats_ratio,
    carbohydrates_ratio
)
values
    ('Понижение веса', 0.850, 0.3000, 0.2500, 0.4500),
    ('Поддержание', 1.000, 0.2500, 0.2500, 0.5000),
    ('Массонабор', 1.100, 0.2500, 0.2500, 0.5000)
on conflict (name) do nothing;