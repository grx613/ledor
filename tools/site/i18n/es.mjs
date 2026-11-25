// Español. Primera traducción — debe revisarla un hablante nativo.
export default {
  lang: 'es', dir: 'ltr', ogLocale: 'es_ES', name: 'Español', letters: false,

  meta: {
    title: 'LeDor Kalendář — calendario hebreo de cumpleaños y yahrzeits para toda la familia',
    description: 'App gratuita para la familia y la comunidad: cumpleaños hebreos, yahrzeits y aniversarios según los calendarios hebreo y gregoriano, lista compartida sincronizada por PIN y recordatorios la víspera, antes de la puesta de sol.',
    ogTitle: 'LeDor — recuerda a toda la familia los cumpleaños y los yahrzeits',
    ogDesc: 'Cumpleaños hebreos, yahrzeits y aniversarios. Lista compartida por PIN, recordatorios antes de la puesta de sol. Gratis, sin anuncios.',
  },
  nav: { download: 'Descargar', lang: 'Idioma' },
  stores: { soon: 'Pronto en', apk: 'Descargar para Android', apkNote: 'Archivo APK · Android 8.0 o posterior' },
  lists: ['Familia Cohen', 'Jabad de Nueva York', 'Comunidad Bet Shalom'],

  hero: {
    h1: 'Recuerda a toda la familia los cumpleaños y los yahrzeits',
    lead: 'LeDor lleva las fechas familiares según los calendarios hebreo y gregoriano y te las recuerda con antelación. Y la lista se comparte fácilmente con la familia o la comunidad: todos tienen las mismas fechas en el móvil.',
    note: 'Gratis · sin anuncios · sin registro',
    alt: 'Calendario de LeDor: fechas hebreas y gregorianas, próximos eventos de las listas de la familia y la comunidad',
    listsCap: 'Tus listas', now: 'ahora',
  },

  remind: {
    kicker: 'Recordatorios',
    h2: 'Un recordatorio la víspera — y el mismo día',
    sub: 'Una fecha hebrea cae cada año en un día distinto, y el día empieza al anochecer. LeDor hace las cuentas y te avisa cuando aún estás a tiempo.',
    points: [
      { icon: 'candle', t: 'La víspera, antes de la puesta de sol', p: 'Por defecto, tres horas antes de la puesta de sol en tu ciudad: tiempo para llamar, preparar la mesa o encender una vela de recuerdo.' },
      { icon: 'bell', t: 'Y otra vez el mismo día', p: 'A mediodía, o a la hora que elijas.' },
      { icon: 'star', t: 'Según el calendario hebreo, el gregoriano o ambos', p: 'Para cada evento decides qué fecha celebrar.' },
    ],
    cap: { eve: 'La víspera · 3 h antes de la puesta de sol', day: 'El mismo día · 12:00', yz: 'La víspera de un yahrzeit' },
    notifs: {
      eve: { title: 'Esta noche: Cumpleaños — Sara Cohen', text: 'Con la puesta de sol empieza el 19 Kislev 5787. Cumple 37 años' },
      day: { title: 'Hoy: Cumpleaños — Sara Cohen', text: '19 Kislev 5787 · 29 de noviembre de 2026. Cumple 37 años' },
      yz: { title: 'Yahrzeit esta noche: Abuelo Aarón', text: 'Con la puesta de sol empieza el 20 Kislev 5787. Es hora de encender la vela de recuerdo.' },
    },
  },

  share: {
    kicker: 'Listas compartidas',
    h2: 'Comparte la lista con la familia o la comunidad',
    sub: 'Una persona apunta las fechas y todos las ven y reciben recordatorios. Sin registro, correo ni contraseñas.',
    steps: [
      { t: 'Crea una lista', p: '«Familia Cohen», «Nuestra comunidad», «Amigos»: puedes tener varias listas.' },
      { t: 'Toca «Compartir»', p: 'LeDor envía una invitación por WhatsApp o Telegram con el nombre de la lista y el código de acceso.' },
      { t: 'Tu familia se conecta', p: 'Instalan LeDor, introducen el nombre y el código, y ven las mismas fechas, con recordatorios en su móvil.' },
    ],
    points: [
      '<b>Tú decides quién edita.</b> Un código solo para ver y otro para añadir y corregir fechas.',
      '<b>Varias listas a la vez.</b> Familia, comunidad, amigos: cada lista con su color y sus notificaciones.',
      '<b>Funciona sin conexión.</b> Los cambios se envían cuando vuelve la conexión.',
      '<b>Sin cuentas.</b> Ni correo, ni teléfono, ni contraseña.',
    ],
    sync: 'Los cambios llegan cuando alguien abre la app o desliza la lista hacia abajo, y una vez al día en segundo plano.',
    msgCap: 'Invitación por WhatsApp o Telegram',
    msg: ['La lista «Familia Cohen» en la app LeDor: cumpleaños, yahrzeits y eventos familiares según los calendarios hebreo y gregoriano.', '1. Instala la app: kalendar.bothub.chat', '2. Abre «Listas» → «Conectar una lista»', '3. Nombre: Familia Cohen', '4. PIN: 4821'],
    altLists: 'Varias listas en LeDor: personal, familiar y de dos comunidades',
    altEvents: 'Eventos de todas las listas, cada uno con el nombre de su lista',
  },

  features: {
    kicker: 'Y además',
    h2: 'Pensada para el calendario hebreo',
    items: [
      { icon: 'star', t: 'Las dos fechas, una junto a la otra', p: 'Cada evento se muestra según el calendario hebreo y el gregoriano, en el calendario y en la lista.' },
      { icon: 'scroll', t: 'Yahrzeit según las reglas', p: 'Adar I y Adar II en año bisiesto, el 30 de Jeshván y Kislev: todo contemplado. La costumbre para Adar se elige en ajustes.' },
      { icon: 'bar', t: 'Bar mitzvá y bat mitzvá', p: 'Introduce una fecha de nacimiento y LeDor te dirá las fechas de bar mitzvá y bat mitzvá.' },
      { icon: 'battery', t: 'No gasta batería', p: 'Sin trabajo en segundo plano ni ubicación: solo se despierta en el momento del recordatorio.' },
      { icon: 'globe', t: 'Seis idiomas', p: 'English, עברית, Русский, ייִדיש, Français, Español. En hebreo e ídish, las fechas se escriben con letras.' },
      { icon: 'heart', t: 'Gratis y abierta', p: 'Sin anuncios, suscripciones ni recopilación de datos. El código fuente es abierto.' },
    ],
  },

  cta: { h2: 'Instala LeDor y las fechas importantes dejarán de escaparse', p: 'Gratis, sin anuncios ni registro.' },

  shots: {
    h2: 'Así es la app', prev: 'Anterior', next: 'Siguiente',
    alt: {
      '03_calendar': 'Calendario por meses hebreos: ambas fechas en cada casilla',
      '05_events': 'Eventos de todas las listas, agrupados por mes hebreo',
      '14_list_share': 'Lista familiar compartida: PIN y envío a familiares',
      '06_detail': 'Ficha del evento: próxima fecha y edad',
      '08_editor': 'Nuevo evento: tipo, fecha en cualquier calendario, según ambos',
      '07_converter': 'Conversor de fechas con la puesta de sol',
    },
  },

  faq: {
    h2: 'Preguntas frecuentes',
    items: [
      { q: '¿Cómo comparto una lista con mi familia?', a: 'Crea una lista compartida en «Listas» y toca Telegram o WhatsApp: tu familia recibe un mensaje con el nombre de la lista, el PIN y un enlace a la app.' },
      { q: '¿Hay que registrarse? ¿Recopiláis mis datos?', a: 'No hay registro. La app no tiene anuncios, analítica ni rastreadores. La lista personal se queda en tu móvil; una lista compartida se guarda en nuestro servidor, solo para sus miembros.' },
      { q: '¿Qué diferencia hay entre el PIN y el PIN maestro?', a: 'El PIN normal permite ver las fechas y recibir recordatorios. El PIN maestro permite añadir, cambiar y eliminar eventos. Dalo solo a personas de confianza.' },
      { q: '¿Puedo celebrar un cumpleaños según ambos calendarios?', a: 'Sí. Elige «Según ambos calendarios»: habrá dos fechas al año y LeDor te recordará cada una. Hace falta el año de nacimiento para convertir la fecha.' },
      { q: '¿Se tiene en cuenta la puesta de sol?', a: 'Sí. Al introducir una fecha puedes marcar «después del ocaso»: entonces la fecha hebrea es la siguiente. El recordatorio de la víspera llega el tiempo que elijas antes de la puesta de sol en tu ciudad. ¿No sabes si ya se había puesto el sol? Indica la hora y la ciudad y la app lo calcula.' },
      { q: '¿Cómo se calcula un yahrzeit en año bisiesto?', a: 'Para quien falleció en Adar de un año común: según la costumbre asquenazí, en Adar I; según la sefardí, en Adar II; la costumbre se elige en ajustes. También se aplican las reglas del 30 de Jeshván y Kislev.' },
      { q: '¿Funciona la app sin internet?', a: 'Sí. Internet solo hace falta para las listas compartidas; los cambios se envían cuando vuelve la conexión.' },
      { q: '¿Cuánta energía consume la app?', a: 'Prácticamente nada. No hay servicios en segundo plano, ni ubicación, ni conexión permanente. El móvil despierta LeDor una fracción de segundo solo en el momento del recordatorio —la víspera y el mismo día— y, si usas una lista compartida, una vez al día para sincronizar, cuando al sistema le conviene.' },
      { q: '¿En qué idiomas está LeDor?', a: 'English, עברית, Русский, ייִדיש, Français, Español. En hebreo e ídish la interfaz va de derecha a izquierda y las fechas hebreas se escriben con letras.' },
      { q: '¿Puedo pasar mis eventos a una lista compartida?', a: 'Sí. Al crear una lista, marca «Mover eventos de la lista personal», o abre la lista personal y elige «Mover eventos a una lista en la nube».' },
      { q: '¿Hay tema oscuro?', a: 'Sí: claro, oscuro o según el sistema, en «Ajustes → Apariencia».' },
      { q: '¿Muestra LeDor las fiestas judías y la hora del encendido de velas?', a: 'Sí. En «Listas» hay una lista integrada «Fiestas judías»: las fiestas y los ayunos aparecen en el calendario, con un aviso la víspera. Para Shabat y las fiestas se muestra la hora del encendido de velas en tu ciudad; el aviso de Shabat se activa aparte. Puedes desactivar los avisos y simplemente consultar las fechas.' },
      { q: '¿Hay versión para iPhone?', a: 'Pronto. Por ahora LeDor está disponible para Android.' },
      { q: '¿Cuánto cuesta la app?', a: 'Es gratis, sin anuncios ni contenido de pago. El proyecto no es comercial y es de código abierto.' },
    ],
  },

  author: {
    h2: 'Sobre el autor', name: 'Gershon',
    p: 'Autor de LeDor. ¿Has encontrado un error en una fecha o tienes una idea? Escribe desde la app: «Más → Enviar comentarios».',
    channel: 'Canal de Telegram del autor', torahAlt: 'Gershon con un rollo de la Torá',
  },
  eco: {
    h2: 'Otros proyectos de BotHub',
    bothub: 'ChatGPT, Claude, Gemini, Grok y otros modelos de IA en un solo lugar, sin suscripción.',
    fire: 'Lo esencial de los mercados: acciones, tipos, petróleo, cripto, en resúmenes de decenas de medios.',
  },
  footer: { privacy: 'Política de privacidad', delete: 'Eliminar datos', converter: 'Conversor de fechas', faq: 'Preguntas frecuentes', copy: 'proyecto no comercial · sin anuncios ni rastreadores' },

  conv: {
    title: 'Conversor de fecha hebrea — cumpleaños hebreo, bar mitzvá, yahrzeit | LeDor',
    description: 'Conversor de fecha hebrea gratuito: convierte una fecha de nacimiento al calendario hebreo teniendo en cuenta la puesta de sol y viceversa, y calcula bar mitzvá, bat mitzvá y yahrzeit para 10 años.',
    h1: 'Conversor de fecha hebrea',
    lead: 'Tiene en cuenta la puesta de sol, los años bisiestos y las reglas de los aniversarios. El resultado incluye las fechas de bar mitzvá, bat mitzvá y yahrzeit.',
    g2h: 'Gregoriana → hebrea', h2g: 'Hebrea → gregoriana', day: 'Día', month: 'Mes', year: 'Año',
    before: 'Antes de la puesta de sol', after: 'Después del ocaso', dirLabel: 'Dirección de la conversión', timeLabel: 'Momento del día',
    noscript: 'Activa JavaScript para usar el conversor.', app: 'Descargar la app',
    prose: `<h2>Cómo convertir una fecha de nacimiento al calendario hebreo</h2>
<p>El día hebreo no empieza a medianoche, sino con la puesta de sol. Por eso, para quien nació por la noche, la fecha hebrea ya es la siguiente. Si no se conoce la hora de nacimiento, elige «antes de la puesta de sol»: es lo más habitual.</p>
<h3>Por qué el cumpleaños hebreo «se mueve»</h3>
<p>El año hebreo es lunisolar: los meses siguen a la luna y, cada pocos años, se añade un decimotercer mes, Adar II. Por eso una misma fecha hebrea cae cada año en una fecha gregoriana distinta.</p>
<h2>Cómo calcular un yahrzeit</h2>
<p>El yahrzeit es el aniversario del fallecimiento de un ser querido según el calendario hebreo. La vela de recuerdo se enciende la víspera por la noche. Si alguien falleció en Adar de un año común, en año bisiesto la costumbre asquenazí lo fija en Adar I. Para el 30 de Jeshván y Kislev rige la regla del primer aniversario. El conversor lo tiene todo en cuenta.</p>
<h2>Cómo calcular la fecha de bar mitzvá y bat mitzvá</h2>
<p>El bar mitzvá es a los 13 años y el bat mitzvá a los 12, en el cumpleaños hebreo. Para quienes nacieron en Adar de un año bisiesto, la fecha sigue al último Adar.</p>`,
    faq: [
      { q: '¿Cómo convierto una fecha de nacimiento al calendario hebreo?', a: 'Introduce la fecha e indica si fue antes o después de la puesta de sol: un nacimiento tras el ocaso corresponde a la fecha hebrea siguiente.' },
      { q: '¿Cuándo es el bar mitzvá y el bat mitzvá?', a: 'El bar mitzvá a los 13 años y el bat mitzvá a los 12, según el calendario hebreo, en el cumpleaños hebreo.' },
      { q: '¿Cuándo se enciende la vela del yahrzeit?', a: 'La víspera por la noche: la fecha hebrea empieza con la puesta de sol.' },
    ],
  },

  js: {
    hebLabel: 'Fecha hebrea', gregLabel: 'Fecha gregoriana',
    evening: ' por la noche — el día hebreo ya ha empezado',
    eveNote: 'El día hebreo empieza la víspera, con la puesta de sol del {date}.',
    errGYear: 'Introduce un año entre 1600 y 2200.', errNoDate: 'Esa fecha no existe.',
    errHYear: 'Introduce un año hebreo entre 5360 y 5960.', errMonthDays: '{month} de {year} tiene {n} días.',
    nextBday: 'Próximo cumpleaños hebreo: <b>{heb}</b> — {greg}', turns: { one: ', cumple {n} año', many: ', cumple {n} años', other: ', cumple {n} años' },
    bar: 'Bar mitzvá (13 años)', bat: 'Bat mitzvá (12 años)',
    ifBirth: 'Si es una fecha de nacimiento', ifDeath: 'Si es una fecha de fallecimiento (yahrzeit)',
    nextYz: 'Próximo yahrzeit: <b>{heb}</b> — {greg}. La vela se enciende la víspera por la noche.', yzNextYear: 'El yahrzeit empieza el año siguiente a la fecha.',
    ctaDays: { one: 'Falta {n} día para el próximo cumpleaños hebreo.', many: 'Faltan {n} días para el próximo cumpleaños hebreo.', other: 'Faltan {n} días para el próximo cumpleaños hebreo.' },
    ctaApp: 'La app LeDor te lo recordará la víspera y el mismo día.',
    monthsNom: { NISAN: 'Nisán', IYAR: 'Iyar', SIVAN: 'Siván', TAMMUZ: 'Tamuz', AV: 'Av', ELUL: 'Elul', TISHREI: 'Tishrei', CHESHVAN: 'Jeshván', KISLEV: 'Kislev', TEVET: 'Tevet', SHEVAT: 'Shevat', ADAR: 'Adar', ADAR_I: 'Adar I', ADAR_II: 'Adar II' },
    monthsGen: { NISAN: 'Nisán', IYAR: 'Iyar', SIVAN: 'Siván', TAMMUZ: 'Tamuz', AV: 'Av', ELUL: 'Elul', TISHREI: 'Tishrei', CHESHVAN: 'Jeshván', KISLEV: 'Kislev', TEVET: 'Tevet', SHEVAT: 'Shevat', ADAR: 'Adar', ADAR_I: 'Adar I', ADAR_II: 'Adar II' },
  },

  privacy: {
    title: 'Política de privacidad — LeDor',
    description: 'Qué datos trata la app LeDor, dónde se guardan, durante cuánto tiempo y cómo eliminarlos.',
    h1: 'Política de privacidad',
    updated: 'Versión del 24 de noviembre de 2026',
    body: `<p>Esta política explica qué datos tratan la app móvil LeDor y el sitio kalendar.bothub.chat, para qué, durante cuánto tiempo se conservan y cómo controlarlos.</p>

<h2>1. Responsable</h2>
<p>LeDor es un proyecto no comercial y de código abierto. El responsable del tratamiento es el autor de la app, una persona física (Gershon). Contacto: el formulario «Más → Enviar comentarios» de la app o el <a href="https://t.me/bothub_founder">canal de Telegram del autor</a>.</p>

<h2>2. En resumen</h2>
<ul>
<li>Sin registro, anuncios, analítica ni rastreadores.</li>
<li>No vendemos datos ni los cedemos a terceros con fines publicitarios o de marketing.</li>
<li>La app no pide ubicación, contactos, fotos ni otros permisos que no necesita.</li>
</ul>

<h2>3. Lo que se guarda solo en tu móvil</h2>
<ul>
<li>tu lista personal de eventos (nombres, fechas, notas);</li>
<li>los ajustes, la ciudad elegida para la puesta de sol y el idioma;</li>
<li>los PIN de las listas compartidas conectadas.</li>
</ul>
<p>Estos datos no salen del móvil, salvo que muevas eventos a una lista compartida.</p>

<h2>4. Lo que se guarda en el servidor</h2>
<p><b>Listas compartidas</b> — solo si creas o conectas una lista compartida: nombre de la lista, eventos (nombre o título, tipo, fechas, nota), marcas de tiempo y versiones técnicas. El PIN normal se guarda para que los editores puedan compartir la lista; el PIN maestro, solo como hash irreversible. Finalidad: sincronizar la lista entre sus miembros.</p>
<p><b>Comentarios</b> — si envías un comentario: su texto y, solo si los indicaste o lo permitiste, un contacto para responder y datos técnicos (versión de la app, modelo del móvil, versión de Android, idioma). Los eventos y los PIN nunca se incluyen. Finalidad: responderte y mejorar la app. El comentario se reenvía al autor por Telegram.</p>
<p><b>Datos técnicos</b> — cada petición al servidor implica tu dirección IP. Para proteger contra la adivinación de PIN, solo se guarda su hash irreversible.</p>
<p>Los datos se guardan en el servidor del desarrollador; las conexiones van cifradas (HTTPS).</p>

<h2>5. Plazos de conservación</h2>
<ul>
<li>una lista compartida y sus eventos — hasta que se elimina la lista;</li>
<li>comentarios — 2 años como máximo;</li>
<li>hash de la dirección IP — un día como máximo;</li>
<li>copias de seguridad de la base — 14 días.</li>
</ul>

<h2>6. Bases jurídicas</h2>
<p>Listas compartidas y sincronización — prestar lo que pides al crear o conectar una lista. Comentarios — tu consentimiento, dado al enviarlos. Protección contra la adivinación de PIN — nuestro interés legítimo en la seguridad del servicio.</p>

<h2>7. Quién ve una lista compartida</h2>
<p>Cualquiera que conozca su nombre y su PIN. Tú decides a quién das el PIN y respondes del consentimiento de las personas cuyas fechas añades.</p>

<h2>8. Destinatarios</h2>
<p>Nadie, salvo obligación legal. El sitio usa la red de Cloudflare, que trata técnicamente las direcciones IP de los visitantes. Los comentarios llegan al autor por Telegram.</p>

<h2 id="delete">9. Tus derechos y eliminación de datos</h2>
<p>Puedes saber qué datos tuyos se guardan, rectificarlos o eliminarlos.</p>
<ul>
<li><b>Una lista compartida entera:</b> «Listas» → la lista → «Eliminar la lista de la nube» → PIN maestro. La lista se elimina para siempre para todos los miembros.</li>
<li><b>Un solo evento:</b> abre el evento → «Eliminar». Desaparece para todos tras la sincronización.</li>
<li><b>Desconectar una lista del móvil:</b> «Listas» → la lista → «Desconectar».</li>
<li><b>Todo en el móvil:</b> desinstala la app o borra sus datos en los ajustes de Android.</li>
<li><b>Un comentario, un PIN maestro perdido u otras solicitudes:</b> escribe con «Enviar comentarios» en la app, indicando el nombre de la lista y un contacto. Respondemos en 30 días.</li>
</ul>
<p>Si estás en la UE, también puedes reclamar ante una autoridad de protección de datos (en España, la AEPD).</p>

<h2>10. Copias de seguridad de Android</h2>
<p>Los datos de la app pueden incluirse en la copia de seguridad del móvil en tu cuenta de Google; depende de los ajustes del móvil.</p>

<h2>11. Menores</h2>
<p>La app no está dirigida a menores de 13 años y no recopila sus datos a sabiendas.</p>

<h2>12. Cambios</h2>
<p>Si la política cambia, la nueva versión aparecerá en esta página con una nueva fecha. Los cambios importantes se anunciarán en la app.</p>`,
  },

  root: { choose: 'Elige un idioma' },
};
