// Français. Première traduction — à relire par un locuteur natif.
export default {
  lang: 'fr', dir: 'ltr', ogLocale: 'fr_FR', name: 'Français', letters: false,

  meta: {
    title: 'LeDor Kalendář — calendrier hébraïque des anniversaires et yahrzeits pour toute la famille',
    description: 'Application gratuite pour la famille et la communauté : anniversaires hébraïques, yahrzeits et dates de famille selon les calendriers hébraïque et grégorien, liste partagée synchronisée par code PIN et rappels la veille avant le coucher du soleil.',
    ogTitle: 'LeDor — rappelle à toute la famille les anniversaires et les yahrzeits',
    ogDesc: 'Anniversaires hébraïques, yahrzeits et dates de famille. Liste partagée par code PIN, rappels avant le coucher du soleil. Gratuit, sans publicité.',
  },
  nav: { download: 'Télécharger', lang: 'Langue' },
  stores: { soon: 'Bientôt sur', apk: 'Télécharger pour Android', apkNote: 'Fichier APK · Android 8.0 ou plus récent' },
  lists: ['Famille Cohen', 'Habad de New York', 'Communauté Beth Shalom'],

  hero: {
    h1: 'Rappelle à toute la famille les anniversaires et les yahrzeits',
    lead: 'LeDor suit les dates de famille selon les calendriers hébraïque et grégorien et vous les rappelle à l’avance. Et la liste se partage facilement avec la famille ou la communauté — chacun a les mêmes dates sur son téléphone.',
    note: 'Gratuit · sans publicité · sans inscription',
    alt: 'Calendrier LeDor : dates hébraïques et grégoriennes, prochains événements des listes de la famille et de la communauté',
    listsCap: 'Vos listes', now: 'maintenant',
  },

  remind: {
    kicker: 'Rappels',
    h2: 'Un rappel la veille au soir — et le jour même',
    sub: 'Une date hébraïque tombe chaque année un jour différent, et le jour commence le soir. LeDor fait le calcul et vous prévient tant qu’il est encore temps.',
    points: [
      { icon: 'candle', t: 'La veille, avant le coucher du soleil', p: 'Par défaut trois heures avant le coucher du soleil dans votre ville : le temps d’appeler, de dresser la table ou d’allumer une bougie du souvenir.' },
      { icon: 'bell', t: 'Et encore le jour même', p: 'À midi, ou à l’heure de votre choix.' },
      { icon: 'star', t: 'Selon le calendrier hébraïque, grégorien, ou les deux', p: 'Pour chaque événement, vous décidez quelle date célébrer.' },
    ],
    cap: { eve: 'La veille au soir · 3 h avant le coucher du soleil', day: 'Le jour même · 12:00', yz: 'La veille d’une yahrzeit' },
    notifs: {
      eve: { title: 'Ce soir : Anniversaire — Sarah Cohen', text: 'Le 19 Kislev 5787 commence au coucher du soleil. Fête ses 37 ans' },
      day: { title: 'Aujourd’hui : Anniversaire — Sarah Cohen', text: '19 Kislev 5787 · 29 novembre 2026. Fête ses 37 ans' },
      yz: { title: 'Yahrzeit ce soir : Grand-père Aaron', text: 'Le 20 Kislev 5787 commence au coucher du soleil. C’est l’heure d’allumer la bougie du souvenir.' },
    },
  },

  share: {
    kicker: 'Listes partagées',
    h2: 'Partagez la liste avec la famille ou la communauté',
    sub: 'Une personne saisit les dates — tous les voient et reçoivent les rappels. Sans inscription, e-mail ni mot de passe.',
    steps: [
      { t: 'Créez une liste', p: '« Famille Cohen », « Notre communauté », « Amis » — vous pouvez avoir plusieurs listes.' },
      { t: 'Touchez « Partager »', p: 'LeDor envoie une invitation par WhatsApp ou Telegram, avec le nom de la liste et le code d’accès.' },
      { t: 'Vos proches se connectent', p: 'Ils installent LeDor, saisissent le nom et le code — et voient les mêmes dates, avec des rappels sur leur téléphone.' },
    ],
    points: [
      '<b>Vous décidez qui modifie.</b> Un code pour consulter, un autre pour ajouter et corriger des dates.',
      '<b>Plusieurs listes à la fois.</b> Famille, communauté, amis — chaque liste a sa couleur et ses notifications.',
      '<b>Fonctionne hors ligne.</b> Les modifications partent dès que la connexion revient.',
      '<b>Sans compte.</b> Ni e-mail, ni téléphone, ni mot de passe.',
    ],
    sync: 'Les modifications arrivent quand on ouvre l’application ou tire la liste vers le bas, et une fois par jour en arrière-plan.',
    msgCap: 'Invitation par WhatsApp ou Telegram',
    msg: ['La liste « Famille Cohen » dans l’application LeDor : anniversaires, yahrzeits et événements familiaux selon les calendriers hébraïque et grégorien.', '1. Installez l’application : kalendar.bothub.chat', '2. Ouvrez « Listes » → « Connecter une liste »', '3. Nom : Famille Cohen', '4. Code PIN : 4821'],
    altLists: 'Plusieurs listes dans LeDor : personnelle, familiale et deux communautés',
    altEvents: 'Événements de toutes les listes, chacun avec le nom de sa liste',
  },

  features: {
    kicker: 'Et aussi',
    h2: 'Pensé pour le calendrier hébraïque',
    items: [
      { icon: 'star', t: 'Les deux dates côte à côte', p: 'Chaque événement s’affiche selon les calendriers hébraïque et grégorien — dans le calendrier et dans la liste.' },
      { icon: 'scroll', t: 'Yahrzeit selon les règles', p: 'Adar I et Adar II en année embolismique, le 30 Hechvan et Kislev — tout est pris en compte. La coutume pour Adar se règle dans les réglages.' },
      { icon: 'bar', t: 'Bar-mitsva et bat-mitsva', p: 'Saisissez une date de naissance — LeDor indique les dates de bar-mitsva et de bat-mitsva.' },
      { icon: 'battery', t: 'Économe en batterie', p: 'Aucun travail en arrière-plan ni géolocalisation : il ne se réveille qu’au moment d’un rappel.' },
      { icon: 'globe', t: 'Six langues', p: 'English, עברית, Русский, ייִדיש, Français, Español. En hébreu et en yiddish, les dates s’écrivent en lettres.' },
      { icon: 'heart', t: 'Gratuit et ouvert', p: 'Sans publicité, abonnement ni collecte de données. Le code source est ouvert.' },
    ],
  },

  cta: { h2: 'Installez LeDor — et les dates importantes ne vous échapperont plus', p: 'Gratuit, sans publicité ni inscription.' },

  shots: {
    h2: 'À quoi ressemble l’application', prev: 'Précédent', next: 'Suivant',
    alt: {
      '03_calendar': 'Calendrier par mois hébraïques : les deux dates dans chaque case',
      '05_events': 'Événements de toutes les listes, groupés par mois hébraïque',
      '14_list_share': 'Liste familiale partagée : codes PIN et envoi aux proches',
      '06_detail': 'Fiche d’événement : prochaine date et âge',
      '08_editor': 'Nouvel événement : type, date dans l’un ou l’autre calendrier, selon les deux',
      '07_converter': 'Convertisseur de dates tenant compte du coucher du soleil',
    },
  },

  faq: {
    h2: 'Questions fréquentes',
    items: [
      { q: 'Comment partager une liste avec ma famille ?', a: 'Créez une liste partagée dans « Listes » et touchez Telegram ou WhatsApp — vos proches reçoivent un message avec le nom de la liste, le code PIN et un lien vers l’application.' },
      { q: 'Faut-il s’inscrire ? Collectez-vous mes données ?', a: 'Aucune inscription. L’application n’a ni publicité, ni statistiques, ni traceurs. La liste personnelle reste sur votre téléphone ; une liste partagée est conservée sur notre serveur, pour ses seuls membres.' },
      { q: 'Quelle différence entre le code PIN et le code maître ?', a: 'Le code ordinaire permet de consulter les dates et de recevoir les rappels. Le code maître permet d’ajouter, modifier et supprimer des événements. Ne le donnez qu’à des personnes de confiance.' },
      { q: 'Peut-on fêter un anniversaire selon les deux calendriers ?', a: 'Oui. Choisissez « Selon les deux calendriers » — il y aura deux dates par an et LeDor rappellera chacune. L’année de naissance est nécessaire pour convertir la date.' },
      { q: 'Le coucher du soleil est-il pris en compte ?', a: 'Oui. En saisissant une date, vous pouvez indiquer « après le coucher » — la date hébraïque est alors la suivante. Le rappel de la veille arrive le temps choisi avant le coucher du soleil dans votre ville. Vous ne savez pas si le soleil était couché ? Indiquez l’heure et la ville — l’application le calcule.' },
      { q: 'Comment se calcule une yahrzeit en année embolismique ?', a: 'Pour un décès en Adar d’une année simple : en Adar I selon la coutume ashkénaze, en Adar II selon la coutume séfarade ; la coutume se choisit dans les réglages. Les règles du 30 Hechvan et Kislev sont aussi appliquées.' },
      { q: 'L’application fonctionne-t-elle hors ligne ?', a: 'Oui. Internet n’est nécessaire que pour les listes partagées ; les modifications partent dès que la connexion revient.' },
      { q: 'Combien d’énergie consomme l’application ?', a: 'Presque rien. Pas de service en arrière-plan, pas de géolocalisation, pas de connexion permanente. Le téléphone réveille LeDor une fraction de seconde au moment d’un rappel — la veille et le jour même — et, si vous utilisez une liste partagée, une fois par jour pour la synchronisation, quand le système le juge opportun.' },
      { q: 'Dans quelles langues LeDor est-il disponible ?', a: 'English, עברית, Русский, ייִדיש, Français, Español. En hébreu et en yiddish, l’interface s’affiche de droite à gauche et les dates hébraïques sont écrites en lettres.' },
      { q: 'Puis-je transférer mes événements vers une liste partagée ?', a: 'Oui. En créant une liste, cochez « Transférer les événements de la liste personnelle », ou ouvrez la liste personnelle et choisissez « Transférer les événements vers une liste cloud ».' },
      { q: 'Y a-t-il un thème sombre ?', a: 'Oui : clair, sombre ou selon le système — dans « Réglages → Apparence ».' },
      { q: 'LeDor affiche-t-il les fêtes juives et l’heure d’allumage des bougies ?', a: 'Oui. Dans « Listes », une liste intégrée « Fêtes juives » (désactivée par défaut : activez-la avec son interrupteur) affiche les fêtes et les jeûnes dans le calendrier, avec un rappel la veille au soir. Pour Chabbat et les fêtes, l’heure d’allumage des bougies dans votre ville est indiquée ; le rappel de Chabbat s’active séparément. Vous pouvez désactiver les rappels et simplement consulter les dates.' },
      { q: 'Existe-t-il une version iPhone ?', a: 'Bientôt. LeDor est pour l’instant disponible sur Android.' },
      { q: 'Combien coûte l’application ?', a: 'Elle est gratuite, sans publicité ni contenu payant. Le projet est non commercial et open source.' },
    ],
  },

  author: {
    h2: 'L’auteur', name: 'Gershon',
    p: 'Auteur de LeDor. Une erreur de date ou une idée ? Écrivez directement depuis l’application : « Plus → Envoyer un avis ».',
    channel: 'Chaîne Telegram de l’auteur', photoAlt: 'Gershon avec un loulav dans la soukka',
  },
  eco: {
    h2: 'Autres projets BotHub',
    bothub: 'ChatGPT, Claude, Gemini, Grok et d’autres IA au même endroit — sans abonnement.',
    fire: 'L’essentiel de l’actualité des marchés : actions, taux, pétrole, crypto — en bref, à partir de dizaines de médias.',
  },
  footer: { privacy: 'Politique de confidentialité', delete: 'Supprimer les données', converter: 'Convertisseur de dates', faq: 'Questions fréquentes', copy: 'projet non commercial · sans publicité ni traceurs' },

  conv: {
    title: 'Convertisseur de date hébraïque — anniversaire, bar-mitsva, yahrzeit | LeDor',
    description: 'Convertisseur de date hébraïque gratuit : convertir une date de naissance en date hébraïque en tenant compte du coucher du soleil et inversement, trouver les dates de bar-mitsva, bat-mitsva et yahrzeit sur 10 ans.',
    h1: 'Convertisseur de date hébraïque',
    lead: 'Tient compte du coucher du soleil, des années embolismiques et des règles des anniversaires. Le résultat inclut les dates de bar-mitsva, bat-mitsva et yahrzeit.',
    g2h: 'Grégorien → hébraïque', h2g: 'Hébraïque → grégorien', day: 'Jour', month: 'Mois', year: 'Année',
    before: 'Avant le coucher du soleil', after: 'Après le coucher', dirLabel: 'Sens de conversion', timeLabel: 'Moment de la journée',
    noscript: 'Activez JavaScript pour utiliser le convertisseur.', app: 'Télécharger l’application',
    prose: `<h2>Comment convertir une date de naissance en date hébraïque</h2>
<p>Le jour hébraïque ne commence pas à minuit mais au coucher du soleil. Pour une personne née le soir, la date hébraïque est donc déjà la suivante. Si l’heure de naissance est inconnue, choisissez « avant le coucher du soleil » : c’est l’usage le plus courant.</p>
<h3>Pourquoi l’anniversaire hébraïque « se déplace »</h3>
<p>L’année hébraïque est luni-solaire : les mois suivent la lune et, tous les quelques ans, un treizième mois, Adar II, est ajouté. C’est pourquoi une même date hébraïque tombe chaque année à une date grégorienne différente.</p>
<h2>Comment calculer une yahrzeit</h2>
<p>La yahrzeit est l’anniversaire du décès d’un proche selon le calendrier hébraïque. La bougie du souvenir s’allume la veille au soir. Pour un décès en Adar d’une année simple, la coutume ashkénaze fixe la yahrzeit en Adar I lors d’une année embolismique. Pour le 30 Hechvan et Kislev s’applique la règle de la première année. Le convertisseur en tient compte.</p>
<h2>Comment calculer la date de bar-mitsva et de bat-mitsva</h2>
<p>La bar-mitsva a lieu à 13 ans, la bat-mitsva à 12 ans, le jour de l’anniversaire hébraïque. Pour les personnes nées en Adar d’une année embolismique, la date suit le dernier Adar.</p>`,
    faq: [
      { q: 'Comment convertir une date de naissance en date hébraïque ?', a: 'Saisissez la date et indiquez si c’était avant ou après le coucher du soleil : une naissance après le coucher correspond à la date hébraïque suivante.' },
      { q: 'Quand a lieu la bar-mitsva et la bat-mitsva ?', a: 'La bar-mitsva à 13 ans, la bat-mitsva à 12 ans selon le calendrier hébraïque, le jour de l’anniversaire hébraïque.' },
      { q: 'Quand allumer la bougie de yahrzeit ?', a: 'La veille au soir : la date hébraïque commence au coucher du soleil.' },
    ],
  },

  js: {
    hebLabel: 'Date hébraïque', gregLabel: 'Date grégorienne',
    evening: ' soir — le jour hébraïque a déjà commencé',
    eveNote: 'Le jour hébraïque commence la veille au soir, au coucher du soleil du {date}.',
    errGYear: 'Saisissez une année entre 1600 et 2200.', errNoDate: 'Cette date n’existe pas.',
    errHYear: 'Saisissez une année hébraïque entre 5360 et 5960.', errMonthDays: '{month} {year} compte {n} jours.',
    nextBday: 'Prochain anniversaire hébraïque : <b>{heb}</b> — {greg}', turns: { one: ', {n} an', many: ', {n} ans', other: ', {n} ans' },
    bar: 'Bar-mitsva (13 ans)', bat: 'Bat-mitsva (12 ans)',
    ifBirth: 'S’il s’agit d’une date de naissance', ifDeath: 'S’il s’agit d’une date de décès (yahrzeit)',
    nextYz: 'Prochaine yahrzeit : <b>{heb}</b> — {greg}. La bougie s’allume la veille au soir.', yzNextYear: 'La yahrzeit commence l’année suivant la date.',
    ctaDays: { one: '{n} jour avant le prochain anniversaire hébraïque.', many: '{n} jours avant le prochain anniversaire hébraïque.', other: '{n} jours avant le prochain anniversaire hébraïque.' },
    ctaApp: 'L’application LeDor vous le rappellera la veille au soir et le jour même.',
    monthsNom: { NISAN: 'Nissan', IYAR: 'Iyar', SIVAN: 'Sivan', TAMMUZ: 'Tamouz', AV: 'Av', ELUL: 'Eloul', TISHREI: 'Tichri', CHESHVAN: 'Hechvan', KISLEV: 'Kislev', TEVET: 'Tevet', SHEVAT: 'Chevat', ADAR: 'Adar', ADAR_I: 'Adar I', ADAR_II: 'Adar II' },
    monthsGen: { NISAN: 'Nissan', IYAR: 'Iyar', SIVAN: 'Sivan', TAMMUZ: 'Tamouz', AV: 'Av', ELUL: 'Eloul', TISHREI: 'Tichri', CHESHVAN: 'Hechvan', KISLEV: 'Kislev', TEVET: 'Tevet', SHEVAT: 'Chevat', ADAR: 'Adar', ADAR_I: 'Adar I', ADAR_II: 'Adar II' },
  },

  privacy: {
    title: 'Politique de confidentialité — LeDor',
    description: 'Quelles données l’application LeDor traite, où elles sont conservées, combien de temps et comment les supprimer.',
    h1: 'Politique de confidentialité',
    updated: 'Version du 24 novembre 2026',
    body: `<p>Cette politique explique quelles données l’application mobile LeDor et le site kalendar.bothub.chat traitent, pourquoi, combien de temps elles sont conservées et comment les contrôler.</p>

<h2>1. Responsable du traitement</h2>
<p>LeDor est un projet non commercial et open source. Le responsable du traitement est l’auteur de l’application, une personne physique (Gershon). Contact : e-mail <a href="mailto:grx613@gmail.com">grx613@gmail.com</a>, le formulaire « Plus → Envoyer un avis » dans l’application ou la <a href="https://t.me/bothub_founder">chaîne Telegram de l’auteur</a>.</p>

<h2>2. En bref</h2>
<ul>
<li>Pas d’inscription, de publicité, de statistiques ni de traceurs.</li>
<li>Nous ne vendons pas les données et ne les transmettons pas à des tiers à des fins publicitaires ou marketing.</li>
<li>L’application ne demande ni la localisation, ni les contacts, ni les photos, ni d’autres autorisations inutiles.</li>
</ul>

<h2>3. Ce qui reste uniquement sur votre téléphone</h2>
<ul>
<li>votre liste personnelle d’événements (noms, dates, notes) ;</li>
<li>les réglages, la ville choisie pour le coucher du soleil et la langue ;</li>
<li>les codes PIN des listes partagées connectées.</li>
</ul>
<p>Ces données ne quittent pas votre téléphone, sauf si vous transférez des événements vers une liste partagée.</p>

<h2>4. Ce qui est conservé sur le serveur</h2>
<p><b>Listes partagées</b> — seulement si vous créez ou connectez une liste partagée : nom de la liste, événements (nom ou titre, type, dates, note), horodatages et versions techniques. Le code ordinaire est conservé pour que les éditeurs puissent partager la liste ; le code maître uniquement sous forme d’empreinte irréversible. Finalité : synchroniser la liste entre ses membres.</p>
<p><b>Avis</b> — si vous envoyez un avis : son texte et, seulement si vous les avez indiqués ou autorisés, un contact pour la réponse et des données techniques (version de l’application, modèle du téléphone, version d’Android, langue). Les événements et les codes PIN n’y figurent jamais. Finalité : vous répondre et améliorer l’application. L’avis est transmis à l’auteur via Telegram.</p>
<p><b>Données techniques</b> — chaque requête au serveur implique votre adresse IP. Pour protéger contre la recherche de codes PIN, seule son empreinte irréversible est conservée.</p>
<p>Les données sont conservées sur le serveur du développeur ; les connexions sont chiffrées (HTTPS).</p>

<h2>5. Durée de conservation</h2>
<ul>
<li>liste partagée et ses événements — jusqu’à la suppression de la liste ;</li>
<li>avis — 2 ans au maximum ;</li>
<li>empreinte de l’adresse IP — un jour au maximum ;</li>
<li>sauvegardes de la base — 14 jours.</li>
</ul>

<h2>6. Bases légales</h2>
<p>Listes partagées et synchronisation — exécution de ce que vous demandez en créant ou connectant une liste. Avis — votre consentement, donné en l’envoyant. Protection contre la recherche de codes — notre intérêt légitime à la sécurité du service.</p>

<h2>7. Qui voit une liste partagée</h2>
<p>Toute personne qui connaît son nom et son code PIN. Vous décidez à qui donner le code et êtes responsable du consentement des personnes dont vous ajoutez les dates.</p>

<h2>8. Destinataires</h2>
<p>Personne, sauf obligation légale. Le site utilise le réseau Cloudflare, qui traite techniquement les adresses IP des visiteurs. Les avis sont transmis à l’auteur via Telegram.</p>

<h2 id="delete">9. Vos droits et la suppression des données</h2>
<p>Vous pouvez demander quelles données vous concernant sont conservées, les rectifier ou les supprimer.</p>
<ul>
<li><b>Une liste partagée entière :</b> « Listes » → la liste → « Supprimer la liste du cloud » → code maître. La liste est supprimée définitivement pour tous les membres.</li>
<li><b>Un seul événement :</b> ouvrez l’événement → « Supprimer ». Il disparaît chez tous après la synchronisation.</li>
<li><b>Déconnecter une liste du téléphone :</b> « Listes » → la liste → « Déconnecter ».</li>
<li><b>Tout sur le téléphone :</b> désinstallez l’application ou effacez ses données dans les réglages Android.</li>
<li><b>Un avis, un code maître perdu, autres demandes :</b> écrivez via « Envoyer un avis » dans l’application avec le nom de la liste et un contact. Réponse sous 30 jours.</li>
</ul>
<p>Si vous êtes dans l’UE, vous pouvez aussi introduire une réclamation auprès d’une autorité de protection des données (en France, la CNIL).</p>

<h2>10. Sauvegardes Android</h2>
<p>Les données de l’application peuvent figurer dans la sauvegarde du téléphone de votre compte Google — cela dépend des réglages du téléphone.</p>

<h2>11. Enfants</h2>
<p>L’application n’est pas destinée aux enfants de moins de 13 ans et ne collecte pas sciemment leurs données.</p>

<h2>12. Modifications</h2>
<p>Si la politique change, la nouvelle version apparaîtra sur cette page avec une nouvelle date. Les changements importants seront annoncés dans l’application.</p>`,
  },

  root: { choose: 'Choisissez une langue' },
};
