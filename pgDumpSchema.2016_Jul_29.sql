--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: portfolio_trigger(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION portfolio_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
  PERFORM pg_notify('portfolioNotify', row_to_json(NEW)::text);
  RETURN new;
END;
$$;


ALTER FUNCTION public.portfolio_trigger() OWNER TO postgres;

--
-- Name: watchlist_trigger(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION watchlist_trigger() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
BEGIN
  PERFORM pg_notify('watchlistNotify', row_to_json(NEW)::text);
  RETURN new;
END;
$$;


ALTER FUNCTION public.watchlist_trigger() OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: adjustedreservedcash; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE adjustedreservedcash (
    reserved_cash double precision
);


ALTER TABLE public.adjustedreservedcash OWNER TO postgres;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE orders (
    portfolio character varying(20) NOT NULL,
    order_id bigint NOT NULL,
    open boolean,
    ticker character varying(32),
    underlying_ticker character varying(32),
    strike_price double precision,
    limit_price double precision,
    action character varying(20),
    total_quantity integer,
    sec_type character varying(20),
    tif character varying(20),
    epoch_opened bigint,
    epoch_closed bigint,
    close_reason character varying(20),
    fill_price double precision,
    epoch_expiry bigint,
    claim_against_cash double precision
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- Name: portfolios; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE portfolios (
    name character varying(20) NOT NULL,
    net_asset_value double precision,
    free_cash double precision,
    reserved_cash double precision,
    total_cash double precision
);


ALTER TABLE public.portfolios OWNER TO postgres;

--
-- Name: positions; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE positions (
    portfolio character varying(20) NOT NULL,
    position_id bigint NOT NULL,
    open boolean,
    ticker character varying(32),
    sec_type character varying(20),
    underlying_ticker character varying(32),
    strike_price double precision,
    epoch_opened bigint,
    long_position boolean,
    number_transacted integer,
    price_at_open double precision,
    cost_basis double precision,
    last_tick double precision,
    net_asset_value double precision,
    epoch_closed bigint,
    price_at_close double precision,
    profit double precision,
    epoch_expiry bigint,
    claim_against_cash double precision,
    originating_order_id bigint
);


ALTER TABLE public.positions OWNER TO postgres;

--
-- Name: prices; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE prices (
    ticker character varying(32) NOT NULL,
    adj_close double precision,
    close_epoch bigint NOT NULL
);


ALTER TABLE public.prices OWNER TO postgres;

--
-- Name: watchlist; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE watchlist (
    ticker character varying(32) NOT NULL,
    twenty_dma double precision,
    first_low_boll double precision,
    first_high_boll double precision,
    last_tick double precision,
    last_tick_epoch bigint,
    active boolean,
    second_low_boll double precision,
    second_high_boll double precision
);


ALTER TABLE public.watchlist OWNER TO postgres;

--
-- Name: orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (portfolio, order_id);


--
-- Name: portfolios_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY portfolios
    ADD CONSTRAINT portfolios_pkey PRIMARY KEY (name);


--
-- Name: positions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY positions
    ADD CONSTRAINT positions_pkey PRIMARY KEY (portfolio, position_id);


--
-- Name: prices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY prices
    ADD CONSTRAINT prices_pkey PRIMARY KEY (ticker, close_epoch);


--
-- Name: watchlist_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY watchlist
    ADD CONSTRAINT watchlist_pkey PRIMARY KEY (ticker);


--
-- Name: portfolio_changes; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER portfolio_changes AFTER UPDATE ON portfolios FOR EACH ROW EXECUTE PROCEDURE portfolio_trigger();


--
-- Name: watchlist_changes; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER watchlist_changes AFTER UPDATE ON watchlist FOR EACH ROW EXECUTE PROCEDURE watchlist_trigger();


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

