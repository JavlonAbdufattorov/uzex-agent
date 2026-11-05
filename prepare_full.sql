CREATE OR REPLACE VIEW deals_full AS
SELECT
    -- Основные данные сделки
    d.*,

    -- Информация о товаре
    c.product_brand,
    c.product_size,
    c.product_grade,
    c.product_type,
    c.product_class,
    c.hs_code,
    c.product_group,
    c.category,
    c.industry,

    -- Информация из контракта
    c.unit AS contract_unit,
    c.currency,
    c.base_price,
    c.delivery_basis,
    c.region AS contract_region,
    c.auction_type AS contract_auction_type,
    c.contract_type AS contract_contract_type,
    c.contract_form AS contract_contract_form,
    c.registered_date,
    c.expires_date,

    -- Продавец
    seller.company_name AS seller_name,
    seller.region AS seller_region,
    seller.activity AS seller_activity,

    -- Покупатель
    buyer.company_name AS buyer_name,
    buyer.region AS buyer_region,
    buyer.activity AS buyer_activity,

    -- Брокеры
    sb.broker_name AS seller_broker_name,
    bb.broker_name AS buyer_broker_name

FROM deals d

LEFT JOIN contracts c
    ON d.contract_id = c.contract_id

LEFT JOIN participants seller
    ON d.seller_inn = seller.inn

LEFT JOIN participants buyer
    ON d.buyer_inn = buyer.inn

LEFT JOIN brokers sb
    ON d.seller_broker = sb.broker_code

LEFT JOIN brokers bb
    ON d.buyer_broker = bb.broker_code;
