--
-- PostgreSQL database dump
--


-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: articles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.articles (
    id bigint NOT NULL,
    author character varying(255) NOT NULL,
    category character varying(255) NOT NULL,
    content text NOT NULL,
    cover_image character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    excerpt text,
    meta_description character varying(300),
    meta_keywords character varying(255),
    published boolean NOT NULL,
    published_at timestamp(6) without time zone,
    reading_time_minutes integer,
    slug character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone
);


--
-- Name: articles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.articles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: articles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.articles_id_seq OWNED BY public.articles.id;


--
-- Name: availabilities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.availabilities (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    day_of_week character varying(255),
    end_time time(6) without time zone NOT NULL,
    is_active boolean NOT NULL,
    is_recurring boolean NOT NULL,
    specific_date date,
    start_time time(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    teacher_id bigint NOT NULL,
    CONSTRAINT availabilities_day_of_week_check CHECK (((day_of_week)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[])))
);


--
-- Name: availabilities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.availabilities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: availabilities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.availabilities_id_seq OWNED BY public.availabilities.id;


--
-- Name: courses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.courses (
    id bigint NOT NULL,
    content text,
    created_at timestamp(6) without time zone NOT NULL,
    description text,
    estimated_minutes integer,
    grade character varying(255) NOT NULL,
    icon_name character varying(255),
    order_in_grade integer NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    CONSTRAINT courses_grade_check CHECK (((grade)::text = ANY ((ARRAY['PION'::character varying, 'CAVALIER'::character varying, 'FOU'::character varying, 'TOUR'::character varying, 'DAME'::character varying, 'ROI'::character varying])::text[])))
);


--
-- Name: courses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.courses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: courses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.courses_id_seq OWNED BY public.courses.id;


--
-- Name: credit_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credit_transactions (
    id bigint NOT NULL,
    amount_cents integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(255),
    stripe_payment_intent_id character varying(255),
    transaction_type character varying(255) NOT NULL,
    lesson_id bigint,
    user_id bigint NOT NULL,
    CONSTRAINT credit_transactions_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['TOPUP'::character varying, 'LESSON_PAYMENT'::character varying, 'REFUND'::character varying])::text[])))
);


--
-- Name: credit_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.credit_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: credit_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.credit_transactions_id_seq OWNED BY public.credit_transactions.id;


--
-- Name: exercises; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exercises (
    id bigint NOT NULL,
    chess_level character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    description text,
    difficulty_level character varying(255) NOT NULL,
    player_color character varying(255) NOT NULL,
    starting_fen character varying(255) NOT NULL,
    time_limit_seconds integer,
    title character varying(255) NOT NULL,
    lesson_id bigint,
    CONSTRAINT exercises_chess_level_check CHECK (((chess_level)::text = ANY ((ARRAY['PION'::character varying, 'CAVALIER'::character varying, 'FOU'::character varying, 'TOUR'::character varying, 'DAME'::character varying, 'ROI'::character varying])::text[]))),
    CONSTRAINT exercises_difficulty_level_check CHECK (((difficulty_level)::text = ANY ((ARRAY['DEBUTANT'::character varying, 'FACILE'::character varying, 'MOYEN'::character varying, 'DIFFICILE'::character varying, 'EXPERT'::character varying])::text[])))
);


--
-- Name: exercises_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.exercises_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exercises_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.exercises_id_seq OWNED BY public.exercises.id;


--
-- Name: favorite_teachers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.favorite_teachers (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    notify_new_slots boolean NOT NULL,
    student_id bigint NOT NULL,
    teacher_id bigint NOT NULL
);


--
-- Name: favorite_teachers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.favorite_teachers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: favorite_teachers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.favorite_teachers_id_seq OWNED BY public.favorite_teachers.id;


