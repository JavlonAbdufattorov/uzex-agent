SELECT count(*) AS total FROM deals;

SELECT year(deal_date) AS y, count(*) AS n FROM deals GROUP BY 1 ORDER BY 1;

SELECT min(deal_date), max(deal_date) FROM deals;

SELECT
  count(*) FILTER (WHERE deal_id IS NULL)   AS bad_id,
  count(*) FILTER (WHERE deal_date IS NULL) AS bad_date,
  count(*) FILTER (WHERE amount IS NULL)    AS bad_amount,
  count(*) FILTER (WHERE qty_lots IS NULL)  AS bad_qty,
  count(*) FILTER (WHERE lot IS NULL)       AS bad_lot
FROM deals;

SELECT status, count(*) FROM deals GROUP BY 1 ORDER BY 2 DESC;
SELECT auction_type, count(*) FROM deals GROUP BY 1;
SELECT contract_type, contract_form, count(*) FROM deals GROUP BY 1,2;
SELECT unit, count(*) FROM deals GROUP BY 1 ORDER BY 2 DESC;
SELECT region, count(*) FROM deals GROUP BY 1 ORDER BY 2 DESC;

SELECT count(*) FROM contracts_raw;
SELECT count(*) FROM participants_raw;
SELECT count(*) FROM brokers_raw;
SELECT count(*) FROM usd_rates_raw;

DESCRIBE contracts_raw;
DESCRIBE participants_raw;
DESCRIBE brokers_raw;
DESCRIBE usd_rates_raw;

SELECT * FROM contracts_raw    LIMIT 3;
SELECT * FROM participants_raw LIMIT 3;
SELECT * FROM brokers_raw      LIMIT 3;
SELECT * FROM usd_rates_raw    LIMIT 3;
SELECT * FROM deals            LIMIT 3;
