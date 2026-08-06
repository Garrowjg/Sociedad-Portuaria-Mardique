document.addEventListener('DOMContentLoaded', function () {
    /* ── Header transparente → fondo azul + blur al hacer scroll ────────
       Funciona en TODOS los HTML que incluyan panel-nav.js.
       Al inicio: header transparente sobre el hero/video.
       Al bajar 80px: aparece fondo azul semitransparente + blur + animación.
       Al volver arriba: vuelve a transparente (vía CSS transition).
    ─────────────────────────────────────────────────────────────────── */
    var hdr = document.getElementById('main-header') || document.querySelector('header');
    if (!hdr) return;

    var THRESHOLD = 80; // px — funciona en todas las páginas
    var ticking   = false;
    var wasScrolled = false;

    function applyScrolled() {
        var isNowScrolled = window.scrollY > THRESHOLD;

        if (isNowScrolled && !wasScrolled) {
            // Entrando: agrega clase y deja correr la animación CSS
            hdr.classList.add('scrolled');
            wasScrolled = true;
        } else if (!isNowScrolled && wasScrolled) {
            // Volviendo arriba: quita clase, la transition CSS hace el fade
            hdr.classList.remove('scrolled');
            wasScrolled = false;
        }

        ticking = false;
    }

    window.addEventListener('scroll', function () {
        if (!ticking) {
            window.requestAnimationFrame(applyScrolled);
            ticking = true;
        }
    }, { passive: true });

    // Al cargar por si la página ya estaba scrolleada (ej: recarga en medio de la página)
    applyScrolled();
});