--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    id bigint NOT NULL,
    commission_rate double precision,
    created_at timestamp(6) without time zone NOT NULL,
    description character varying(255) NOT NULL,
    invoice_number character varying(255) NOT NULL,
    invoice_type character varying(255) NOT NULL,
    issued_at timestamp(6) without time zone NOT NULL,
    pdf_path character varying(255),
    promo_applied boolean,
    refund_percentage integer,
    status character varying(255) NOT NULL,
    stripe_invoice_id character varying(255),
    stripe_payment_intent_id character varying(255),
    stripe_refund_id character varying(255),
    subtotal_cents integer NOT NULL,
    total_cents integer NOT NULL,
    vat_cents integer NOT NULL,
    vat_rate integer,
    customer_id bigint NOT NULL,
    issuer_id bigint,
    lesson_id bigint,
    original_invoice_id bigint,
    CONSTRAINT invoices_invoice_type_check CHECK (((invoice_type)::text = ANY ((ARRAY['LESSON_INVOICE'::character varying, 'COMMISSION_INVOICE'::character varying, 'PAYOUT_INVOICE'::character varying])::text[])))
);


--
-- Name: invoices_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.invoices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: invoices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.invoices_id_seq OWNED BY public.invoices.id;


--
-- Name: lessons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lessons (
    id bigint NOT NULL,
    cancellation_reason character varying(255),
    cancelled_at timestamp(6) without time zone,
    cancelled_by character varying(255),
    commission_cents integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    duration_minutes integer NOT NULL,
    earnings_credited boolean,
    is_free_trial boolean,
    is_from_subscription boolean NOT NULL,
    notes text,
    price_cents integer NOT NULL,
    recording_url character varying(255),
    refund_percentage integer,
    refunded_amount_cents integer,
    reminder_sent boolean,
    scheduled_at timestamp(6) without time zone NOT NULL,
    status character varying(255) NOT NULL,
    stripe_refund_id character varying(255),
    teacher_earnings_cents integer NOT NULL,
    teacher_joined_at timestamp(6) without time zone,
    teacher_observations text,
    updated_at timestamp(6) without time zone,
    zoom_link character varying(255),
    zoom_meeting_id character varying(255),
    course_id bigint,
    student_id bigint NOT NULL,
    subscription_id bigint,
    teacher_id bigint NOT NULL,
    CONSTRAINT lessons_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying, 'NO_SHOW'::character varying])::text[])))
);


--
-- Name: lessons_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lessons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lessons_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lessons_id_seq OWNED BY public.lessons.id;


--
-- Name: page_views; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.page_views (
    id bigint NOT NULL,
    page_url character varying(500) NOT NULL,
    session_id character varying(36),
    visited_at timestamp(6) without time zone NOT NULL,
    user_id bigint
);


--
-- Name: page_views_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.page_views_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: page_views_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.page_views_id_seq OWNED BY public.page_views.id;


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    token character varying(255) NOT NULL,
    used_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    amount_cents integer NOT NULL,
    commission_cents integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    failure_reason character varying(255),
    payment_type character varying(255) NOT NULL,
    processed_at timestamp(6) without time zone,
    refund_reason character varying(255),
    status character varying(255) NOT NULL,
    stripe_charge_id character varying(255),
    stripe_payment_intent_id character varying(255),
    stripe_transfer_id character varying(255),
    teacher_payout_cents integer,
    lesson_id bigint,
    payer_id bigint NOT NULL,
    subscription_id bigint,
    teacher_id bigint,
    CONSTRAINT payments_payment_type_check CHECK (((payment_type)::text = ANY ((ARRAY['SUBSCRIPTION'::character varying, 'ONE_TIME_LESSON'::character varying, 'CREDIT_TOPUP'::character varying, 'LESSON_FROM_CREDIT'::character varying])::text[]))),
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: progress_tracking; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progress_tracking (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    current_level character varying(255) NOT NULL,
    evaluated_at timestamp(6) without time zone,
    evaluated_by_teacher_id bigint,
    last_lesson_date timestamp(6) without time zone,
    lessons_at_current_level integer NOT NULL,
    lessons_required_for_next_level integer NOT NULL,
    level_set_by_coach boolean NOT NULL,
    total_lessons_completed integer NOT NULL,
    updated_at timestamp(6) without time zone,
    student_id bigint NOT NULL,
    CONSTRAINT progress_tracking_current_level_check CHECK (((current_level)::text = ANY ((ARRAY['PION'::character varying, 'CAVALIER'::character varying, 'FOU'::character varying, 'TOUR'::character varying, 'DAME'::character varying, 'ROI'::character varying])::text[])))
);


