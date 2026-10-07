SET search_path TO public;

CREATE OR REPLACE FUNCTION public.prevent_logistics_event_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'logistics_event is append-only; % is not permitted', TG_OP
        USING ERRCODE = '55000';
END;
$$;

DROP TRIGGER IF EXISTS trg_logistics_event_append_only ON public.logistics_event;

CREATE TRIGGER trg_logistics_event_append_only
BEFORE UPDATE OR DELETE ON public.logistics_event
FOR EACH ROW
EXECUTE FUNCTION public.prevent_logistics_event_mutation();
