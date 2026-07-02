function initMobileMenu() {
    var toggle = document.getElementById('mobileToggle');
    var drawer = document.getElementById('mobileDrawer');
    var overlay = document.getElementById('drawerOverlay');
    var closeBtn = document.getElementById('drawerClose');

    function openDrawer() {
        if (!drawer) return;
        drawer.classList.add('open');
        if (overlay) overlay.classList.add('open');
        if (toggle) toggle.classList.add('open');
        document.body.style.overflow = 'hidden';
    }
    function closeDrawer() {
        if (!drawer) return;
        drawer.classList.remove('open');
        if (overlay) overlay.classList.remove('open');
        if (toggle) toggle.classList.remove('open');
        document.body.style.overflow = '';
    }

    if (toggle) toggle.addEventListener('click', openDrawer);
    if (closeBtn) closeBtn.addEventListener('click', closeDrawer);
    if (overlay) overlay.addEventListener('click', closeDrawer);

    var subPairs = [['drawerServCliente','subServCliente'],['drawerAcceso','subAcceso']];
    subPairs.forEach(function(pair) {
        var btn = document.getElementById(pair[0]);
        var sub = document.getElementById(pair[1]);
        if (!btn || !sub) return;
        btn.addEventListener('click', function() {
            var isOpen = sub.classList.contains('open');
            document.querySelectorAll('.drawer-submenu').forEach(function(s) { s.classList.remove('open'); });
            document.querySelectorAll('.drawer-item').forEach(function(b) { b.classList.remove('open'); });
            if (!isOpen) {
                sub.classList.add('open');
                btn.classList.add('open');
            }
        });
    });
}

document.addEventListener('turbolinks:load', initMobileMenu);
document.addEventListener('DOMContentLoaded', initMobileMenu);
document.addEventListener('DOMContentLoaded', function () {
    const bar = document.getElementById('scroll-progress');
    window.addEventListener('scroll', () => {
        if (bar) {
            bar.style.width = (window.scrollY / (document.documentElement.scrollHeight - window.innerHeight) * 100) + '%';
        }
    }, { passive: true });

    const hdr = document.getElementById('main-header');
    window.addEventListener('scroll', () => {
        if (hdr) hdr.classList.toggle('scrolled', window.scrollY > 60);
    }, { passive: true });

    const heroLine = document.getElementById('heroLine');
    if (heroLine) setTimeout(() => heroLine.classList.add('animate'), 350);

    const typed = document.getElementById('typedText');
    const words = ['MARDIQUE', 'EL FUTURO', 'CONECTIVIDAD'];
    let wi = 0, ci = 0, deleting = false;
    function typeLoop() {
        if (!typed) return;
        const word = words[wi];
        if (!deleting) {
            typed.textContent = word.slice(0, ci + 1);
            ci++;
            if (ci === word.length) { deleting = true; setTimeout(typeLoop, 1800); return; }
            setTimeout(typeLoop, 90);
        } else {
            typed.textContent = word.slice(0, ci - 1);
            ci--;
            if (ci === 0) { deleting = false; wi = (wi + 1) % words.length; setTimeout(typeLoop, 400); return; }
            setTimeout(typeLoop, 50);
        }
    }
    if (typed) typeLoop();

    const pc = document.getElementById('heroParticles');
    if (pc) {
        for (let i = 0; i < 14; i++) {
            const el = document.createElement('div'); el.className = 'hero-particle';
            const s = Math.random() * 90 + 20;
            el.style.cssText = `width:${s}px;height:${s}px;left:${Math.random()*100}%;top:${Math.random()*100}%;animation-duration:${Math.random()*7+5}s;animation-delay:${Math.random()*4}s;opacity:${Math.random()*0.15+0.04};`;
            pc.appendChild(el);
        }
    }

    const io = new IntersectionObserver(entries => {
        entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('visible'); io.unobserve(e.target); } });
    }, { threshold: 0.1, rootMargin: '0px 0px -36px 0px' });
    document.querySelectorAll('.reveal, .reveal-left, .reveal-right').forEach(el => io.observe(el));

    const co = new IntersectionObserver(entries => {
        entries.forEach(e => {
            if (!e.isIntersecting) return;
            const tgt = parseInt(e.target.dataset.target), step = tgt / 50; let cur = 0;
            const t = setInterval(() => { cur += step; if (cur >= tgt) { e.target.textContent = tgt; clearInterval(t); } else e.target.textContent = Math.floor(cur); }, 28);
            co.unobserve(e.target);
        });
    }, { threshold: 0.5 });
    document.querySelectorAll('.counter').forEach(c => co.observe(c));
});

var IMAGE_DATA = {
    'Puerto1.png': { title: 'Carga y Descarga de Precisión', desc: 'Maniobras ágiles y seguras de mercancías a granel y sacos de gran formato directamente hacia el transporte terrestre.' },
    'Puerto2.png': { title: 'Capacidad a Gran Escala', desc: 'Infraestructura robusta diseñada para el atraque de buques de gran calado, respaldada por asistencia especializada de remolcadores.' },
    'Puerto3.png': { title: 'Conectividad Multimodal', desc: 'Plataforma operativa y muelles estratégicos optimizados para la transferencia eficiente de mercancías por vías fluviales y marítimas.' },
    'Puerto4.png': { title: 'Infraestructura de Vanguardia', desc: 'Equipamiento de última generación, zonas de almacenamiento y personal altamente calificado para el control y despacho seguro de la carga.' },
};

/* Catálogo de imágenes para la página de Servicios (mismo carrusel, otro set) */
var SERVICIOS_IMAGE_DATA = {
    'MuelleMaritimo.jpg': { title: 'Muelle Marítimo', desc: 'Operaciones de muellaje y atraque de embarcaciones respaldadas por infraestructura robusta y tecnología de precisión para garantizar la eficiencia en cada maniobra.' },
    'MuelleFluvial.jpg': { title: 'Muelle Fluvial', desc: 'Conexión estratégica que permite operaciones de muellaje y atraque simultáneo de convoy, remolcadores y embarcaciones marítimas y fluviales.' },
    'ServiciosPortuarios.jpg': { title: 'Servicios Portuarios', desc: 'Personal altamente calificado, equipos de última tecnología e infraestructura para el manejo integral de carga, almacenamiento y control de inventarios.' },
    'PlataformaLogistica.jpg': { title: 'Plataforma Logística', desc: 'Hub regional con bodegas, patios de maniobras y conexiones estratégicas para la distribución eficiente de mercancías a nivel nacional e internacional.' },
};

/* ============================================================
   CARRUSEL MODAL — Mardique (reutilizable en cualquier página)
   ============================================================ */
var carouselIndex = 0;
var CURRENT_IMAGE_DATA = IMAGE_DATA;
var CAROUSEL_IMAGES = Object.keys(IMAGE_DATA);

/**
 * Abre el carrusel modal.
 * @param {string} src - nombre del archivo de imagen (ej: 'Puerto1.png').
 * @param {object} [dataset] - opcional, diccionario { archivo: {title, desc} } a usar.
 *                              Si no se pasa, se usa IMAGE_DATA (set de Inicio) por defecto.
 */
function openLightbox(src, dataset) {
    CURRENT_IMAGE_DATA = dataset || IMAGE_DATA;
    CAROUSEL_IMAGES = Object.keys(CURRENT_IMAGE_DATA);
    var idx = CAROUSEL_IMAGES.indexOf(src);
    if (idx === -1) idx = 0;
    carouselIndex = idx;
    _buildCarousel();
    var overlay = document.getElementById('carouselOverlay');
    if (overlay) {
        overlay.classList.add('open');
        document.body.style.overflow = 'hidden';
    }
    _updateCarousel(false);
}

function closeCarousel() {
    var overlay = document.getElementById('carouselOverlay');
    if (overlay) {
        overlay.classList.remove('open');
        document.body.style.overflow = '';
    }
}

/* Mantiene compatibilidad si algo llama closeLightbox() directamente */
function closeLightbox() { closeCarousel(); }

function handleCarouselOverlayClick(e) {
    if (e.target === document.getElementById('carouselOverlay')) closeCarousel();
}

function carouselNav(dir) {
    carouselIndex = (carouselIndex + dir + CAROUSEL_IMAGES.length) % CAROUSEL_IMAGES.length;
    _updateCarousel(true);
}