--
-- Name: progress_tracking_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.progress_tracking_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: progress_tracking_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.progress_tracking_id_seq OWNED BY public.progress_tracking.id;


--
-- Name: quiz_questions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quiz_questions (
    id bigint NOT NULL,
    correct_answer character varying(255) NOT NULL,
    explanation text,
    level character varying(255) NOT NULL,
    option_a character varying(255) NOT NULL,
    option_b character varying(255) NOT NULL,
    option_c character varying(255) NOT NULL,
    option_d character varying(255),
    order_in_level integer,
    question text NOT NULL,
    CONSTRAINT quiz_questions_level_check CHECK (((level)::text = ANY ((ARRAY['PION'::character varying, 'CAVALIER'::character varying, 'FOU'::character varying, 'TOUR'::character varying, 'DAME'::character varying, 'ROI'::character varying])::text[])))
);


--
-- Name: quiz_questions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quiz_questions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quiz_questions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quiz_questions_id_seq OWNED BY public.quiz_questions.id;


--
-- Name: quiz_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quiz_results (
    id bigint NOT NULL,
    cavalier_score integer,
    completed_at timestamp(6) without time zone NOT NULL,
    dame_score integer,
    determined_level character varying(255) NOT NULL,
    fou_score integer,
    pion_score integer,
    roi_score integer,
    tour_score integer,
    student_id bigint NOT NULL,
    CONSTRAINT quiz_results_determined_level_check CHECK (((determined_level)::text = ANY ((ARRAY['PION'::character varying, 'CAVALIER'::character varying, 'FOU'::character varying, 'TOUR'::character varying, 'DAME'::character varying, 'ROI'::character varying])::text[])))
);


--
-- Name: quiz_results_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quiz_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quiz_results_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quiz_results_id_seq OWNED BY public.quiz_results.id;


--
-- Name: ratings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ratings (
    id bigint NOT NULL,
    comment text,
    created_at timestamp(6) without time zone NOT NULL,
    stars integer NOT NULL,
    lesson_id bigint NOT NULL,
    student_id bigint NOT NULL,
    teacher_id bigint NOT NULL
);


--
-- Name: ratings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ratings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ratings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ratings_id_seq OWNED BY public.ratings.id;


--
-- Name: student_wallets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.student_wallets (
    id bigint NOT NULL,
    balance_cents integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    total_refunded_cents integer NOT NULL,
    total_top_ups_cents integer NOT NULL,
    total_used_cents integer NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id bigint NOT NULL
);


--
-- Name: student_wallets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.student_wallets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: student_wallets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.student_wallets_id_seq OWNED BY public.student_wallets.id;


--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.subscriptions (
    id bigint NOT NULL,
    cancelled_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    end_date date,
    is_active boolean NOT NULL,
    plan_type character varying(255) NOT NULL,
    price_cents integer NOT NULL,
    start_date date NOT NULL,
    stripe_subscription_id character varying(255),
    student_id bigint NOT NULL,
    CONSTRAINT subscriptions_plan_type_check CHECK (((plan_type)::text = 'PREMIUM'::text))
);


--
-- Name: subscriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.subscriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: subscriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.subscriptions_id_seq OWNED BY public.subscriptions.id;


--
-- Name: teacher_balances; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teacher_balances (
    id bigint NOT NULL,
    available_balance_cents integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    lessons_completed integer NOT NULL,
    pending_balance_cents integer NOT NULL,
    total_earned_cents integer NOT NULL,
    total_withdrawn_cents integer NOT NULL,
    updated_at timestamp(6) without time zone,
    teacher_id bigint NOT NULL
);


--
-- Name: teacher_balances_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.teacher_balances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: teacher_balances_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.teacher_balances_id_seq OWNED BY public.teacher_balances.id;


