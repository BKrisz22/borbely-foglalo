import { CalendarOptions } from '@fullcalendar/angular';
import interactionPlugin from '@fullcalendar/angular/interaction';
import themePlugin from '@fullcalendar/angular/themes/monarch';
import timeGridPlugin from '@fullcalendar/angular/timegrid';
// Az @fullcalendar/angular saját locales exportja hiányzó fájlra mutat, ezért innen jön.
import huLocale from 'fullcalendar/locales/hu';

import { SHOP_TIME_ZONE } from '../../core/time/shop-time';

/** Keskeny kijelzőn (telefonon) napi nézettel indul. */
const NARROW_SCREEN = '(max-width: 719px)';

/**
 * A naptár és a beosztás közös beállításai: idősáv-rács 7 és 21 óra között, magyarul,
 * hétfővel kezdődő héttel, az üzlet idejében (a böngésző időzónájától függetlenül).
 */
export function baseCalendarOptions(): CalendarOptions {
  const narrow = typeof matchMedia === 'function' && matchMedia(NARROW_SCREEN).matches;
  return {
    plugins: [themePlugin, timeGridPlugin, interactionPlugin],
    locale: huLocale,
    timeZone: SHOP_TIME_ZONE,
    firstDay: 1,
    initialView: narrow ? 'timeGridDay' : 'timeGridWeek',
    headerToolbar: { start: 'prev,next today', center: 'title', end: 'timeGridWeek,timeGridDay' },
    allDaySlot: false,
    slotMinTime: '07:00',
    slotMaxTime: '21:00',
    slotDuration: '00:30',
    nowIndicator: true,
    height: 'auto',
    longPressDelay: 400,
  };
}