function _buildCarousel() {
    var thumbsEl = document.getElementById('carouselThumbs');
    var dotsEl   = document.getElementById('carouselDots');
    if (!thumbsEl || !dotsEl) return;
    thumbsEl.innerHTML = '';
    dotsEl.innerHTML   = '';
    CAROUSEL_IMAGES.forEach(function(key, i) {
        /* miniaturas */
        var th = document.createElement('div');
        th.className = 'carousel-thumb' + (i === carouselIndex ? ' active' : '');
        th.setAttribute('data-idx', i);
        th.onclick = function() { carouselIndex = i; _updateCarousel(true); };
        var img = document.createElement('img');
        img.src = '/images/' + key;
        img.alt = (CURRENT_IMAGE_DATA[key] && CURRENT_IMAGE_DATA[key].title) || '';
        th.appendChild(img);
        thumbsEl.appendChild(th);

        /* dots */
        var dot = document.createElement('button');
        dot.className = 'carousel-dot' + (i === carouselIndex ? ' active' : '');
        dot.setAttribute('aria-label', 'Imagen ' + (i + 1));
        dot.setAttribute('data-idx', i);
        dot.onclick = function() { carouselIndex = i; _updateCarousel(true); };
        dotsEl.appendChild(dot);
    });
}

function _updateCarousel(animate) {
    var src  = CAROUSEL_IMAGES[carouselIndex];
    var data = CURRENT_IMAGE_DATA[src] || { title: 'Puerto Mardique', desc: 'Infraestructura portuaria de clase mundial.' };

    var imgEl   = document.getElementById('carouselImg');
    var titleEl = document.getElementById('carouselTitle');
    var descEl  = document.getElementById('carouselDesc');
    var labelEl = document.getElementById('carouselLabel');

    if (imgEl) {
        if (animate) {
            imgEl.classList.add('fading');
            setTimeout(function() {
                imgEl.src = '/images/' + src;
                imgEl.classList.remove('fading');
            }, 250);
        } else {
            imgEl.src = '/images/' + src;
        }
    }
    if (titleEl) titleEl.textContent = data.title;
    if (descEl)  descEl.textContent  = data.desc;
    if (labelEl) labelEl.textContent = 'MARDIQUE';

    /* actualizar thumbs activos */
    document.querySelectorAll('.carousel-thumb').forEach(function(el) {
        el.classList.toggle('active', parseInt(el.getAttribute('data-idx')) === carouselIndex);
    });
    /* actualizar dots activos */
    document.querySelectorAll('.carousel-dot').forEach(function(el) {
        el.classList.toggle('active', parseInt(el.getAttribute('data-idx')) === carouselIndex);
    });
}

/* Teclado: flechas para navegar, Escape para cerrar */
document.addEventListener('keydown', function(e) {
    var overlay = document.getElementById('carouselOverlay');
    if (!overlay || !overlay.classList.contains('open')) return;
    if (e.key === 'Escape')    closeCarousel();
    if (e.key === 'ArrowRight') carouselNav(1);
    if (e.key === 'ArrowLeft')  carouselNav(-1);
});

/* ============================================================
   GLOBE 3D PREMIUM v2 — DISEÑO CORPORATIVO
   ============================================================ */