--
-- Name: teacher_payouts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.teacher_payouts (
    id bigint NOT NULL,
    amount_cents integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    is_paid boolean NOT NULL,
    lessons_count integer NOT NULL,
    notes character varying(255),
    paid_at timestamp(6) without time zone,
    payment_reference character varying(255),
    stripe_transfer_id character varying(255),
    year_month character varying(255) NOT NULL,
    teacher_id bigint NOT NULL
);


--
-- Name: teacher_payouts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.teacher_payouts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: teacher_payouts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.teacher_payouts_id_seq OWNED BY public.teacher_payouts.id;


--
-- Name: user_course_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_course_progress (
    id bigint NOT NULL,
    completed_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    started_at timestamp(6) without time zone,
    status character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    validated_at timestamp(6) without time zone,
    validated_by_teacher_id bigint,
    course_id bigint NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT user_course_progress_status_check CHECK (((status)::text = ANY ((ARRAY['LOCKED'::character varying, 'IN_PROGRESS'::character varying, 'PENDING_VALIDATION'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: user_course_progress_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_course_progress_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_course_progress_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_course_progress_id_seq OWNED BY public.user_course_progress.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    accepts_free_trial boolean,
    accepts_subscription boolean,
    account_holder_name character varying(255),
    avatar_url character varying(255),
    bic character varying(255),
    bio text,
    birth_date date,
    company_name character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    elo_rating integer,
    email character varying(255) NOT NULL,
    email_reminders_enabled boolean,
    first_name character varying(255) NOT NULL,
    google_calendar_enabled boolean,
    google_calendar_refresh_token text,
    google_calendar_token text,
    has_used_free_trial boolean,
    hourly_rate_cents integer,
    iban character varying(255),
    is_suspended boolean,
    languages character varying(255),
    last_active_at timestamp(6) without time zone,
    last_login_at timestamp(6) without time zone,
    last_name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    siret character varying(255),
    stripe_connect_account_id character varying(255),
    stripe_connect_onboarding_complete boolean,
    updated_at timestamp(6) without time zone,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['STUDENT'::character varying, 'TEACHER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: articles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.articles ALTER COLUMN id SET DEFAULT nextval('public.articles_id_seq'::regclass);


--
-- Name: availabilities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.availabilities ALTER COLUMN id SET DEFAULT nextval('public.availabilities_id_seq'::regclass);


--
-- Name: courses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses ALTER COLUMN id SET DEFAULT nextval('public.courses_id_seq'::regclass);


--
-- Name: credit_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions ALTER COLUMN id SET DEFAULT nextval('public.credit_transactions_id_seq'::regclass);


--
-- Name: exercises id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exercises ALTER COLUMN id SET DEFAULT nextval('public.exercises_id_seq'::regclass);


--
-- Name: favorite_teachers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite_teachers ALTER COLUMN id SET DEFAULT nextval('public.favorite_teachers_id_seq'::regclass);


--
-- Name: invoices id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices ALTER COLUMN id SET DEFAULT nextval('public.invoices_id_seq'::regclass);


--
-- Name: lessons id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons ALTER COLUMN id SET DEFAULT nextval('public.lessons_id_seq'::regclass);


--
-- Name: page_views id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_views ALTER COLUMN id SET DEFAULT nextval('public.page_views_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: progress_tracking id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progress_tracking ALTER COLUMN id SET DEFAULT nextval('public.progress_tracking_id_seq'::regclass);


--
-- Name: quiz_questions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions ALTER COLUMN id SET DEFAULT nextval('public.quiz_questions_id_seq'::regclass);


--
-- Name: quiz_results id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results ALTER COLUMN id SET DEFAULT nextval('public.quiz_results_id_seq'::regclass);


--
-- Name: ratings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings ALTER COLUMN id SET DEFAULT nextval('public.ratings_id_seq'::regclass);


--
-- Name: student_wallets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_wallets ALTER COLUMN id SET DEFAULT nextval('public.student_wallets_id_seq'::regclass);


--
-- Name: subscriptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions ALTER COLUMN id SET DEFAULT nextval('public.subscriptions_id_seq'::regclass);


--
-- Name: teacher_balances id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_balances ALTER COLUMN id SET DEFAULT nextval('public.teacher_balances_id_seq'::regclass);


--
-- Name: teacher_payouts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_payouts ALTER COLUMN id SET DEFAULT nextval('public.teacher_payouts_id_seq'::regclass);


--
-- Name: user_course_progress id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_course_progress ALTER COLUMN id SET DEFAULT nextval('public.user_course_progress_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: articles articles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_pkey PRIMARY KEY (id);


--
-- Name: availabilities availabilities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.availabilities
    ADD CONSTRAINT availabilities_pkey PRIMARY KEY (id);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: credit_transactions credit_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT credit_transactions_pkey PRIMARY KEY (id);


--
-- Name: exercises exercises_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT exercises_pkey PRIMARY KEY (id);


--
-- Name: favorite_teachers favorite_teachers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite_teachers
    ADD CONSTRAINT favorite_teachers_pkey PRIMARY KEY (id);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);


--
-- Name: lessons lessons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT lessons_pkey PRIMARY KEY (id);


--
-- Name: page_views page_views_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_views
    ADD CONSTRAINT page_views_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: progress_tracking progress_tracking_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progress_tracking
    ADD CONSTRAINT progress_tracking_pkey PRIMARY KEY (id);


--
-- Name: quiz_questions quiz_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions
    ADD CONSTRAINT quiz_questions_pkey PRIMARY KEY (id);


--
-- Name: quiz_results quiz_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results
    ADD CONSTRAINT quiz_results_pkey PRIMARY KEY (id);


--
-- Name: ratings ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_pkey PRIMARY KEY (id);


--
-- Name: student_wallets student_wallets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_wallets
    ADD CONSTRAINT student_wallets_pkey PRIMARY KEY (id);


--
-- Name: subscriptions subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


--
-- Name: teacher_balances teacher_balances_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_balances
    ADD CONSTRAINT teacher_balances_pkey PRIMARY KEY (id);


--
-- Name: teacher_payouts teacher_payouts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_payouts
    ADD CONSTRAINT teacher_payouts_pkey PRIMARY KEY (id);


--
-- Name: ratings uk_1n7a1rmcqsv3odg9rjstg7n7v; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT uk_1n7a1rmcqsv3odg9rjstg7n7v UNIQUE (lesson_id);


--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: password_reset_tokens uk_71lqwbwtklmljk3qlsugr1mig; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT uk_71lqwbwtklmljk3qlsugr1mig UNIQUE (token);


--
-- Name: teacher_balances uk_9oaj8qksxuxau425ul54ucfja; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_balances
    ADD CONSTRAINT uk_9oaj8qksxuxau425ul54ucfja UNIQUE (teacher_id);


--
-- Name: student_wallets uk_9t7h17xa7g4l82ncj4hce69iy; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_wallets
    ADD CONSTRAINT uk_9t7h17xa7g4l82ncj4hce69iy UNIQUE (user_id);


--
-- Name: progress_tracking uk_k6r8m300sv5q46nn0f25v1n6l; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progress_tracking
    ADD CONSTRAINT uk_k6r8m300sv5q46nn0f25v1n6l UNIQUE (student_id);


--
-- Name: invoices uk_l1x55mfsay7co0r3m9ynvipd5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT uk_l1x55mfsay7co0r3m9ynvipd5 UNIQUE (invoice_number);


--
-- Name: articles uk_sn7al9fwhgtf98rvn8nxhjt4f; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT uk_sn7al9fwhgtf98rvn8nxhjt4f UNIQUE (slug);


--
-- Name: user_course_progress ukft2vonp1b45a7qvyf4uky4mvy; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_course_progress
    ADD CONSTRAINT ukft2vonp1b45a7qvyf4uky4mvy UNIQUE (user_id, course_id);


--
-- Name: favorite_teachers ukj7xp25v7gxb619xw82ulsswog; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite_teachers
    ADD CONSTRAINT ukj7xp25v7gxb619xw82ulsswog UNIQUE (student_id, teacher_id);


--
-- Name: user_course_progress user_course_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_course_progress
    ADD CONSTRAINT user_course_progress_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_exercise_chess_level; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exercise_chess_level ON public.exercises USING btree (chess_level);


--
-- Name: idx_exercise_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_exercise_lesson_id ON public.exercises USING btree (lesson_id);


--
-- Name: idx_invoice_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_customer_id ON public.invoices USING btree (customer_id);


--
-- Name: idx_invoice_issued_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_issued_at ON public.invoices USING btree (issued_at);


--
-- Name: idx_invoice_issuer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_issuer_id ON public.invoices USING btree (issuer_id);


--
-- Name: idx_invoice_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_lesson_id ON public.invoices USING btree (lesson_id);


--
-- Name: idx_invoice_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_number ON public.invoices USING btree (invoice_number);


--
-- Name: idx_invoice_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_status ON public.invoices USING btree (status);


--
-- Name: idx_invoice_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_type ON public.invoices USING btree (invoice_type);


--
-- Name: idx_lesson_scheduled_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lesson_scheduled_at ON public.lessons USING btree (scheduled_at);


--
-- Name: idx_lesson_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lesson_status ON public.lessons USING btree (status);


--
-- Name: idx_lesson_status_scheduled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lesson_status_scheduled ON public.lessons USING btree (status, scheduled_at);


--
-- Name: idx_lesson_student_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lesson_student_id ON public.lessons USING btree (student_id);


--
-- Name: idx_lesson_teacher_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lesson_teacher_id ON public.lessons USING btree (teacher_id);


--
-- Name: idx_lesson_teacher_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lesson_teacher_status ON public.lessons USING btree (teacher_id, status);


--
-- Name: idx_page_view_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_page_view_session ON public.page_views USING btree (session_id);


--
-- Name: idx_page_view_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_page_view_timestamp ON public.page_views USING btree (visited_at);


--
-- Name: idx_page_view_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_page_view_user ON public.page_views USING btree (user_id);


--
-- Name: idx_payment_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_created_at ON public.payments USING btree (created_at);


--
-- Name: idx_payment_lesson_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_lesson_id ON public.payments USING btree (lesson_id);


--
-- Name: idx_payment_payer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_payer_id ON public.payments USING btree (payer_id);


--
-- Name: idx_payment_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_status ON public.payments USING btree (status);


--
-- Name: idx_payment_stripe_intent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_stripe_intent ON public.payments USING btree (stripe_payment_intent_id);


--
-- Name: idx_payment_teacher_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payment_teacher_id ON public.payments USING btree (teacher_id);


--
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_email ON public.users USING btree (email);


--
-- Name: idx_user_last_login; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_last_login ON public.users USING btree (last_login_at);


--
-- Name: idx_user_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_role ON public.users USING btree (role);


--
-- Name: credit_transactions fk13k733c16qk34hbyodbcdp04; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT fk13k733c16qk34hbyodbcdp04 FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);


--
-- Name: lessons fk17ucc7gjfjddsyi0gvstkqeat; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT fk17ucc7gjfjddsyi0gvstkqeat FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: invoices fk1q3y9g5fwp5u0k2rmitl6ewlf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk1q3y9g5fwp5u0k2rmitl6ewlf FOREIGN KEY (issuer_id) REFERENCES public.users(id);


--
-- Name: ratings fk23wyif4jd4acjls8dmx0tubgm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT fk23wyif4jd4acjls8dmx0tubgm FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: quiz_results fk27oneimg9j5b0vocr7n1ky2uq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results
    ADD CONSTRAINT fk27oneimg9j5b0vocr7n1ky2uq FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: payments fk2g8lk4ddut97mbwyj83ldghbe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk2g8lk4ddut97mbwyj83ldghbe FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);


--
-- Name: lessons fk843n4rnjdhi154ra8472wg8ho; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT fk843n4rnjdhi154ra8472wg8ho FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: invoices fk8ygwwcxj1ni4pqwqb6g4fhcvw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk8ygwwcxj1ni4pqwqb6g4fhcvw FOREIGN KEY (customer_id) REFERENCES public.users(id);


--
-- Name: payments fk9qqwh5i8e0nxivjbobt9vj0ys; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk9qqwh5i8e0nxivjbobt9vj0ys FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: payments fka3xnf2o6mt8cqbewvq2ouq3rq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fka3xnf2o6mt8cqbewvq2ouq3rq FOREIGN KEY (subscription_id) REFERENCES public.subscriptions(id);


--
-- Name: ratings fkass713si88fodyhf57ytdddn6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT fkass713si88fodyhf57ytdddn6 FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: invoices fkb0ls0l6dpvg5rk1vfqyuwrxv1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fkb0ls0l6dpvg5rk1vfqyuwrxv1 FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);


--
-- Name: page_views fkbninblcx3s25mrup64q75bbep; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.page_views
    ADD CONSTRAINT fkbninblcx3s25mrup64q75bbep FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: teacher_payouts fkc323p8mmdn4uhqckurq5m3gxu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_payouts
    ADD CONSTRAINT fkc323p8mmdn4uhqckurq5m3gxu FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: lessons fkei9c8guqbar9fmqyyvy9t2o93; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT fkei9c8guqbar9fmqyyvy9t2o93 FOREIGN KEY (subscription_id) REFERENCES public.subscriptions(id);


--
-- Name: lessons fkes95yw68i7qsrabf92vsepcth; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lessons
    ADD CONSTRAINT fkes95yw68i7qsrabf92vsepcth FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: exercises fkes9e0n86cjfb0l6349clxvxc1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exercises
    ADD CONSTRAINT fkes9e0n86cjfb0l6349clxvxc1 FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);


--
-- Name: subscriptions fkf8idxauhs5lntnyws59711ibv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.subscriptions
    ADD CONSTRAINT fkf8idxauhs5lntnyws59711ibv FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: ratings fkfjr95sn1h4o9s4c1kd93ybdwk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT fkfjr95sn1h4o9s4c1kd93ybdwk FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);


--
-- Name: favorite_teachers fkgbmjjqqw1ghf59y5o09947nye; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite_teachers
    ADD CONSTRAINT fkgbmjjqqw1ghf59y5o09947nye FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: user_course_progress fkj5thv4d609cmoa8mxffdfluy1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_course_progress
    ADD CONSTRAINT fkj5thv4d609cmoa8mxffdfluy1 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: progress_tracking fkjiephngyby8aiceu64wmeuuc5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progress_tracking
    ADD CONSTRAINT fkjiephngyby8aiceu64wmeuuc5 FOREIGN KEY (student_id) REFERENCES public.users(id);


--
-- Name: user_course_progress fkjj3sw5b9g86gydd6tdvnuailh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_course_progress
    ADD CONSTRAINT fkjj3sw5b9g86gydd6tdvnuailh FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: credit_transactions fkjl0dwyeb8wag4j40g87uhvk8d; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_transactions
    ADD CONSTRAINT fkjl0dwyeb8wag4j40g87uhvk8d FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: availabilities fkjtx165gb8k2fttx6fd955pxj9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.availabilities
    ADD CONSTRAINT fkjtx165gb8k2fttx6fd955pxj9 FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: password_reset_tokens fkk3ndxg5xp6v7wd4gjyusp15gq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fkk3ndxg5xp6v7wd4gjyusp15gq FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: invoices fkksa24bhmoffofjqlqnu0t37d6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fkksa24bhmoffofjqlqnu0t37d6 FOREIGN KEY (original_invoice_id) REFERENCES public.invoices(id);


--
-- Name: teacher_balances fklqu7yp0yk4fyccsnicdgh9giq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.teacher_balances
    ADD CONSTRAINT fklqu7yp0yk4fyccsnicdgh9giq FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: favorite_teachers fkn684or1kwk5awsos3793m6f7a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorite_teachers
    ADD CONSTRAINT fkn684or1kwk5awsos3793m6f7a FOREIGN KEY (teacher_id) REFERENCES public.users(id);


--
-- Name: student_wallets fkp81kghn7yx8n9wyvcqkmnjk93; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.student_wallets
    ADD CONSTRAINT fkp81kghn7yx8n9wyvcqkmnjk93 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: payments fkrw6097avxrnm0ymrdxhlmsql8; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkrw6097avxrnm0ymrdxhlmsql8 FOREIGN KEY (payer_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--


