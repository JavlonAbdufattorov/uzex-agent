-- load.sql: загрузка данных в DuckDB

-- 1. Сырые сделки: всё как текст
CREATE OR REPLACE TABLE deals_raw AS
SELECT * FROM read_csv('data/deals_*.csv',
       header=true, all_varchar=true, union_by_name=true);

-- 2. Типизированные сделки
CREATE OR REPLACE TABLE deals AS
SELECT
  try_cast("Номер сделки" AS BIGINT)                                      AS deal_id,
  try_strptime("Дата сделки", '%d.%m.%Y')::DATE                           AS deal_date,
  try_cast("Время сделки" AS TIME)                                        AS deal_time,
  "Тип торгов"                                                            AS auction_type,
  try_cast("Количество" AS DOUBLE)                                        AS qty_lots,
  try_cast(regexp_replace("Сумма сделки",   '[^0-9.\-]', '', 'g') AS DECIMAL(24,2)) AS amount,
  try_cast(regexp_replace("Стартовая цена", '[^0-9.\-]', '', 'g') AS DECIMAL(24,2)) AS start_price,
  "Номер контракта"                                                       AS contract_id,
  "Наименование товара"                                                   AS product_name,
  try_cast("Лот" AS DOUBLE)                                               AS lot,
  "Единица измерения"                                                     AS unit,
  "Регион"                                                                AS region,
  "Тип контракта"                                                         AS contract_type,
  "Форма контракта"                                                       AS contract_form,
  "Состояние сделки"                                                      AS status,
  "ИНН продавца"                                                          AS seller_inn,
  "ИНН покупателя"                                                        AS buyer_inn,
  "Брокер продавца"                                                       AS seller_broker,
  "Брокер покупателя"                                                     AS buyer_broker,
  try_strptime("Дата оплаты",   '%d.%m.%Y')::DATE                         AS paid_date,
  try_strptime("Дата поставки", '%d.%m.%Y')::DATE                         AS delivered_date,
  try_cast("Поставлено" AS DOUBLE)                                        AS delivered_lots
FROM deals_raw;

-- 3. Остальные файлы пока как текст
CREATE OR REPLACE TABLE contracts_raw    AS SELECT * FROM read_csv('data/contracts.csv',    header=true, all_varchar=true);
CREATE OR REPLACE TABLE participants_raw AS SELECT * FROM read_csv('data/participants.csv', header=true, all_varchar=true);
CREATE OR REPLACE TABLE brokers_raw      AS SELECT * FROM read_csv('data/brokers.csv',      header=true, all_varchar=true);
CREATE OR REPLACE TABLE usd_rates_raw    AS SELECT * FROM read_csv('data/usd_rates.csv',    header=true, all_varchar=true);