(function() {
    const canvas = document.getElementById('globeCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    const GOLD = '#f0a030', CYAN = '#00c8f0', TEAL = '#00e8a0', ROSE = '#ff6b6b';
    let zoomScale = 1.0, targetZoom = 1.0;
    const ZOOM_MIN = 0.50, ZOOM_MAX = 3.0, ZOOM_STEP = 0.12;

    let rot = 1.32, tilt = 0.2;
    let rotSpeed = 0.00035, targetRotSpeed = 0.00035;
    let isDragging = false, isHovering = false, didDrag = false;
    let lastX = 0, lastY = 0;
    let R, cx, cy;
    let selectedNode = null;
    let mouseX = -9999, mouseY = -9999;
    let time = 0;
    const TILT_MIN = -1.1, TILT_MAX = 1.1;

    const NODES = [
        { lon: -75.5, lat: 10.4, hub: true,  labelDir: 'above', label: 'Sociedad Portuaria Mardique', desc: 'Puerto multipropósito privado con conexión directa al interior del país. Operaciones 24/7.', image: '/images/globo/Mardique.png' },
        { lon: -118.3, lat: 33.7, hub: false, labelDir: 'above', label: 'Los Ángeles', desc: 'El puerto más activo de EE.UU. Principal puerta de entrada para bienes desde Asia.', image: '/images/globo/Los angeles.png' },
        { lon: -74.0, lat: 40.7, hub: false, labelDir: 'above', label: 'Nueva York', desc: 'Segundo puerto más importante de la costa este de EE.UU. Puerta a los mercados del noreste.', image: '/images/globo/Nueva York.png' },
        { lon: -95.3, lat: 29.8, hub: false, labelDir: 'below', label: 'Houston', desc: 'Líder en carga de hidrocarburos y productos petroquímicos en América del Norte.', image: '/images/globo/Houston.png' },
        { lon: -79.9, lat: 9.0,  hub: false, labelDir: 'below', label: 'Canal de Panamá', desc: 'Corredor interoceánico estratégico. Más de 14.000 buques transitan cada año.', image: '/images/globo/Panama.png' },
        { lon: -46.3, lat: -23.9, hub: false, labelDir: 'right', label: 'Santos', desc: 'El puerto más grande de América Latina. Maneja más del 30% del comercio exterior brasileño.', image: '/images/globo/Santos.png' },
        { lon: -77.1, lat: -12.1, hub: false, labelDir: 'left',  label: 'Callao', desc: 'Principal puerto del Pacífico sudamericano. Puerta de la minería y agroexportación peruana.', image: '/images/globo/Callao.png' },
        { lon: -58.4, lat: -34.6, hub: false, labelDir: 'below', label: 'Buenos Aires', desc: 'Principal puerto de Argentina en el Río de la Plata. Clave para granos y contenedores.', image: '/images/globo/Buenos aires.png' },
        { lon: 4.5,  lat: 51.9, hub: false, labelDir: 'above', label: 'Róterdam', desc: 'El puerto más grande de Europa. Conecta a Cartagena con todos los mercados europeos.', image: '/images/globo/Rotterdam.png' },
        { lon: 2.2,  lat: 41.4, hub: false, labelDir: 'above', label: 'Barcelona', desc: 'Principal puerta de entrada de mercancías de América Latina a la Europa mediterránea.', image: '/images/globo/Barcelona.png' },
        { lon: 55.3, lat: 25.1, hub: false, labelDir: 'above', label: 'Jebel Ali', desc: 'El mayor puerto del Medio Oriente. Uno de los 10 más activos del mundo.', image: '/images/globo/Jebel Ali.png' },
        { lon: 32.3, lat: 29.9, hub: false, labelDir: 'right', label: 'Port Said', desc: 'Entrada al Canal de Suez. Uno de los puntos de tránsito marítimo más importantes del mundo.', image: '/images/globo/Port Said.png' },
        { lon: 18.3, lat: -33.9, hub: false, labelDir: 'below', label: 'Ciudad del Cabo', desc: 'Punto estratégico entre América del Sur y el océano Índico.', image: '/images/globo/Ciudad del cabo.png' },
        { lon: 121.5, lat: 31.2, hub: false, labelDir: 'above', label: 'Shanghái', desc: 'El puerto más grande del mundo por volumen de contenedores. 49M TEU / año.', image: '/images/globo/Shanghái.png' },
        { lon: 103.9, lat: 1.3,  hub: false, labelDir: 'below', label: 'Singapur', desc: 'Segundo puerto más grande del mundo. Mayor hub de transbordo de Asia.', image: '/images/globo/Singapur.png' },
        { lon: 129.1, lat: 35.1, hub: false, labelDir: 'above', label: 'Busan', desc: 'Quinto puerto más transitado del mundo. 21.7M TEU / año.', image: '/images/globo/Busan.png' },
        { lon: 151.2, lat: -33.9, hub: false, labelDir: 'right', label: 'Sídney', desc: 'Principal puerto de Australia para contenedores.', image: '/images/globo/Sidney.png' }
    ];

    const ROUTES = [
        { from: 0, to: 1 }, { from: 0, to: 4 }, { from: 0, to: 8 },
        { from: 0, to: 5 }, { from: 0, to: 13 }
    ];

    const particles = [];
    ROUTES.forEach((r, i) => {
        for (let j = 0; j < 1; j++) {
            particles.push({ routeIdx: i, t: j * 0.4 + Math.random() * 0.1, speed: 0.0006 + Math.random() * 0.0004, dir: 1, type: 'ship' });
            particles.push({ routeIdx: i, t: j * 0.4 + Math.random() * 0.1, speed: 0.0006 + Math.random() * 0.0004, dir: -1, type: 'truck' });
        }
    });

    const stars = Array.from({ length: 150 }, (_, idx) => {
        const isFeature = idx < 5; /* un puñado de estrellas "destacadas" con destello marcado */
        return {
            x: Math.random(), y: Math.random(),
            size: isFeature ? Math.random() * 1.2 + 2.2 : Math.random() * 1.8 + 0.1,
            opacity: isFeature ? Math.random() * 0.3 + 0.6 : Math.random() * 0.7 + 0.1,
            twinkleSpeed: isFeature ? Math.random() * 0.01 + 0.002 : Math.random() * 0.02 + 0.003,
            twinkleOffset: Math.random() * Math.PI * 2,
            color: isFeature ? (Math.random() > 0.5 ? '#bcd8ff' : '#ffe0b0') : (Math.random() > 0.82 ? (Math.random() > 0.5 ? '#8ab4ff' : '#ffb07a') : '#ffffff'),
            isFeature
        };
    });

    const shootingStars = Array.from({ length: 3 }, () => ({
        active: false, x: 0, y: 0, dx: 0, dy: 0, life: 0, maxLife: 0, timer: Math.random() * 300, speed: 0
    }));

    const clouds = Array.from({ length: 8 }, () => ({
        lonOffset: Math.random() * 360 - 180,
        latOffset: (Math.random() - 0.5) * 100,
        size: Math.random() * 40 + 12,
        driftSpeed: 0.00008 + Math.random() * 0.00015,
        opacity: Math.random() * 0.10 + 0.03,
        phase: Math.random() * Math.PI * 2
    }));
    /* Segunda capa de nubes, más pequeñas y lentas: da parallax y sensación de dos altitudes */
    const cloudsFar = Array.from({ length: 10 }, () => ({
        lonOffset: Math.random() * 360 - 180,
        latOffset: (Math.random() - 0.5) * 110,
        size: Math.random() * 22 + 8,
        driftSpeed: 0.00003 + Math.random() * 0.00006,
        opacity: Math.random() * 0.06 + 0.02,
        phase: Math.random() * Math.PI * 2
    }));

    const dataPulses = [];
    let pulseTimer = 0;
    const ORBIT_TILT = 0.35;
    const ORBIT_POINTS = 80;
    let ripples = [];

    const CONTINENTS = [
        { name: 'namerica', points: [
                [-168,72],[-161,72],[-153,70],[-141,70],[-135,68],[-130,67],[-126,65],[-120,64],
                [-110,68],[-100,70],[-90,70],[-82,68],[-78,65],[-72,63],[-66,61],[-60,55],
                [-64,52],[-66,48],[-68,46],[-70,44],[-74,41],[-76,37],[-78,34],[-80,31],
                [-81,28],[-82,24],[-84,20],[-88,19],[-92,17],[-100,18],
                [-104,19],[-108,22],[-112,28],[-116,30],[-118,32],[-120,36],[-122,38],
                [-124,44],[-126,50],[-130,56],[-136,58],[-142,60],[-148,62],[-155,62],
                [-160,60],[-163,63],[-165,65],[-168,68],[-168,72]
            ]},
        { name: 'camerica', points: [
                [-92,17], [-88,15], [-84,15], [-82,10], [-80,9], [-77,8],
                [-77,7], [-80,8], [-83,9], [-85,12], [-88,13], [-92,14]
            ]},
        { name: 'greenland', points: [
                [-44,84],[-30,83],[-18,82],[-14,80],[-20,76],[-28,72],[-42,70],[-52,68],
                [-58,68],[-64,70],[-68,72],[-62,76],[-52,80],[-44,84]
            ]},
        { name: 'samerica', points: [
                [-73,12],[-71,11],[-68,10],[-63,10],[-59,9],[-53,6],[-50,4],[-45,4],
                [-38,5],[-34,6],[-34,-4],[-35,-10],[-37,-14],[-38,-18],[-40,-22],
                [-43,-24],[-46,-28],[-52,-32],[-54,-36],[-60,-40],[-63,-44],
                [-65,-48],[-66,-52],[-68,-56],[-70,-54],[-72,-48],[-73,-42],
                [-73,-36],[-72,-30],[-72,-24],[-73,-18],[-76,-14],[-80,-8],
                [-80,-2],[-78,2],[-78,6],[-76,9],[-73,12]
            ]},
        { name: 'europe', points: [
                [-10,36],[-6,36],[0,37],[5,37],[8,38],[12,38],[15,38],[18,40],
                [22,40],[25,40],[28,42],[30,44],[28,48],[26,52],[24,56],[22,60],
                [20,64],[16,68],[14,70],[10,70],[6,68],[2,65],[0,62],
                [-2,58],[-4,56],[-6,54],[-4,52],[-2,50],[0,48],[2,46],
                [6,44],[10,43],[14,44],[16,42],[18,40],[14,40],[8,38],
                [4,37],[0,37],[-4,37],[-9,39],[-10,36]
            ]},
        { name: 'iceland', points: [[-24,64],[-20,64],[-14,64],[-14,66],[-18,67],[-22,66],[-24,65],[-24,64]] },
        { name: 'britain', points: [[-5,50],[-3,50],[0,51],[2,52],[1,54],[-2,56],[-4,58],[-6,58],[-6,56],[-4,54],[-3,52],[-5,50]] },
        { name: 'africa', points: [
                [-17,35], [-13,29], [-17,21], [-15,12], [-10,5], [4,4], [9,2], [12,-6],
                [13,-12], [18,-34], [22,-34], [26,-33], [30,-28], [33,-24], [35,-20],
                [39,-12], [41,-5], [48,5], [51,11], [43,13], [42,22], [36,22],
                [33,28], [32,31], [25,32], [15,32], [11,37], [0,36], [-6,36]
            ]},
        { name: 'madagascar', points: [[44,-12],[46,-14],[48,-18],[50,-22],[48,-24],[46,-26],[44,-24],[44,-20],[44,-16],[44,-12]] },
        { name: 'asia', points: [
                [26,42],[30,42],[34,44],[38,40],[44,40],[50,40],[56,38],
                [60,36],[64,34],[68,34],[72,30],[76,28],[80,26],[84,24],
                [88,22],[92,20],[96,18],[100,18],[104,18],[106,16],
                [100,14],[98,10],[100,6],[104,4],[108,4],[112,6],
                [116,10],[120,14],[116,18],[112,20],[110,18],[108,20],
                [112,22],[116,22],[120,24],[124,28],[128,32],[132,36],
                [136,38],[140,42],[142,46],[145,50],[142,54],[138,56],
                [132,60],[124,64],[116,68],[108,70],[100,70],[90,68],
                [80,68],[72,66],[64,64],[56,62],[48,60],[40,58],
                [34,54],[30,50],[26,46],[26,42]
            ]},
        { name: 'india', points: [
                [68,24],[72,22],[76,20],[80,16],[84,14],[80,10],[78,8],
                [76,8],[74,10],[72,12],[70,16],[68,20],[66,22],[68,24]
            ]},
        { name: 'arabia', points: [
                [36,30],[40,28],[44,26],[48,24],[52,22],[56,20],[58,16],
                [56,14],[52,12],[48,12],[44,14],[40,16],[38,20],[36,24],[36,30]
            ]},
        { name: 'australia', points: [
                [114,-22],[116,-20],[120,-18],[124,-16],[128,-14],[132,-12],
                [136,-12],[140,-14],[142,-16],[146,-18],[148,-22],[150,-24],
                [152,-28],[152,-32],[150,-36],[148,-38],[144,-38],[140,-38],
                [136,-36],[132,-34],[128,-32],[122,-30],[118,-26],[114,-24],[114,-22]
            ]},
        { name: 'newzealand', points: [[172,-34],[174,-36],[176,-38],[174,-40],[172,-40],[170,-38],[172,-34]] },
        { name: 'japan', points: [[130,31],[132,33],[134,34],[136,36],[138,38],[140,40],[142,42],[142,44],[140,44],[138,42],[136,38],[134,36],[132,34],[130,32],[130,31]] },
        { name: 'iberia', points: [[-9.5,36],[-5.5,36],[-1,37],[0,37],[2,38],[3.2,40.5],[3.2,42.5],[1,43],[-2,43.5],[-5,43.8],[-8.5,44],[-9.3,43],[-9.5,37],[-9.5,36]] },
        { name: 'cuba', points: [[-85,21.8],[-78,21.5],[-76,22],[-78,23],[-82,23.5],[-84.5,23],[-85,21.8]] },
        { name: 'hispaniola', points: [[-72,18],[-68.5,18],[-70,19.5],[-72,19],[-72,18]] },
        { name: 'jamaica', points: [[-78.5,17.7],[-76.2,17.7],[-76.2,18.5],[-78.5,18.5],[-78.5,17.7]] },
        { name: 'borneo', points: [[109,1],[115,1],[117,3],[117,5],[115,7],[109,7],[108,5],[109,1]] },
        { name: 'sumatra', points: [[96,-5],[101,-5],[104,-3],[105,0],[104,3],[101,5],[98,5],[96,-5]] },
        { name: 'java', points: [[106,-7],[112,-7],[114,-8],[114,-6],[112,-5],[106,-5],[106,-7]] },
        { name: 'newguinea', points: [[134,-6],[141,-6],[144,-5],[146,-7],[148,-9],[146,-10],[141,-8],[134,-8],[134,-6]] },
        { name: 'philippines', points: [[121,12],[126,12],[127,10],[125,7],[122,6],[120,7],[119,9],[121,12]] },
        { name: 'sicily', points: [[12,37],[15,37],[15,38],[13,38],[12,37]] },
        { name: 'sardinia', points: [[8,39],[9,39],[10,40],[9,41],[8,40],[8,39]] },
        { name: 'srilanka', points: [[80,7],[82,7],[82,9],[80,9],[80,7]] },
        { name: 'taiwan', points: [[120,22],[122,22],[122,25],[120,25],[120,22]] }
    ];

    const REGION_LABELS = [
        { lon: -100, lat: 45,  label: 'Norteamérica' },
        { lon: -88,  lat: 13,  label: 'Centroamérica' },
        { lon: -58,  lat: -18, label: 'Sudamérica' },
        { lon: 15,   lat: 52,  label: 'Europa' },
        { lon: 18,   lat: 5,   label: 'África' },
        { lon: 92,   lat: 36,  label: 'Asia' },
        { lon: 134,  lat: -27, label: 'Oceanía' },
        { lon: 45,   lat: 26,  label: 'Oriente Medio' },
    ];

    function toRad(d) { return d * Math.PI / 180; }

    function project(lon, lat) {
        const lam = toRad(lon) + rot;
        const phi = toRad(lat);
        const x0 = Math.cos(phi) * Math.sin(lam);
        const y0 = Math.sin(phi);
        const z0 = Math.cos(phi) * Math.cos(lam);
        const x1 = x0;
        const y1 = y0 * Math.cos(tilt) - z0 * Math.sin(tilt);
        const z1 = y0 * Math.sin(tilt) + z0 * Math.cos(tilt);
        const effectiveR = R * zoomScale;
        return { x: cx + effectiveR * x1, y: cy - effectiveR * y1, z: z1 };
    }

    function projectRaw(lon, lat) {
        const lam = toRad(lon) + rot;
        const phi = toRad(lat);
        const x0 = Math.cos(phi) * Math.sin(lam);
        const y0 = Math.sin(phi);
        const z0 = Math.cos(phi) * Math.cos(lam);
        const x1 = x0;
        const y1 = y0 * Math.cos(tilt) - z0 * Math.sin(tilt);
        const z1 = y0 * Math.sin(tilt) + z0 * Math.cos(tilt);
        return { x: x1, y: y1, z: z1 };
    }

    function slerp(lon0, lat0, lon1, lat1, t) {
        return { lon: lon0 + (lon1 - lon0) * t, lat: lat0 + (lat1 - lat0) * t };
    }

    function continentFacing(points) {
        let sumLon = 0, sumLat = 0;
        points.forEach(p => { sumLon += p[0]; sumLat += p[1]; });
        return projectRaw(sumLon / points.length, sumLat / points.length).z;
    }

    /* ================================================================
       PREMIUM DRAWING — DISEÑO CORPORATivo
       ================================================================ */
    function drawBackground() {
        ctx.save();
        const g1 = ctx.createRadialGradient(canvas.width*0.14, canvas.height*0.12, 0, canvas.width*0.14, canvas.height*0.12, canvas.width*0.5);
        g1.addColorStop(0, 'rgba(0,60,140,0.18)');
        g1.addColorStop(0.5, 'rgba(0,30,80,0.08)');
        g1.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = g1; ctx.fillRect(0,0,canvas.width,canvas.height);
        const g2 = ctx.createRadialGradient(canvas.width*0.9, canvas.height*0.8, 0, canvas.width*0.9, canvas.height*0.8, canvas.width*0.4);
        g2.addColorStop(0, 'rgba(100,0,80,0.10)');
        g2.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = g2; ctx.fillRect(0,0,canvas.width,canvas.height);
        const g3 = ctx.createRadialGradient(canvas.width*0.5, canvas.height*0.3, 0, canvas.width*0.5, canvas.height*0.3, canvas.width*0.45);
        g3.addColorStop(0, 'rgba(0,20,60,0.07)');
        g3.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = g3; ctx.fillRect(0,0,canvas.width,canvas.height);
        ctx.restore();
    }

    function drawStars() {
        const now = Date.now() * 0.001;
        ctx.save();
        stars.forEach(s => {
            const twinkle = Math.sin(now * s.twinkleSpeed * 12 + s.twinkleOffset) * 0.35 + 0.65;
            ctx.globalAlpha = s.opacity * twinkle;
            const sx = s.x * canvas.width, sy = s.y * canvas.height;
            ctx.shadowColor = s.color;
            ctx.shadowBlur = s.size > 1.5 ? 3 : 0;
            ctx.fillStyle = s.color;
            ctx.beginPath(); ctx.arc(sx, sy, s.size, 0, Math.PI * 2); ctx.fill();
            ctx.shadowBlur = 0;
            if (s.size > 1.6) {
                ctx.globalAlpha = s.opacity * twinkle * 0.15;
                ctx.beginPath(); ctx.arc(sx, sy, s.size * 4, 0, Math.PI * 2); ctx.fill();
            }
            if (s.isFeature) {
                ctx.globalAlpha = s.opacity * twinkle * 0.5;
                ctx.strokeStyle = s.color;
                ctx.lineWidth = 0.6;
                const flareLen = s.size * 5;
                ctx.beginPath(); ctx.moveTo(sx - flareLen, sy); ctx.lineTo(sx + flareLen, sy); ctx.stroke();
                ctx.beginPath(); ctx.moveTo(sx, sy - flareLen); ctx.lineTo(sx, sy + flareLen); ctx.stroke();
            }
        });
        ctx.restore();
    }

    function drawShootingStars() {
        shootingStars.forEach(s => {
            if (!s.active) {
                s.timer -= 0.016;
                if (s.timer <= 0) {
                    s.active = true;
                    s.x = Math.random() * canvas.width * 0.8 + canvas.width * 0.1;
                    s.y = Math.random() * canvas.height * 0.35;
                    const angle = Math.PI * 0.15 + Math.random() * Math.PI * 0.2;
                    s.dx = Math.cos(angle) * (8 + Math.random() * 6);
                    s.dy = Math.sin(angle) * (8 + Math.random() * 6);
                    s.life = 0; s.maxLife = 40 + Math.random() * 30;
                } return;
            }
            s.life++;
            if (s.life > s.maxLife) { s.active = false; s.timer = 100 + Math.random() * 300; return; }
            const p = s.life / s.maxLife, alpha = p < 0.1 ? p * 10 : (1 - p);
            ctx.save(); ctx.globalAlpha = alpha * 0.55;
            const len = 40 + p * 30;
            const g = ctx.createLinearGradient(s.x, s.y, s.x - s.dx * 0.08 * len, s.y - s.dy * 0.08 * len);
            g.addColorStop(0, 'rgba(255,255,255,0.9)'); g.addColorStop(0.3, 'rgba(200,230,255,0.4)'); g.addColorStop(1, 'rgba(200,230,255,0)');
            ctx.strokeStyle = g; ctx.lineWidth = 1.8 + p * 0.6;
            ctx.beginPath(); ctx.moveTo(s.x, s.y); ctx.lineTo(s.x - s.dx * 0.08 * len, s.y - s.dy * 0.08 * len); ctx.stroke();
            ctx.shadowColor = '#88ccff'; ctx.shadowBlur = 15;
            ctx.fillStyle = '#fff'; ctx.beginPath(); ctx.arc(s.x, s.y, 2.2, 0, Math.PI * 2); ctx.fill();
            ctx.shadowBlur = 0; s.x += s.dx; s.y += s.dy;
            ctx.restore();
        });
    }

    function drawContinents() {
        CONTINENTS.forEach(cont => {
            const facing = continentFacing(cont.points);
            if (facing < -0.06) return;
            const alpha = Math.max(0, Math.min(1, facing * 2.8 + 0.2));
            const latMid = cont.points.reduce((s,p) => s + p[1], 0) / cont.points.length;
            ctx.beginPath();
            cont.points.forEach((pt, i) => {
                const p = project(pt[0], pt[1]);
                if (i === 0) ctx.moveTo(p.x, p.y); else ctx.lineTo(p.x, p.y);
            });
            ctx.closePath();
            const warm = Math.max(20, Math.min(80, 70 - latMid * 0.4));
            const gr = Math.max(140, Math.min(220, 200 - Math.abs(latMid) * 0.6));
            const bl = Math.max(60, Math.min(120, 110 - Math.abs(latMid) * 0.5));
            const baseA = 0.12 * alpha + 0.04;
            ctx.fillStyle = `rgba(${warm}, ${gr}, ${bl}, ${baseA})`;
            ctx.fill();
            const sunBoost = Math.max(0, Math.min(0.3, facing * 0.4));
            ctx.fillStyle = `rgba(160, 240, 130, ${sunBoost * 0.06})`;
            ctx.fill();
            /* Relieve sutil: un par de manchas de sombra/luz internas para romper el color plano */
            ctx.save();
            ctx.clip();
            const relief = ctx.createRadialGradient(
                cont.points[0] ? project(cont.points[0][0], cont.points[0][1]).x : cx,
                cont.points[0] ? project(cont.points[0][0], cont.points[0][1]).y : cy,
                0,
                cx, cy, R * zoomScale * 0.9
            );
            relief.addColorStop(0, `rgba(255,255,255,${0.05 * alpha})`);
            relief.addColorStop(0.5, `rgba(0,20,10,${0.05 * alpha})`);
            relief.addColorStop(1, `rgba(0,10,5,${0.09 * alpha})`);
            ctx.fillStyle = relief; ctx.fill();
            ctx.restore();
            ctx.strokeStyle = `rgba(0, 255, 180, ${0.45 * alpha})`;
            ctx.lineWidth = 0.8; ctx.stroke();
            ctx.strokeStyle = `rgba(0, 200, 255, ${0.10 * alpha})`;
            ctx.lineWidth = 1.8; ctx.stroke();
            ctx.save();
            ctx.shadowColor = 'rgba(0,220,190,0.5)';
            ctx.shadowBlur = 6 * alpha;
            ctx.strokeStyle = `rgba(0, 230, 200, ${0.18 * alpha})`;
            ctx.lineWidth = 0.6; ctx.stroke();
            ctx.restore();
        });
    }

    function drawGlobe() {
        const effectiveR = R * zoomScale;
        drawBackground();
        drawStars();
        ctx.save();
        const sh = ctx.createRadialGradient(cx+effectiveR*0.18, cy+effectiveR*0.18, effectiveR*0.8, cx+effectiveR*0.18, cy+effectiveR*0.18, effectiveR*1.6);
        sh.addColorStop(0, 'rgba(0,8,24,0.40)');
        sh.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.beginPath(); ctx.arc(cx+effectiveR*0.1, cy+effectiveR*0.1, effectiveR*1.08, 0, Math.PI*2);
        ctx.fillStyle = sh; ctx.fill();
        ctx.restore();
        const grd = ctx.createRadialGradient(cx - effectiveR*0.3, cy - effectiveR*0.35, effectiveR*0.02, cx, cy, effectiveR);
        grd.addColorStop(0, '#2a8fd4');
        grd.addColorStop(0.18, '#1479b8');
        grd.addColorStop(0.4, '#0a3d70');
        grd.addColorStop(0.65, '#051f42');
        grd.addColorStop(0.85, '#020e24');
        grd.addColorStop(1, '#010610');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = grd; ctx.fill();
        /* Terminador día/noche: oscurece el lado opuesto a la luz para dar volumen real de esfera */
        ctx.save();
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.clip();
        const term = ctx.createRadialGradient(cx + effectiveR*0.55, cy + effectiveR*0.6, 0, cx + effectiveR*0.55, cy + effectiveR*0.6, effectiveR*1.35);
        term.addColorStop(0, 'rgba(0,0,0,0)');
        term.addColorStop(0.55, 'rgba(0,3,12,0.10)');
        term.addColorStop(0.8, 'rgba(0,2,10,0.38)');
        term.addColorStop(1, 'rgba(0,1,6,0.62)');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = term; ctx.fill();
        ctx.restore();
        const shimmerAngle = Date.now() * 0.00008;
        const sx = cx + Math.cos(shimmerAngle) * effectiveR * 0.25;
        const sy = cy + Math.sin(shimmerAngle * 0.7) * effectiveR * 0.2;
        const sg = ctx.createRadialGradient(sx, sy, 0, sx, sy, effectiveR * 0.5);
        sg.addColorStop(0, 'rgba(100,200,255,0.06)');
        sg.addColorStop(0.5, 'rgba(80,180,240,0.03)');
        sg.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = sg; ctx.fill();
        ctx.save(); ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.clip();
        for (let lat = -80; lat <= 80; lat += 12) {
            ctx.beginPath(); let first = true;
            for (let i = 0; i <= 90; i++) {
                const p = project(-180 + 360*i/90, lat);
                if (p.z < 0) { first = true; continue; }
                if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y);
            }
            ctx.strokeStyle = lat === 0 ? 'rgba(0,200,255,0.25)' : 'rgba(255,255,255,0.025)';
            ctx.lineWidth = lat === 0 ? 0.8 : 0.3; ctx.stroke();
        }
        drawContinents();
        ctx.restore();
        drawAtmosphere();
        const sunGrd = ctx.createRadialGradient(cx - effectiveR*0.40, cy - effectiveR*0.42, 0, cx - effectiveR*0.06, cy - effectiveR*0.06, effectiveR*0.65);
        sunGrd.addColorStop(0, 'rgba(255,255,255,0.10)');
        sunGrd.addColorStop(0.3, 'rgba(255,255,255,0.03)');
        sunGrd.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = sunGrd; ctx.fill();
        /* Highlight especular puntual y contenido, tipo esfera pulida (sutil) */
        ctx.save();
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.clip();
        const spec = ctx.createRadialGradient(cx - effectiveR*0.34, cy - effectiveR*0.38, 0, cx - effectiveR*0.34, cy - effectiveR*0.38, effectiveR*0.16);
        spec.addColorStop(0, 'rgba(255,255,255,0.14)');
        spec.addColorStop(0.5, 'rgba(230,245,255,0.04)');
        spec.addColorStop(1, 'rgba(230,245,255,0)');
        ctx.beginPath(); ctx.arc(cx - effectiveR*0.34, cy - effectiveR*0.38, effectiveR*0.16, 0, Math.PI*2); ctx.fillStyle = spec; ctx.fill();
        ctx.restore();
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2);
        ctx.strokeStyle = 'rgba(0,180,255,0.25)'; ctx.lineWidth = 1.2; ctx.stroke();
        if (zoomScale < 1.4) {
            const fa = Math.max(0, Math.min(0.18, (1.4 - zoomScale) * 0.22));
            ctx.save(); ctx.globalAlpha = fa;
            const fg = ctx.createRadialGradient(cx, cy, effectiveR*0.05, cx, cy, effectiveR*1.15);
            fg.addColorStop(0, 'rgba(200,230,255,0)');
            fg.addColorStop(0.7, 'rgba(200,230,255,0.01)');
            fg.addColorStop(1, 'rgba(210,235,255,0.05)');
            ctx.fillStyle = fg; ctx.beginPath(); ctx.arc(cx, cy, effectiveR*1.15, 0, Math.PI*2); ctx.fill();
            ctx.restore();
        }
    }

    function drawAtmosphere() {
        const effectiveR = R * zoomScale;
        const now = Date.now() * 0.001;
        const pulse = Math.sin(now * 0.25) * 0.10 + 0.90;
        const hueShift = (Math.sin(now * 0.08) * 15 + 205) | 0;
        ctx.save();
        const g = ctx.createRadialGradient(cx, cy, effectiveR*0.88, cx, cy, effectiveR*1.22);
        g.addColorStop(0, 'rgba(0,100,200,0)');
        g.addColorStop(0.2, `rgba(0,${180 + Math.sin(now*0.15)*30},255,${0.10 * pulse})`);
        g.addColorStop(0.5, `hsla(${hueShift},100%,60%,${0.05 * pulse})`);
        g.addColorStop(0.8, `hsla(${hueShift + 30},80%,50%,${0.02 * pulse})`);
        g.addColorStop(1, 'rgba(0,40,120,0)');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR*1.22, 0, Math.PI*2); ctx.fillStyle = g; ctx.fill();
        /* Anillo delgado y discreto justo en el borde, para "sellar" el limbo del planeta */
        const rim = ctx.createRadialGradient(cx, cy, effectiveR*0.98, cx, cy, effectiveR*1.03);
        rim.addColorStop(0, 'rgba(120,210,255,0)');
        rim.addColorStop(0.6, `rgba(140,220,255,${0.14 * pulse})`);
        rim.addColorStop(1, 'rgba(140,220,255,0)');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR*1.03, 0, Math.PI*2); ctx.fillStyle = rim; ctx.fill();
        const h = ctx.createRadialGradient(cx - effectiveR*0.35, cy - effectiveR*0.40, 0, cx - effectiveR*0.08, cy - effectiveR*0.08, effectiveR*0.85);
        h.addColorStop(0, `rgba(120,200,255,${0.10 * pulse})`);
        h.addColorStop(0.5, `rgba(80,180,255,${0.05 * pulse})`);
        h.addColorStop(1, 'rgba(80,180,255,0)');
        ctx.beginPath(); ctx.arc(cx, cy, effectiveR*1.02, 0, Math.PI*2); ctx.fillStyle = h; ctx.fill();
        ctx.restore();
    }

    function drawOrbitRing() {
        const effectiveR = R * zoomScale;
        ctx.save();
        ctx.beginPath(); let started = false;
        for (let i = 0; i <= ORBIT_POINTS; i++) {
            const a = (i/ORBIT_POINTS)*Math.PI*2;
            const ox = effectiveR*1.28*Math.cos(a), oy = effectiveR*0.32*Math.sin(a), oz = effectiveR*1.28*Math.sin(a)*Math.sin(ORBIT_TILT);
            const ry = oy*Math.cos(tilt) - oz*Math.sin(tilt), rz = oy*Math.sin(tilt) + oz*Math.cos(tilt);
            const sx = cx+ox, sy = cy-ry;
            if (rz < 0 && !started) continue;
            if (rz < 0 && started) { started = false; continue; }
            if (!started) { ctx.moveTo(sx, sy); started = true; } else ctx.lineTo(sx, sy);
        }
        const p = Math.sin(Date.now()*0.0004)*0.15+0.5;
        ctx.strokeStyle = `rgba(0,200,240,${0.05 * p})`;
        ctx.lineWidth = 0.6; ctx.stroke();
        ctx.restore();
    }

    function drawClouds() {
        const zf = Math.max(0.4, Math.min(3.0, 2.5 - zoomScale * 0.9));
        clouds.forEach(c => {
            const lon = c.lonOffset + Date.now() * c.driftSpeed;
            const lat = c.latOffset + Math.sin(Date.now()*0.00008 + c.phase) * 12;
            const p = project(lon, lat);
            if (p.z < 0.1) return;
            const effectiveR = R * zoomScale;
            const scale = effectiveR / 200;
            const sz = c.size * scale;
            ctx.save();
            ctx.globalAlpha = Math.min(0.42, c.opacity * zf * 1.6);
            const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, sz);
            g.addColorStop(0, 'rgba(255,255,255,0.34)');
            g.addColorStop(0.5, 'rgba(255,255,255,0.14)');
            g.addColorStop(1, 'rgba(255,255,255,0)');
            ctx.fillStyle = g; ctx.beginPath(); ctx.arc(p.x, p.y, sz, 0, Math.PI*2); ctx.fill();
            const ox = Math.cos(c.phase)*sz*0.5, oy = Math.sin(c.phase*0.7)*sz*0.5;
            const g2 = ctx.createRadialGradient(p.x+ox, p.y+oy, 0, p.x+ox, p.y+oy, sz*0.6);
            g2.addColorStop(0, 'rgba(255,255,255,0.18)');
            g2.addColorStop(1, 'rgba(255,255,255,0)');
            ctx.fillStyle = g2; ctx.beginPath(); ctx.arc(p.x+ox, p.y+oy, sz*0.6, 0, Math.PI*2); ctx.fill();
            ctx.restore();
        });
        cloudsFar.forEach(c => {
            const lon = c.lonOffset + Date.now() * c.driftSpeed;
            const lat = c.latOffset + Math.sin(Date.now()*0.00005 + c.phase) * 8;
            const p = project(lon, lat);
            if (p.z < 0.15) return;
            const effectiveR = R * zoomScale;
            const scale = effectiveR / 200;
            const sz = c.size * scale;
            ctx.save();
            ctx.globalAlpha = Math.min(0.22, c.opacity * zf);
            const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, sz);
            g.addColorStop(0, 'rgba(255,255,255,0.20)');
            g.addColorStop(1, 'rgba(255,255,255,0)');
            ctx.fillStyle = g; ctx.beginPath(); ctx.arc(p.x, p.y, sz, 0, Math.PI*2); ctx.fill();
            ctx.restore();
        });
    }

    function drawDataPulses() {
        const hub = NODES[0];
        const hPos = project(hub.lon, hub.lat);
        if (!hPos || hPos.z < 0.05) return;
        pulseTimer += 0.018;
        if (pulseTimer > 0.8) { pulseTimer = 0; dataPulses.push({ r: 0, max: 2.2, sp: 0.09, op: 0.65 }); }
        for (let i = dataPulses.length-1; i >=0; i--) {
            const p = dataPulses[i];
            p.r += p.sp * 0.035; p.op -= 0.005;
            if (p.op <= 0) { dataPulses.splice(i,1); continue; }
            const effectiveR = R * zoomScale;
            const wr = effectiveR * p.r;
            ctx.save();
            ctx.globalAlpha = p.op * Math.max(0, 1 - p.r/p.max);
            ctx.shadowColor = 'rgba(0,200,240,0.3)'; ctx.shadowBlur = 20;
            ctx.strokeStyle = 'rgba(0,220,255,0.6)'; ctx.lineWidth = 1.4;
            ctx.beginPath(); ctx.arc(hPos.x, hPos.y, wr, 0, Math.PI*2); ctx.stroke();
            ctx.shadowBlur = 0;
            ctx.strokeStyle = 'rgba(240,160,48,0.4)'; ctx.lineWidth = 0.8;
            ctx.beginPath(); ctx.arc(hPos.x, hPos.y, wr*0.65, 0, Math.PI*2); ctx.stroke();
            ctx.strokeStyle = 'rgba(240,160,48,0.2)'; ctx.lineWidth = 0.5;
            ctx.beginPath(); ctx.arc(hPos.x, hPos.y, wr*1.2, 0, Math.PI*2); ctx.stroke();
            ctx.restore();
        }
    }

    function addRipple(x, y) { ripples.push({x,y,r:0,max:70,op:0.7,sp:2.8}); }
    function drawRipples() {
        for (let i = ripples.length-1; i>=0; i--) {
            const r = ripples[i];
            r.r += r.sp; r.op -= 0.012;
            if (r.op <= 0) { ripples.splice(i,1); continue; }
            ctx.save();
            ctx.globalAlpha = r.op;
            ctx.strokeStyle = GOLD; ctx.lineWidth = 1.2;
            ctx.beginPath(); ctx.arc(r.x, r.y, r.r, 0, Math.PI*2); ctx.stroke();
            ctx.strokeStyle = 'rgba(255,255,255,0.25)'; ctx.lineWidth = 0.6;
            ctx.beginPath(); ctx.arc(r.x, r.y, r.r*0.65, 0, Math.PI*2); ctx.stroke();
            ctx.restore();
        }
    }

    function drawRoutes() {
        const hub = NODES[0];
        ROUTES.forEach((route, idx) => {
            const dest = NODES[route.to];
            const pts = [];
            for (let i = 0; i <= 60; i++) {
                const pt = slerp(hub.lon, hub.lat, dest.lon, dest.lat, i/60);
                const p = project(pt.lon, pt.lat); pts.push(p);
            }
            const pulse = Math.sin(Date.now()*0.0007 + idx*1.5)*0.3+0.5;
            ctx.save();
            ctx.beginPath(); let first = true;
            pts.forEach(p => { if (p.z < 0) { first = true; return; } if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y); });
            ctx.shadowColor = `rgba(0,200,255,${0.15 * pulse})`;
            ctx.shadowBlur = 18;
            ctx.strokeStyle = `rgba(0,200,255,${0.07 + pulse*0.05})`;
            ctx.lineWidth = 4 + pulse * 2;
            ctx.stroke(); ctx.shadowBlur = 0;
            ctx.beginPath(); first = true;
            pts.forEach(p => { if (p.z < 0) { first = true; return; } if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y); });
            ctx.strokeStyle = `rgba(0,220,255,${0.35 + pulse*0.2})`;
            ctx.lineWidth = 1.0;
            ctx.setLineDash([6, 10]);
            ctx.lineDashOffset = -(Date.now() * 0.04 + idx * 30) % 20;
            ctx.stroke(); ctx.setLineDash([]);
            ctx.beginPath(); first = true;
            pts.forEach(p => { if (p.z < 0) { first = true; return; } if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y); });
            ctx.strokeStyle = `rgba(240,160,48,${0.12 + pulse*0.10})`;
            ctx.lineWidth = 2.5;
            ctx.stroke();
            ctx.restore();
        });
    }

    function drawRegionLabels() {
        if (zoomScale > 1.3) return;
        const alpha = Math.max(0, Math.min(1, (1.3-zoomScale)*5));
        REGION_LABELS.forEach(rl => {
            const p = project(rl.lon, rl.lat);
            if (p.z < 0.18) return;
            const la = alpha * Math.min(1, (p.z-0.15)*4);
            ctx.save(); ctx.globalAlpha = la;
            const fs = Math.round(9*zoomScale+2);
            ctx.font = `700 ${fs}px 'Plus Jakarta Sans', sans-serif`;
            ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
            ctx.shadowColor = 'rgba(0,0,0,0.85)'; ctx.shadowBlur = 10;
            ctx.fillStyle = 'rgba(160,220,255,0.75)';
            ctx.fillText(rl.label.toUpperCase(), p.x, p.y);
            ctx.restore();
        });
    }

    function roundRect(ctx, x, y, w, h, r) {
        ctx.beginPath();
        ctx.moveTo(x+r,y); ctx.lineTo(x+w-r,y);
        ctx.quadraticCurveTo(x+w,y,x+w,y+r);
        ctx.lineTo(x+w,y+h-r); ctx.quadraticCurveTo(x+w,y+h,x+w-r,y+h);
        ctx.lineTo(x+r,y+h); ctx.quadraticCurveTo(x,y+h,x,y+h-r);
        ctx.lineTo(x,y+r); ctx.quadraticCurveTo(x,y,x+r,y);
        ctx.closePath();
    }

    /* --- PORT ICONS (used for traveling particles) --- */

    function drawNodes() {
        canvas._nodePositions = [];
        const nodeAlpha = Math.max(0, Math.min(1, (zoomScale - 0.7) * 2.2));
        NODES.forEach((node, i) => {
            const p = project(node.lon, node.lat);
            if (p.z < 0.05) { canvas._nodePositions.push(null); return; }
            canvas._nodePositions.push({ x: p.x, y: p.y, node, index: i });
            const isSelected = selectedNode && selectedNode.index === i;
            const r = node.hub ? 9 : 5;
            const color = node.hub ? GOLD : (isSelected ? GOLD : CYAN);
            const pulse = (Math.sin(Date.now()*0.003 + i*1.2)+1)/2;
            ctx.save();
            ctx.globalAlpha = nodeAlpha;
            const pr = isSelected ? r+18+pulse*10 : r+12+pulse*8;
            ctx.beginPath(); ctx.arc(p.x, p.y, pr, 0, Math.PI*2);
            ctx.fillStyle = isSelected ? `rgba(240,160,48,0.10)` : (node.hub ? `rgba(240,160,48,0.07)` : `rgba(0,200,240,0.06)`);
            ctx.fill();
            ctx.beginPath(); ctx.arc(p.x, p.y, r+3.5, 0, Math.PI*2);
            ctx.strokeStyle = isSelected ? 'rgba(240,160,48,0.9)' : (node.hub ? 'rgba(240,160,48,0.6)' : 'rgba(0,200,240,0.6)');
            ctx.lineWidth = isSelected ? 2.0 : 1.0; ctx.stroke();
            ctx.beginPath(); ctx.arc(p.x, p.y, r, 0, Math.PI*2);
            ctx.fillStyle = color;
            ctx.shadowColor = color;
            ctx.shadowBlur = node.hub ? 20 : (isSelected ? 20 : 10);
            ctx.fill(); ctx.shadowBlur = 0;
            ctx.beginPath(); ctx.arc(p.x, p.y, r*0.4, 0, Math.PI*2);
            ctx.fillStyle = 'rgba(255,255,255,0.85)'; ctx.fill();
            /* Radar ping: anillo que se expande y se desvanece, en bucle, por nodo */
            const pingCycle = 2200;
            const pingT = ((Date.now() + i * 700) % pingCycle) / pingCycle;
            const pingR = r + 4 + pingT * (node.hub ? 30 : 20);
            const pingA = (1 - pingT) * (node.hub ? 0.35 : 0.22);
            ctx.beginPath(); ctx.arc(p.x, p.y, pingR, 0, Math.PI*2);
            ctx.strokeStyle = node.hub ? `rgba(240,160,48,${pingA})` : `rgba(0,220,255,${pingA})`;
            ctx.lineWidth = 1.3; ctx.stroke();
            if (node.hub) {
                const rp = Math.sin(Date.now()*0.002)*0.3+0.7;
                ctx.beginPath(); ctx.arc(p.x, p.y, r+8+pulse*4, 0, Math.PI*2);
                ctx.strokeStyle = `rgba(240,160,48,${0.12 * rp})`;
                ctx.lineWidth = 1.2; ctx.stroke();
                const angle = Date.now() * 0.001;
                for (let j = 0; j < 8; j++) {
                    const a = angle + (j / 8) * Math.PI * 2;
                    const or = r + 14 + Math.sin(angle * 0.4 + j) * 3;
                    ctx.beginPath(); ctx.arc(p.x + Math.cos(a) * or, p.y + Math.sin(a) * or, 1.6, 0, Math.PI * 2);
                    ctx.fillStyle = `rgba(240,160,48,${0.5 + Math.sin(a + angle) * 0.3})`;
                    ctx.fill();
                }
            }
            if (nodeAlpha > 0.25) {
            }
            ctx.restore();
            // Label: solo cuando el nodo está seleccionado (click), nunca automático
            if (isSelected) {
                ctx.save(); ctx.globalAlpha = 1;
                ctx.font = `600 10px 'Plus Jakarta Sans', sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
                const gap = r + 12;
                let lx = p.x, ly = p.y;
                if (node.labelDir === 'above') ly = p.y - gap;
                else if (node.labelDir === 'below') ly = p.y + gap;
                else if (node.labelDir === 'right') lx = p.x + gap + 6;
                else if (node.labelDir === 'left') lx = p.x - gap - 6;
                const lw = Math.max(ctx.measureText(node.label).width + 18, 60);
                ctx.fillStyle = 'rgba(180,70,0,0.92)';
                roundRect(ctx, lx - lw/2, ly - 9, lw, 18, 4);
                ctx.fill();
                ctx.fillStyle = '#fff';
                ctx.fillText(node.label, lx, ly);
                ctx.restore();
            }
        });
    }

    function drawMiniShip(ctx2, x, y, s) {
        ctx2.save();
        ctx2.font = `900 ${s}px "Font Awesome 6 Free"`;
        ctx2.textAlign = 'center'; ctx2.textBaseline = 'middle';
        ctx2.fillStyle = '#4dd0e1';
        ctx2.fillText('\uf21a', x, y);
        ctx2.restore();
    }
    function drawMiniTruck(ctx2, x, y, s) {
        ctx2.save();
        ctx2.font = `900 ${s}px "Font Awesome 6 Free"`;
        ctx2.textAlign = 'center'; ctx2.textBaseline = 'middle';
        ctx2.fillStyle = '#d09030';
        ctx2.fillText('\uf0d1', x, y);
        ctx2.restore();
    }
    function drawParticles() {
        particles.forEach(p => {
            const route = ROUTES[p.routeIdx];
            const from = p.dir === 1 ? 0 : route.to;
            const to = p.dir === 1 ? route.to : 0;
            const hub = NODES[from], dest = NODES[to];
            const pt = slerp(hub.lon, hub.lat, dest.lon, dest.lat, p.t);
            const pos = project(pt.lon, pt.lat);
            if (pos.z < 0.05) return;
            const sz = Math.min(zoomScale, 1.3) * 10;
            if (p.type === 'ship') drawMiniShip(ctx, pos.x, pos.y, sz);
            else drawMiniTruck(ctx, pos.x, pos.y, sz);
        });
    }

    /* --- PREMIUM INFO CARD --- */
    function showInfoCard(nodeData, projPos) {
        const n = nodeData.node;
        const card = document.getElementById('globeInfoCard');
        if (!card) return;
        document.getElementById('globeInfoTag').textContent = n.hub ? '★ HUB PRINCIPAL' : '⚓ PUERTO';
        document.getElementById('globeInfoTitle').textContent = n.label.replace(' (HUB)', '');
        document.getElementById('globeInfoDesc').textContent = n.desc;
        const imgEl = document.getElementById('globeInfoImg');
        if (imgEl) {
            imgEl.alt = '';
            imgEl.style.transition = 'opacity .18s ease';
            imgEl.style.opacity = '0';
            const preloader = new Image();
            preloader.onload = function() {
                imgEl.src = n.image;
                imgEl.style.display = 'block';
                requestAnimationFrame(function() { imgEl.style.opacity = '1'; });
            };
            preloader.onerror = function() {
                imgEl.style.display = 'none';
            };
            preloader.src = n.image;
        }
        const cardW = 210, cardH = 170;
        let left = projPos.x + 16, top = projPos.y - 55;
        if (left + cardW > canvas.width - 6) left = projPos.x - cardW - 16;
        if (left < 6) left = 6;
        if (top + cardH > canvas.height - 6) top = canvas.height - cardH - 6;
        if (top < 6) top = 6;
        card.style.left = left + 'px'; card.style.top = top + 'px'; card.style.width = cardW + 'px';
        card.classList.add('visible');
        rotSpeed = 0.00005; targetRotSpeed = 0.00005;
        addRipple(projPos.x, projPos.y);
    }

    function hideInfoCard() {
        const card = document.getElementById('globeInfoCard');
        if (card) card.classList.remove('visible');
        rotSpeed = 0.00035; targetRotSpeed = 0.00035;
        selectedNode = null;
    }

    function resize() {
        const rect = canvas.parentElement.getBoundingClientRect();
        canvas.width = rect.width; canvas.height = rect.height;
        cx = canvas.width * 0.5; cy = canvas.height * 0.5;
        R = Math.min(canvas.width, canvas.height) * 0.40;
    }

    function loop() {
        time = Date.now() * 0.001;
        const zd = targetZoom - zoomScale;
        if (Math.abs(zd) < 0.003) zoomScale = targetZoom;
        else zoomScale += zd * (Math.abs(zd) > 0.2 ? 0.25 : 0.10);
        rotSpeed += (targetRotSpeed - rotSpeed) * 0.02;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        if (!isDragging) {
            rot += rotSpeed;
            if (!isHovering && !selectedNode && !didDrag) {
                tilt += Math.sin(Date.now() * 0.00012) * 0.000015;
                tilt = Math.max(TILT_MIN * 0.5, Math.min(TILT_MAX * 0.5, tilt));
            }
        }
        drawGlobe();
        drawOrbitRing();
        drawClouds();
        drawShootingStars();
        drawRoutes();
        drawParticles();
        drawDataPulses();
        drawNodes();
        drawRegionLabels();
        drawRipples();
        if (!isDragging && canvas._nodePositions) {
            let over = false;
            canvas._nodePositions.forEach(pos => { if (pos && Math.hypot(mouseX-pos.x, mouseY-pos.y) <= 16) over = true; });
            canvas.style.cursor = over ? 'pointer' : 'grab';
        }
        particles.forEach(p => { p.t += p.speed; if (p.t > 1) p.t = 0; });
        if (isRunning) requestAnimationFrame(loop);
    }

    /* --- EVENTS --- */
    canvas.addEventListener('mousedown', e => {
        isDragging = false; didDrag = false; lastX = e.clientX; lastY = e.clientY;
        canvas.style.cursor = 'grabbing';
        canvas._dragStartX = e.clientX; canvas._dragStartY = e.clientY; canvas._isMouseDown = true;
    });
    window.addEventListener('mouseup', () => { canvas._isMouseDown = false; isDragging = false; canvas.style.cursor = 'grab'; });
    window.addEventListener('mousemove', e => {
        if (canvas._isMouseDown) {
            const dx = e.clientX - canvas._dragStartX, dy = e.clientY - canvas._dragStartY;
            if (Math.hypot(dx,dy) > 4) { isDragging = true; didDrag = true; rot += (e.clientX-lastX)*0.004; tilt += (e.clientY-lastY)*0.004; if (tilt<TILT_MIN) tilt=TILT_MIN; if (tilt>TILT_MAX) tilt=TILT_MAX; canvas.style.cursor='grabbing'; }
        }
        lastX = e.clientX; lastY = e.clientY;
        const rect = canvas.getBoundingClientRect(), sx = canvas.width/rect.width, sy = canvas.height/rect.height;
        mouseX = (e.clientX - rect.left)*sx; mouseY = (e.clientY - rect.top)*sy;
        const effR = R*zoomScale;
        isHovering = Math.hypot(mouseX-cx, mouseY-cy) < effR*1.2;
        if (isHovering && !isDragging && !selectedNode) targetRotSpeed = 0.00008;
        else if (!selectedNode) targetRotSpeed = 0.00035;
    });
    canvas.addEventListener('mouseenter', () => { isHovering = true; });
    canvas.addEventListener('mouseleave', () => { isHovering = false; if (!selectedNode) targetRotSpeed = 0.00035; });
    canvas.addEventListener('click', e => {
        if (didDrag) { didDrag = false; return; }
        const rect = canvas.getBoundingClientRect(), sx = canvas.width/rect.width, sy = canvas.height/rect.height;
        const cx2 = (e.clientX-rect.left)*sx, cy2 = (e.clientY-rect.top)*sy;
        if (!canvas._nodePositions) return;
        let found = null;
        canvas._nodePositions.forEach(pos => { if (pos && Math.hypot(cx2-pos.x, cy2-pos.y) <= 18) found = pos; });
        if (found) { if (selectedNode && selectedNode.index === found.index) hideInfoCard(); else { selectedNode = found; showInfoCard(found, {x:found.x,y:found.y}); } }
        else hideInfoCard();
    });
    canvas.addEventListener('wheel', e => { e.preventDefault(); const d = e.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP; targetZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, targetZoom + d)); }, { passive: false });
    let ltx=0,lty=0,ltd=0;
    canvas.addEventListener('touchstart', e => { ltx=e.touches[0].clientX; lty=e.touches[0].clientY; didDrag=false; if (e.touches.length===2) ltd=Math.hypot(e.touches[0].clientX-e.touches[1].clientX, e.touches[0].clientY-e.touches[1].clientY); }, { passive: true });
    canvas.addEventListener('touchmove', e => {
        e.preventDefault();
        if (e.touches.length===2) { const d=Math.hypot(e.touches[0].clientX-e.touches[1].clientX, e.touches[0].clientY-e.touches[1].clientY); targetZoom=Math.max(ZOOM_MIN,Math.min(ZOOM_MAX,targetZoom+(d-ltd)*0.005)); ltd=d; return; }
        const dx=e.touches[0].clientX-ltx, dy=e.touches[0].clientY-lty;
        if (Math.hypot(dx,dy)>3) didDrag=true;
        rot+=dx*0.004; tilt+=dy*0.004; if (tilt<TILT_MIN) tilt=TILT_MIN; if (tilt>TILT_MAX) tilt=TILT_MAX;
        ltx=e.touches[0].clientX; lty=e.touches[0].clientY;
    }, { passive: false });
    canvas.addEventListener('touchend', e => {
        if (!didDrag && e.changedTouches.length>0) {
            const t=e.changedTouches[0], rect=canvas.getBoundingClientRect(), sx=canvas.width/rect.width, sy=canvas.height/rect.height;
            const tx=(t.clientX-rect.left)*sx, ty=(t.clientY-rect.top)*sy;
            if (!canvas._nodePositions) return;
            let found=null;
            canvas._nodePositions.forEach(pos => { if (pos && Math.hypot(tx-pos.x,ty-pos.y)<=20) found=pos; });
            if (found) { if (selectedNode && selectedNode.index===found.index) hideInfoCard(); else { selectedNode=found; showInfoCard(found,{x:found.x,y:found.y}); } } else hideInfoCard();
        }
    });
    document.getElementById('zoomIn')?.addEventListener('click', () => { targetZoom = Math.min(ZOOM_MAX, targetZoom+ZOOM_STEP*1.5); });
    document.getElementById('zoomOut')?.addEventListener('click', () => { targetZoom = Math.max(ZOOM_MIN, targetZoom-ZOOM_STEP*1.5); });
    document.getElementById('zoomReset')?.addEventListener('click', () => { targetZoom = 1.0; });
    canvas.style.cursor = 'grab';
    let isRunning = false;

    const sectionEl = document.querySelector('.strategic-section') || canvas;
    const obs = new IntersectionObserver(function(entries) {
        entries.forEach(function(e) {
            if (e.isIntersecting) {
                if (!isRunning && canvas.width > 0 && canvas.height > 0) {
                    isRunning = true;
                    resize();
                    loop();
                }
            } else {
                isRunning = false;
            }
        });
    }, { threshold: 0.1, rootMargin: '0px' });
    obs.observe(sectionEl);

    var globeToggle = document.getElementById('globeToggle');
    var globeCollapse = document.getElementById('globeCollapse');
    if (globeToggle && globeCollapse) {
        var strategicMap = document.querySelector('.strategic-map');
        globeToggle.addEventListener('click', function() {
            var isExpanded = globeCollapse.classList.contains('expanded');
            if (isExpanded) {
                globeCollapse.classList.remove('expanded');
                if (strategicMap) strategicMap.classList.remove('expanded');
                globeToggle.classList.remove('active');
                isRunning = false;
                canvas.width = 0;
                canvas.height = 0;
            } else {
                globeCollapse.classList.add('expanded');
                if (strategicMap) strategicMap.classList.add('expanded');
                globeToggle.classList.add('active');
                setTimeout(function() {
                    resize();
                    if (!isRunning && canvas.width > 0 && canvas.height > 0) {
                        isRunning = true;
                        loop();
                    }
                }, 50);
            }
        });
    }

    window.addEventListener('resize', resize);
    resize();
})();