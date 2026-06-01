
    document.addEventListener('DOMContentLoaded', function () {

        /* 1 – SCROLL PROGRESS */
        const bar = document.getElementById('scroll-progress');
        window.addEventListener('scroll', () => {
            bar.style.width = (window.scrollY / (document.documentElement.scrollHeight - window.innerHeight) * 100) + '%';
        }, { passive: true });

        /* 2 – HEADER SHRINK */
        const hdr = document.getElementById('main-header');
        window.addEventListener('scroll', () => hdr.classList.toggle('scrolled', window.scrollY > 60), { passive: true });

        /* 3 – HERO LINE */
        setTimeout(() => document.getElementById('heroLine').classList.add('animate'), 350);

        /* 4 – TYPING EFFECT */
        const typed = document.getElementById('typedText');
        const words = ['MARDIQUE', 'EL FUTURO', 'CONECTIVIDAD'];
        let wi = 0, ci = 0, deleting = false;
        function typeLoop() {
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
        typeLoop();

        /* 5 – PARTÍCULAS HERO */
        const pc = document.getElementById('heroParticles');
        for (let i = 0; i < 14; i++) {
            const el = document.createElement('div'); el.className = 'hero-particle';
            const s = Math.random() * 90 + 20;
            el.style.cssText = `width:${s}px;height:${s}px;left:${Math.random()*100}%;top:${Math.random()*100}%;animation-duration:${Math.random()*7+5}s;animation-delay:${Math.random()*4}s;opacity:${Math.random()*0.15+0.04};`;
            pc.appendChild(el);
        }

        /* 6 – REVEAL ON SCROLL */
        const io = new IntersectionObserver(entries => {
            entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('visible'); io.unobserve(e.target); } });
        }, { threshold: 0.1, rootMargin: '0px 0px -36px 0px' });
        document.querySelectorAll('.reveal, .reveal-left, .reveal-right').forEach(el => io.observe(el));

        /* 7 – CONTADORES ANIMADOS */
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

    /* 8 – LIGHTBOX */
    function openLightbox(src) {
        document.getElementById('lightboxImg').src = src;
        document.getElementById('lightbox').classList.add('open');
        document.body.style.overflow = 'hidden';
    }
    function closeLightbox() {
        document.getElementById('lightbox').classList.remove('open');
        document.body.style.overflow = '';
    }
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeLightbox(); });

    /* ============================================================
       GLOBO 3D — CON ZOOM (rueda + botones)
       ============================================================ */
    (function() {
        const canvas = document.getElementById('globeCanvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');

        const GOLD = '#f09a36', CYAN = '#00a3e0';
        let zoomScale = 1.0;
        const ZOOM_MIN = 0.55, ZOOM_MAX = 2.8, ZOOM_STEP = 0.15;

        const NODES = [
            { lon: -75.5, lat: 10.4,  label: 'Cartagena (HUB)', hub: true,  labelDir: 'below' },
            { lon: -100,  lat: 40,    label: 'Norteamérica',    hub: false, labelDir: 'above' },
            { lon: -87,   lat: 14,    label: 'Centroamérica',   hub: false, labelDir: 'left'  },
            { lon:  10,   lat: 50,    label: 'Europa',          hub: false, labelDir: 'above' },
            { lon: -55,   lat: -15,   label: 'Sudamérica',      hub: false, labelDir: 'right' },
            { lon:  115,  lat:  25,   label: 'Asia',            hub: false, labelDir: 'above' },
        ];
        const ROUTES = [
            { from: 0, to: 1, vehicle: '🚛' },
            { from: 0, to: 2, vehicle: '🚢' },
            { from: 0, to: 3, vehicle: '🚢' },
            { from: 0, to: 4, vehicle: '🚢' },
            { from: 0, to: 5, vehicle: '🚢' },
        ];

        let rot = 1.32, rotSpeed = 0.0005, isDragging = false, lastX = 0;
        let R, cx, cy;

        const particles = ROUTES.map((r, i) => ({
            routeIdx: i, t: (i / ROUTES.length) * 0.9,
            speed: 0.00035 + Math.random() * 0.0003,
        }));

        const RIVERS = [
            { points: [[-73,-4],[-68,-4],[-62,-3],[-56,-2],[-51,-1],[-46,-1],[-40,0],[-38,0]], color: 'rgba(0,163,224,0.55)', width: 1.6 },
            { points: [[-72,5],[-68,6],[-64,7],[-62,7],[-60,8],[-62,9],[-63,11]], color: 'rgba(0,163,224,0.4)', width: 0.9 },
            { points: [[-75,2],[-74,4],[-74,6],[-74,8],[-74,10],[-75.5,10.4]], color: 'rgba(0,200,255,0.7)', width: 1.3 },
            { points: [[-93,48],[-92,44],[-90,40],[-89,36],[-90,32],[-90,29],[-89,28]], color: 'rgba(0,163,224,0.35)', width: 0.9 },
            { points: [[32,4],[32,8],[33,12],[33,16],[33,20],[33,24],[31,28],[30,32],[31,36]], color: 'rgba(0,163,224,0.35)', width: 0.8 },
            { points: [[-54,-24],[-56,-28],[-58,-30],[-58,-33],[-58,-35],[-57,-38]], color: 'rgba(0,163,224,0.45)', width: 1.1 },
            { points: [[-58,-18],[-58,-20],[-58,-22],[-57,-24],[-56,-26],[-55,-27],[-54,-24]], color: 'rgba(0,163,224,0.35)', width: 0.8 },
            { points: [[-46,-10],[-44,-12],[-43,-14],[-41,-16],[-38,-18],[-36,-10]], color: 'rgba(0,163,224,0.35)', width: 0.8 },
            { points: [[-50,-5],[-49,-8],[-48,-10],[-47,-12],[-48,-15]], color: 'rgba(0,163,224,0.3)', width: 0.7 },
            { points: [[-67,-1],[-65,0],[-63,1],[-62,2],[-61,3],[-60,3]], color: 'rgba(0,163,224,0.3)', width: 0.7 },
        ];

        const RIVER_BOATS = [
            { riverIdx: 0, t: 0.3, speed: 0.0012 },
            { riverIdx: 0, t: 0.7, speed: 0.0009 },
            { riverIdx: 2, t: 0.5, speed: 0.0015 },
            { riverIdx: 5, t: 0.4, speed: 0.0011 },
            { riverIdx: 5, t: 0.75, speed: 0.0008 },
            { riverIdx: 9, t: 0.5, speed: 0.0013 },
        ];

        const CONTINENTS = [
            { name: 'namerica', baseColor: [48,108,68], points: [[-168,72],[-156,72],[-140,70],[-130,70],[-125,68],[-110,68],[-90,70],[-85,70],[-75,65],[-65,63],[-60,52],[-55,47],[-66,44],[-70,42],[-75,40],[-76,35],[-80,30],[-84,25],[-88,20],[-90,18],[-93,17],[-100,18],[-104,19],[-108,23],[-112,27],[-115,30],[-117,32],[-120,35],[-122,38],[-124,46],[-128,52],[-132,56],[-138,58],[-145,62],[-155,62],[-160,60],[-165,64],[-168,68],[-168,72]] },
            { name: 'camerica', baseColor: [52,112,70], points: [[-90,18],[-86,16],[-83,14],[-80,12],[-77,9],[-75,8],[-77,8],[-80,9],[-83,9],[-85,11],[-88,15],[-90,18]] },
            { name: 'samerica', baseColor: [44,100,62], points: [[-75,11],[-72,10],[-65,8],[-60,8],[-55,7],[-50,5],[-40,4],[-35,5],[-34,-4],[-36,-10],[-38,-15],[-40,-22],[-43,-24],[-48,-28],[-52,-32],[-56,-36],[-62,-40],[-65,-44],[-66,-50],[-68,-54],[-70,-56],[-72,-50],[-74,-44],[-74,-38],[-72,-30],[-70,-20],[-75,-14],[-80,-8],[-80,-2],[-78,2],[-78,6],[-75,11]] },
            { name: 'europe', baseColor: [54,116,74], points: [[-10,36],[0,36],[5,37],[10,38],[15,38],[18,40],[25,40],[28,42],[30,44],[28,52],[25,56],[22,60],[18,64],[14,68],[10,70],[5,70],[0,68],[-5,65],[-8,62],[-10,58],[-6,54],[-2,50],[0,47],[3,46],[6,44],[10,43],[15,44],[18,42],[22,40],[25,40],[18,40],[10,38],[5,37],[0,36],[-10,36]] },
            { name: 'africa', baseColor: [42,96,56], points: [[-18,15],[-15,18],[-5,18],[0,16],[8,16],[15,16],[22,14],[28,12],[32,12],[38,12],[42,14],[45,16],[44,20],[42,26],[38,30],[34,34],[32,38],[28,42],[22,36],[16,36],[10,36],[4,34],[0,32],[-4,28],[-8,22],[-14,18],[-18,15]] },
            { name: 'asia', baseColor: [48,104,66], points: [[30,40],[38,38],[45,40],[52,38],[60,36],[68,34],[74,32],[80,28],[90,24],[98,20],[104,18],[110,18],[116,20],[122,24],[128,30],[132,35],[138,40],[142,44],[145,48],[142,54],[136,58],[128,62],[120,66],[110,70],[100,70],[88,68],[76,66],[64,64],[52,62],[42,60],[38,56],[34,52],[30,48],[28,44],[30,40]] },
            { name: 'oceania', baseColor: [44,100,60], points: [[114,-22],[118,-20],[122,-18],[126,-16],[130,-14],[136,-14],[138,-16],[142,-18],[146,-18],[149,-22],[149,-26],[148,-30],[145,-36],[140,-38],[136,-38],[132,-36],[128,-34],[122,-30],[116,-26],[114,-22]] },
            { name: 'greenland', baseColor: [80,140,100], points: [[-44,82],[-30,80],[-18,76],[-20,70],[-28,64],[-44,62],[-52,64],[-58,68],[-56,74],[-48,80],[-44,82]] },
        ];

        const MOUNTAINS = [[-70,-32],[72,28],[86,28],[-105,40],[-68,-32],[-78,0]];

        function toRad(d) { return d * Math.PI / 180; }

        function project(lon, lat) {
            const lam = toRad(lon) + rot;
            const phi = toRad(lat);
            const effectiveR = R * zoomScale;
            const x = effectiveR * Math.cos(phi) * Math.sin(lam);
            const y = -effectiveR * Math.sin(phi);
            const z = Math.cos(phi) * Math.cos(lam);
            return { x: cx + x, y: cy + y, z };
        }

        function slerp(lon0, lat0, lon1, lat1, t) {
            return { lon: lon0 + (lon1 - lon0) * t, lat: lat0 + (lat1 - lat0) * t };
        }

        function continentFacing(points) {
            let sumLon = 0, sumLat = 0;
            points.forEach(p => { sumLon += p[0]; sumLat += p[1]; });
            return project(sumLon / points.length, sumLat / points.length).z;
        }

        function drawGlobe() {
            const effectiveR = R * zoomScale;
            const grd = ctx.createRadialGradient(cx - effectiveR*0.25, cy - effectiveR*0.28, effectiveR*0.02, cx, cy, effectiveR);
            grd.addColorStop(0, '#1d6da0'); grd.addColorStop(0.2, '#0d4272');
            grd.addColorStop(0.55, '#062848'); grd.addColorStop(0.85, '#031828'); grd.addColorStop(1, '#010e1a');
            ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = grd; ctx.fill();

            const shimmer = ctx.createRadialGradient(cx - effectiveR*0.32, cy - effectiveR*0.36, 0, cx, cy, effectiveR*0.9);
            shimmer.addColorStop(0, 'rgba(120,200,255,0.16)'); shimmer.addColorStop(0.4, 'rgba(60,140,220,0.05)'); shimmer.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = shimmer; ctx.fill();

            ctx.save();
            ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.clip();

            for (let lat = -80; lat <= 80; lat += 10) drawLatLine(lat);
            for (let lon = 0; lon < 360; lon += 15) drawLonLine(lon);
            drawTropicLine(23.5, 'rgba(240,154,54,0.1)');
            drawTropicLine(-23.5, 'rgba(240,154,54,0.1)');

            CONTINENTS.forEach(cont => {
                const facing = continentFacing(cont.points);
                if (facing < -0.08) return;
                const alpha = Math.max(0, Math.min(1, facing * 1.8 + 0.15));
                const [r, g, b] = cont.baseColor;

                ctx.beginPath();
                cont.points.forEach((pt, i) => {
                    const p = project(pt[0], pt[1]);
                    if (i === 0) ctx.moveTo(p.x, p.y); else ctx.lineTo(p.x, p.y);
                });
                ctx.closePath();

                const centP = project(
                    cont.points.reduce((s,p)=>s+p[0],0)/cont.points.length,
                    cont.points.reduce((s,p)=>s+p[1],0)/cont.points.length
                );
                const landGrd = ctx.createRadialGradient(centP.x-10, centP.y-12, 2, centP.x, centP.y, effectiveR*0.45);
                landGrd.addColorStop(0, `rgba(${r+22},${g+22},${b+12},${0.9*alpha})`);
                landGrd.addColorStop(0.45, `rgba(${r+6},${g+8},${b+2},${0.82*alpha})`);
                landGrd.addColorStop(1, `rgba(${Math.max(0,r-16)},${Math.max(0,g-14)},${Math.max(0,b-10)},${0.7*alpha})`);
                ctx.fillStyle = landGrd; ctx.fill();

                ctx.beginPath();
                cont.points.forEach((pt, i) => {
                    const p2 = project(pt[0], pt[1]);
                    if (i === 0) ctx.moveTo(p2.x, p2.y); else ctx.lineTo(p2.x, p2.y);
                });
                ctx.closePath();
                ctx.strokeStyle = `rgba(${r+50},${g+70},${b+35},${0.6*alpha})`; ctx.lineWidth = 1.0; ctx.stroke();
            });

            MOUNTAINS.forEach(([mLon, mLat]) => {
                const p = project(mLon, mLat); if (p.z < 0.1) return;
                const mAlpha = Math.min(1, p.z*2), mH = effectiveR*0.025;
                ctx.beginPath(); ctx.moveTo(p.x, p.y-mH); ctx.lineTo(p.x-mH*0.6, p.y+mH*0.4); ctx.lineTo(p.x+mH*0.6, p.y+mH*0.4); ctx.closePath();
                ctx.fillStyle = `rgba(200,220,200,${0.22*mAlpha})`; ctx.fill();
                ctx.beginPath(); ctx.moveTo(p.x, p.y-mH); ctx.lineTo(p.x-mH*0.25, p.y-mH*0.3); ctx.lineTo(p.x+mH*0.25, p.y-mH*0.3); ctx.closePath();
                ctx.fillStyle = `rgba(255,255,255,${0.35*mAlpha})`; ctx.fill();
            });

            RIVERS.forEach(river => {
                ctx.beginPath(); let firstR = true;
                river.points.forEach(([rLon, rLat]) => {
                    const p = project(rLon, rLat); if (p.z < 0) { firstR = true; return; }
                    if (firstR) { ctx.moveTo(p.x, p.y); firstR = false; } else ctx.lineTo(p.x, p.y);
                });
                ctx.strokeStyle = river.color; ctx.lineWidth = river.width * zoomScale; ctx.lineJoin = ctx.lineCap = 'round'; ctx.stroke();
            });

            ctx.restore();

            const atmOut = ctx.createRadialGradient(cx, cy, effectiveR*0.88, cx, cy, effectiveR*1.14);
            atmOut.addColorStop(0, 'rgba(0,163,224,0)'); atmOut.addColorStop(0.5, 'rgba(0,163,224,0.2)');
            atmOut.addColorStop(0.8, 'rgba(30,80,200,0.07)'); atmOut.addColorStop(1, 'rgba(0,20,60,0)');
            ctx.beginPath(); ctx.arc(cx, cy, effectiveR*1.14, 0, Math.PI*2); ctx.fillStyle = atmOut; ctx.fill();

            const hlGrd = ctx.createRadialGradient(cx-effectiveR*0.35, cy-effectiveR*0.38, 0, cx-effectiveR*0.1, cy-effectiveR*0.1, effectiveR*0.65);
            hlGrd.addColorStop(0, 'rgba(255,255,255,0.16)'); hlGrd.addColorStop(0.4, 'rgba(255,255,255,0.04)'); hlGrd.addColorStop(1, 'rgba(255,255,255,0)');
            ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2); ctx.fillStyle = hlGrd; ctx.fill();

            ctx.beginPath(); ctx.arc(cx, cy, effectiveR, 0, Math.PI*2);
            ctx.strokeStyle = 'rgba(0,163,224,0.22)'; ctx.lineWidth = 1.2; ctx.stroke();
        }

        function drawLatLine(lat) {
            const steps = 90; ctx.beginPath(); let first = true;
            for (let i = 0; i <= steps; i++) {
                const p = project(-180+360*i/steps, lat); if (p.z < 0) { first = true; continue; }
                if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y);
            }
            ctx.strokeStyle = lat === 0 ? 'rgba(0,163,224,0.3)' : 'rgba(255,255,255,0.042)';
            ctx.lineWidth = lat === 0 ? 1.2 : 0.4; ctx.stroke();
        }

        function drawTropicLine(lat, color) {
            const steps = 90; ctx.beginPath(); let first = true;
            for (let i = 0; i <= steps; i++) {
                const p = project(-180+360*i/steps, lat); if (p.z < 0) { first = true; continue; }
                if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y);
            }
            ctx.strokeStyle = color; ctx.lineWidth = 0.8;
            ctx.setLineDash([4,6]); ctx.stroke(); ctx.setLineDash([]);
        }

        function drawLonLine(lonBase) {
            const steps = 40; ctx.beginPath(); let first = true;
            for (let i = 0; i <= steps; i++) {
                const p = project(lonBase, -90+180*i/steps); if (p.z < 0) { first = true; continue; }
                if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y);
            }
            ctx.strokeStyle = 'rgba(255,255,255,0.033)'; ctx.lineWidth = 0.3; ctx.stroke();
        }

        function drawRoutes() {
            const hub = NODES[0];
            ROUTES.forEach(route => {
                const dest = NODES[route.to]; const steps = 100;
                ctx.beginPath(); let first = true;
                for (let i = 0; i <= steps; i++) {
                    const pt = slerp(hub.lon, hub.lat, dest.lon, dest.lat, i/steps);
                    const p = project(pt.lon, pt.lat); if (p.z < 0) { first = true; continue; }
                    if (first) { ctx.moveTo(p.x, p.y+1.5); first = false; } else ctx.lineTo(p.x, p.y+1.5);
                }
                ctx.strokeStyle = 'rgba(0,0,0,0.2)'; ctx.lineWidth = 2;
                ctx.setLineDash([6,7]); ctx.stroke(); ctx.setLineDash([]);

                ctx.beginPath(); first = true;
                for (let i = 0; i <= steps; i++) {
                    const pt = slerp(hub.lon, hub.lat, dest.lon, dest.lat, i/steps);
                    const p = project(pt.lon, pt.lat); if (p.z < 0) { first = true; continue; }
                    if (first) { ctx.moveTo(p.x, p.y); first = false; } else ctx.lineTo(p.x, p.y);
                }
                ctx.strokeStyle = 'rgba(0,200,255,0.5)'; ctx.lineWidth = 1.4;
                ctx.setLineDash([6,7]); ctx.stroke(); ctx.setLineDash([]);
            });
        }

        function drawRiverBoats() {
            RIVER_BOATS.forEach(boat => {
                const river = RIVERS[boat.riverIdx]; const pts = river.points;
                const totalSegs = pts.length-1, globalT = boat.t*totalSegs;
                const segIdx = Math.min(Math.floor(globalT), totalSegs-1);
                const segT = globalT-segIdx;
                const p0 = pts[segIdx], p1 = pts[Math.min(segIdx+1, pts.length-1)];
                const rLon = p0[0]+(p1[0]-p0[0])*segT, rLat = p0[1]+(p1[1]-p0[1])*segT;
                const p = project(rLon, rLat); if (p.z < 0.1) return;
                ctx.save();
                ctx.font = `${Math.round(13*Math.min(zoomScale,1.4))}px "Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
                ctx.fillStyle = 'rgba(0,0,0,0.25)';
                ctx.beginPath(); ctx.arc(p.x, p.y, 9, 0, Math.PI*2); ctx.fill();
                ctx.fillText('🚢', p.x, p.y);
                ctx.restore();
            });
        }

        function drawNodes() {
            NODES.forEach((node, i) => {
                const p = project(node.lon, node.lat); if (p.z < 0.05) return;
                const r = node.hub ? 9 : 5.5;
                const color = node.hub ? GOLD : CYAN;
                const pulse = (Math.sin(Date.now()*0.002+i*1.2)+1)/2;

                ctx.beginPath(); ctx.arc(p.x, p.y, r+10+pulse*8, 0, Math.PI*2);
                ctx.fillStyle = node.hub ? `rgba(240,154,54,${0.05+pulse*0.07})` : `rgba(0,163,224,${0.05+pulse*0.07})`; ctx.fill();

                ctx.beginPath(); ctx.arc(p.x, p.y, r+5, 0, Math.PI*2);
                ctx.strokeStyle = node.hub ? 'rgba(240,154,54,0.4)' : 'rgba(0,163,224,0.4)'; ctx.lineWidth = 1; ctx.stroke();

                ctx.beginPath(); ctx.arc(p.x, p.y, r+2.5, 0, Math.PI*2);
                ctx.strokeStyle = node.hub ? 'rgba(240,154,54,0.72)' : 'rgba(0,163,224,0.72)'; ctx.lineWidth = 1.5; ctx.stroke();

                ctx.beginPath(); ctx.arc(p.x, p.y, r, 0, Math.PI*2);
                ctx.fillStyle = color; ctx.shadowColor = color; ctx.shadowBlur = node.hub ? 20 : 12; ctx.fill(); ctx.shadowBlur = 0;

                ctx.beginPath(); ctx.arc(p.x, p.y, r*0.38, 0, Math.PI*2);
                ctx.fillStyle = 'rgba(255,255,255,0.95)'; ctx.fill();

                ctx.save();
                ctx.font = `bold 11px 'Plus Jakarta Sans',sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
                const textW = ctx.measureText(node.label).width;
                const padX = 10, boxW = textW+padX*2, boxH = 20, gap = r+13;
                let labelCX=p.x, labelCY=p.y, lSX=p.x, lSY=p.y, lEX=p.x, lEY=p.y;
                switch(node.labelDir) {
                    case 'above': labelCY=p.y-gap; lSY=p.y-r-1; lEY=labelCY+boxH/2; break;
                    case 'below': labelCY=p.y+gap; lSY=p.y+r+1; lEY=labelCY-boxH/2; break;
                    case 'left':  labelCX=p.x-boxW/2-gap; lSX=p.x-r-1; lEX=labelCX+boxW/2; lSY=lEY=p.y; labelCY=p.y; break;
                    case 'right': labelCX=p.x+boxW/2+gap; lSX=p.x+r+1; lEX=labelCX-boxW/2; lSY=lEY=p.y; labelCY=p.y; break;
                }
                ctx.beginPath(); ctx.moveTo(lSX, lSY); ctx.lineTo(lEX, lEY);
                ctx.strokeStyle = node.hub ? 'rgba(240,154,54,0.55)' : 'rgba(0,163,224,0.45)'; ctx.lineWidth = 1; ctx.stroke();

                ctx.fillStyle = node.hub ? 'rgba(210,120,15,0.96)' : 'rgba(4,18,38,0.93)';
                ctx.shadowColor = 'rgba(0,0,0,0.45)'; ctx.shadowBlur = 7;
                roundRect(ctx, labelCX-boxW/2, labelCY-boxH/2, boxW, boxH, 5); ctx.fill(); ctx.shadowBlur = 0;

                ctx.strokeStyle = node.hub ? 'rgba(255,195,70,0.55)' : 'rgba(0,163,224,0.45)'; ctx.lineWidth = 0.8;
                roundRect(ctx, labelCX-boxW/2, labelCY-boxH/2, boxW, boxH, 5); ctx.stroke();
                ctx.fillStyle = '#fff'; ctx.fillText(node.label, labelCX, labelCY);
                ctx.restore();
            });
        }

        function drawParticles() {
            particles.forEach(part => {
                const route = ROUTES[part.routeIdx], hub = NODES[route.from], dest = NODES[route.to];
                const pt = slerp(hub.lon, hub.lat, dest.lon, dest.lat, part.t);
                const p = project(pt.lon, pt.lat); if (p.z < 0.05) return;

                for (let ti = 1; ti <= 6; ti++) {
                    const trailT = Math.max(0, part.t-ti*0.01);
                    const tPt = slerp(hub.lon, hub.lat, dest.lon, dest.lat, trailT);
                    const tp = project(tPt.lon, tPt.lat); if (tp.z < 0) continue;
                    ctx.beginPath(); ctx.arc(tp.x, tp.y, Math.max(0.3, 2.2-ti*0.3), 0, Math.PI*2);
                    ctx.fillStyle = `rgba(0,220,255,${0.4-ti*0.06})`; ctx.fill();
                }
                ctx.save();
                const emojiSize = Math.round(19*Math.min(zoomScale,1.3));
                ctx.font = `${emojiSize}px "Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif`;
                ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
                ctx.fillStyle = 'rgba(0,0,0,0.28)';
                ctx.beginPath(); ctx.arc(p.x, p.y, 12, 0, Math.PI*2); ctx.fill();
                ctx.fillText(route.vehicle, p.x, p.y);
                ctx.restore();
            });
        }

        function roundRect(ctx, x, y, w, h, r) {
            ctx.beginPath();
            ctx.moveTo(x+r,y); ctx.lineTo(x+w-r,y); ctx.quadraticCurveTo(x+w,y,x+w,y+r);
            ctx.lineTo(x+w,y+h-r); ctx.quadraticCurveTo(x+w,y+h,x+w-r,y+h);
            ctx.lineTo(x+r,y+h); ctx.quadraticCurveTo(x,y+h,x,y+h-r);
            ctx.lineTo(x,y+r); ctx.quadraticCurveTo(x,y,x+r,y); ctx.closePath();
        }

        function resize() {
            const rect = canvas.parentElement.getBoundingClientRect();
            canvas.width = rect.width; canvas.height = rect.height;
            cx = canvas.width*0.5; cy = canvas.height*0.5;
            R = Math.min(canvas.width, canvas.height)*0.41;
        }

        function loop() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            if (!isDragging) rot += rotSpeed;
            drawGlobe(); drawRoutes(); drawParticles(); drawRiverBoats(); drawNodes();
            particles.forEach(p => { p.t += p.speed; if (p.t > 1) p.t = 0; });
            RIVER_BOATS.forEach(b => { b.t += b.speed; if (b.t > 1) b.t = 0; });
            requestAnimationFrame(loop);
        }

        // ── Interacción drag ──
        canvas.addEventListener('mousedown', e => { isDragging=true; lastX=e.clientX; canvas.style.cursor='grabbing'; });
        window.addEventListener('mouseup', () => { isDragging=false; canvas.style.cursor='grab'; });
        window.addEventListener('mousemove', e => { if (!isDragging) return; rot += (e.clientX-lastX)*0.004; lastX=e.clientX; });
        canvas.addEventListener('touchstart', e => { isDragging=true; lastX=e.touches[0].clientX; }, {passive:true});
        window.addEventListener('touchend', () => { isDragging=false; });
        window.addEventListener('touchmove', e => { if (!isDragging) return; rot += (e.touches[0].clientX-lastX)*0.004; lastX=e.touches[0].clientX; }, {passive:true});

        // ── ZOOM: rueda del mouse ──
        canvas.addEventListener('wheel', e => {
            e.preventDefault();
            const delta = e.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP;
            zoomScale = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoomScale + delta));
        }, { passive: false });

        // ── ZOOM: touch pinch ──
        let lastPinchDist = null;
        canvas.addEventListener('touchstart', e => { if (e.touches.length === 2) lastPinchDist = null; }, {passive:true});
        canvas.addEventListener('touchmove', e => {
            if (e.touches.length !== 2) return;
            const dx = e.touches[0].clientX - e.touches[1].clientX;
            const dy = e.touches[0].clientY - e.touches[1].clientY;
            const dist = Math.sqrt(dx*dx+dy*dy);
            if (lastPinchDist !== null) {
                const ratio = dist / lastPinchDist;
                zoomScale = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoomScale * ratio));
            }
            lastPinchDist = dist;
        }, {passive:true});

        // ── ZOOM: botones ──
        document.getElementById('zoomIn').addEventListener('click', () => { zoomScale = Math.min(ZOOM_MAX, zoomScale+ZOOM_STEP*1.5); });
        document.getElementById('zoomOut').addEventListener('click', () => { zoomScale = Math.max(ZOOM_MIN, zoomScale-ZOOM_STEP*1.5); });
        document.getElementById('zoomReset').addEventListener('click', () => { zoomScale = 1.0; });

        canvas.style.cursor = 'grab';
        resize();
        window.addEventListener('resize', resize);
        loop();
    })();

