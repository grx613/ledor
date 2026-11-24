// Главная: стрелки галереи скриншотов (с учётом RTL) и закрытие меню языков по клику вне его.
(function () {
  var shots = document.getElementById('shots');
  var rtl = document.documentElement.dir === 'rtl' ? -1 : 1;
  document.querySelectorAll('[data-scroll]').forEach(function (b) {
    b.addEventListener('click', function () { shots.scrollBy({ left: Number(b.dataset.scroll) * 260 * rtl, behavior: 'smooth' }); });
  });
  document.addEventListener('click', function (e) {
    document.querySelectorAll('details.langs[open]').forEach(function (d) { if (!d.contains(e.target)) d.removeAttribute('open'); });
  });
})();
