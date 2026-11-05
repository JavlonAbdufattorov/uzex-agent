
-- 1. Типизированные контракты
CREATE OR REPLACE VIEW contracts AS
SELECT
    "Номер контракта" AS contract_id,
    "Наименование продукта" AS product_name,
    "Марка товара" AS product_brand,
    "Размер товара" AS product_size,
    "Сорт товара" AS product_grade,
    "Тип товара" AS product_type,
    "Класс товара" AS product_class,
    "Код ТН ВЭД" AS hs_code,
    "Группа товара" AS product_group,
    "Категория товара" AS category,
    "Отрасль промышленности" AS industry,
    "Единица измерения" AS unit,
    "Валюта" AS currency,
    TRY_CAST("Базисная цена" AS DECIMAL(24,2)) AS base_price,
    "Базис поставки" AS delivery_basis,
    "Регион" AS region,
    "Тип торгов" AS auction_type,
    "Тип контракта" AS contract_type,
    "Форма контракта" AS contract_form,
    TRY_CAST("ИНН продавца" AS VARCHAR) AS seller_inn,
    TRY_STRPTIME("Дата регистрации", '%d.%m.%Y')::DATE AS registered_date,
    TRY_STRPTIME("Действует до", '%d.%m.%Y')::DATE AS expires_date
FROM contracts_raw;


-- 2. Участники
CREATE OR REPLACE VIEW participants AS
SELECT
    "ИНН" AS inn,
    "Наименование" AS company_name,
    "Регион" AS region,
    "Вид деятельности" AS activity,
    "Роль" AS role,
    "Плательщик НДС" AS vat_payer
FROM participants_raw;


-- 3. Брокеры
CREATE OR REPLACE VIEW brokers AS
SELECT
    "Код брокера" AS broker_code,
    "Наименование" AS broker_name,
    "ИНН" AS inn,
    "Регион" AS region
FROM brokers_raw;


-- 4. Курсы валют
CREATE OR REPLACE VIEW usd_rates AS
SELECT
    TRY_STRPTIME("Дата", '%d.%m.%Y')::DATE AS rate_date,
    TRY_CAST("Курс" AS DECIMAL(18,4)) AS usd_rate
FROM usd_rates_raw;
