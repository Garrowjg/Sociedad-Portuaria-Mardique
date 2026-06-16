document.addEventListener('DOMContentLoaded', function() {
    var hdr = document.getElementById('main-header');
    if (!hdr) return;
    var ticking = false;
    window.addEventListener('scroll', function() {
        if (!ticking) {
            window.requestAnimationFrame(function() {
                hdr.classList.toggle('scrolled', window.scrollY > 50);
                ticking = false;
            });
            ticking = true;
        }
    }, { passive: true });
});
