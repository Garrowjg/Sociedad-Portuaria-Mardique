(function() {
    const STORAGE_KEY = 'mardique_chat_history';
    const SESSION_KEY = 'mardique_chat_session';
    const MAX_CHARS = 200;
    const WELCOME = '¡Hola! Soy el asistente virtual de Sociedad Portuaria Mardique. ¿En qué puedo ayudarte?';

    /* ---------- Precarga del ícono para que no parpadee el texto alt ---------- */
    const LOGO_SRC = '/images/ChatbotBtn.png';
    (function preloadLogo() {
        const link = document.createElement('link');
        link.rel = 'preload';
        link.as = 'image';
        link.href = LOGO_SRC;
        document.head.appendChild(link);
        const img = new Image();
        img.src = LOGO_SRC;
    })();

    /* ---------- Estilos del ícono del launcher (imagen animada) ---------- */
    (function injectRobotStyles() {
        const style = document.createElement('style');
        style.id = 'cbRobotStyles';
        style.textContent = `
        .cb-robot-img{
            width: 88%; height: 88%; object-fit: contain; display: block;
            border-radius: 50%;
            animation: cbRobotFloat 3s ease-in-out infinite;
            filter: drop-shadow(0 2px 6px rgba(0,0,0,.3));
            transition: transform .2s ease, filter .2s ease;
        }
        .cb-launcher:hover .cb-robot-img{
            animation: cbRobotWiggle .6s ease-in-out infinite;
            filter: drop-shadow(0 3px 8px rgba(240,154,54,.5));
        }
        @keyframes cbRobotFloat{
            0%,100%{ transform: translateY(0); }
            50%{ transform: translateY(-3px); }
        }
        @keyframes cbRobotWiggle{
            0%,100%{ transform: translateY(-2px) rotate(-3deg) scale(1.05); }
            50%{ transform: translateY(-2px) rotate(3deg) scale(1.05); }
        }
        `;
        document.head.appendChild(style);
    })();

    function loadChat() { try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)) || []; } catch(e) { return []; } }
    function saveChat(msgs) { try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(msgs)); } catch(e) {} }

    let sessionId = '';
    function loadSession() { try { sessionId = sessionStorage.getItem(SESSION_KEY) || ''; } catch(e) { sessionId = ''; } }
    function saveSession(id) { if (!id) return; sessionId = id; try { sessionStorage.setItem(SESSION_KEY, id); } catch(e) {} }

    const CONTACT_AREAS = [
        'Gerente Comercial', 'Representante Legal', 'Gerente de Operaciones',
        'Gerencia Administrativa', 'Seguridad', 'Documentación Aduanera',
        'Talento Humano', 'Contabilidad', 'Coordinación de Operaciones',
        'Supervisor Zona Franca', 'Inscripción de Usuarios', 'Asistente Adm. y Compras'
    ];

    const QUICK_ACTIONS = [
        '¿Qué servicios ofrecen?',
        '¿Dónde están ubicados?',
        '¿Qué trámites están disponibles?',
        'Agendar cita o solicitar información'
    ];

    const IDLE_NUDGE_MS = 90000;
    const IDLE_RATING_MS = 60000;

    let FAQS = [];
    let faqLoaded = false;
    let idleTimers = [];
    let ratingSent = false;
    let nudgeSent = false;
    let blockedRepeat = 0;

    /* ---------- Build DOM ---------- */

    const launcher = document.createElement('div');
    launcher.className = 'cb-launcher';
    launcher.id = 'cbLauncher';
    launcher.innerHTML =
        '<svg class="cb-ring" viewBox="0 0 90 90">' +
        '<defs><linearGradient id="cbRingGradient" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0%" stop-color="#f09a36"/><stop offset="100%" stop-color="#00a3e0"/>' +
        '</linearGradient></defs>' +
        '<circle class="cb-ring-track" cx="45" cy="45" r="41"></circle>' +
        '<circle class="cb-ring-fill" id="cbRingFill" cx="45" cy="45" r="41"></circle>' +
        '</svg>' +
        '<button class="cb-button" id="cbButton" aria-label="Abrir chat">' +
        '<img src="' + LOGO_SRC + '" alt="" class="cb-robot-img" id="cbRobot">' +
        '</button>' +
        '<span class="cb-unread" id="cbUnread" style="display:none">1</span>';

    const modal = document.createElement('div');
    modal.className = 'cb-modal';
    modal.id = 'cbModal';
    modal.innerHTML =
        '<div class="cb-modal-accent"></div>' +
        '<div class="cb-header" id="cbHeader">' +
        '<canvas id="cbHeaderCanvas"></canvas>' +
        '<div class="cb-header-content">' +
        '<div class="cb-header-badge"><img src="' + LOGO_SRC + '" alt=""></div>' +
        '<div class="cb-header-text">' +
        '<div class="cb-header-title">Asistente Mardique</div>' +
        '<div class="cb-header-sub"><span class="cb-status-dot"></span>En línea · Respondemos tus dudas</div>' +
        '</div>' +
        '<button class="cb-close" id="cbClose" aria-label="Cerrar chat">&#10005;</button>' +
        '</div>' +
        '</div>' +
        '<div class="cb-messages" id="cbMessages"></div>' +
        '<div class="cb-faq-panel" id="cbFaqPanel">' +
        '<div class="cb-faq-head"><i class="cb-faq-ico">?</i> Preguntas frecuentes</div>' +
        '<div class="cb-faq-list" id="cbFaqList"></div>' +
        '</div>' +
        '<div class="cb-input-area">' +
        '<button class="cb-faq-btn" id="cbFaqBtn" aria-label="Preguntas frecuentes">' +
        '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' +
        '<circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 2-3 4"/><circle cx="12" cy="17.2" r="0.3" fill="currentColor"/></svg>' +
        '</button>' +
        '<input class="cb-input" id="cbInput" placeholder="Escribe tu pregunta..." autocomplete="off" maxlength="' + MAX_CHARS + '">' +
        '<button class="cb-send" id="cbSend" aria-label="Enviar">' +
        '<svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>' +
        '</button>' +
        '</div>' +
        '<div class="cb-counter" id="cbCounter">0 / ' + MAX_CHARS + '</div>';

    document.body.appendChild(launcher);
    document.body.appendChild(modal);
    launcher.style.opacity = '0';

    const btn = document.getElementById('cbButton');
    const ringFill = document.getElementById('cbRingFill');
    const unreadEl = document.getElementById('cbUnread');
    const messagesEl = document.getElementById('cbMessages');
    const inputEl = document.getElementById('cbInput');
    const sendBtn = document.getElementById('cbSend');
    const closeBtn = document.getElementById('cbClose');
    const faqBtn = document.getElementById('cbFaqBtn');
    const faqPanel = document.getElementById('cbFaqPanel');
    const faqList = document.getElementById('cbFaqList');
    const counterEl = document.getElementById('cbCounter');

    /* ---------- Scroll: progress ring ---------- */

    const RING_C = 2 * Math.PI * 41;
    ringFill.style.strokeDasharray = RING_C.toFixed(1);

    function updateScrollProgress() {
        const doc = document.documentElement;
        const max = doc.scrollHeight - doc.clientHeight;
        const pct = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
        ringFill.style.strokeDashoffset = (RING_C * (1 - pct)).toFixed(1);
    }
    window.addEventListener('scroll', updateScrollProgress, { passive: true });
    updateScrollProgress();

    /* ---------- Canvas: animated waves + sailing ship in the chat header ---------- */

    let headerSceneResize = function() {};

    (function headerScene() {
        const canvas = document.getElementById('cbHeaderCanvas');
        const ctx = canvas.getContext('2d');
        let W, H, t = 0, ships = [{ x: -20, speed: 0.05 }, { x: 220, speed: 0.035 }];

        function resize() {
            const rect = canvas.parentElement.getBoundingClientRect();
            if (!rect.width || !rect.height) return;
            W = canvas.width = rect.width;
            H = canvas.height = rect.height;
        }
        resize();
        window.addEventListener('resize', resize);
        headerSceneResize = resize;

        function drawWave(baseY, amp, color, speed) {
            ctx.beginPath();
            ctx.moveTo(0, H);
            for (let x = 0; x <= W; x += 6) {
                const y = baseY + Math.sin((x * 0.045) + t * speed) * amp;
                ctx.lineTo(x, y);
            }
            ctx.lineTo(W, H);
            ctx.closePath();
            ctx.fillStyle = color;
            ctx.fill();
        }

        function drawShip(x, y, scale, alpha) {
            ctx.save();
            ctx.globalAlpha = alpha;
            ctx.translate(x, y);
            ctx.scale(scale, scale);
            ctx.fillStyle = '#0d2b3e';
            ctx.beginPath();
            ctx.moveTo(-14, 4);
            ctx.lineTo(14, 4);
            ctx.lineTo(9, 11);
            ctx.lineTo(-9, 11);
            ctx.closePath();
            ctx.fill();
            ctx.fillRect(-1.4, -11, 2.8, 15);
            ctx.beginPath();
            ctx.moveTo(1.4, -10);
            ctx.lineTo(11, 2);
            ctx.lineTo(1.4, 2);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }

        function draw() {
            t += 0.09;
            ctx.clearRect(0, 0, W, H);

            ships.forEach(function(s) {
                s.x += s.speed;
                if (s.x > W + 30) s.x = -30;
                drawShip(s.x, H * 0.32, 0.55, 0.16);
            });

            drawWave(H * 0.72, 6, 'rgba(0,163,224,0.22)', 0.35);
            drawWave(H * 0.82, 8, 'rgba(238,246,250,0.10)', 0.22);
            drawWave(H * 0.9, 5, 'rgba(240,154,54,0.10)', 0.5);

            requestAnimationFrame(draw);
        }
        draw();
    })();

    /* ---------- FAQs ---------- */

    function loadFaqs() {
        if (faqLoaded) return;
        fetch('/api/chatbot/faqs')
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (Array.isArray(data) && data.length) {
                    FAQS = data;
                    faqLoaded = true;
                    renderFaqList();
                }
            })
            .catch(function() {});
    }

    function renderFaqList() {
        faqList.innerHTML = '';
        FAQS.forEach(function(faq) {
            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'cb-faq-item';
            item.textContent = faq.question;
            item.addEventListener('click', function() {
                toggleFaqPanel(false);
                if (inputEl.value.trim()) {
                    inputEl.value = '';
                    updateCounter();
                }
                sendMessage(faq.question);
            });
            faqList.appendChild(item);
        });
    }

    function toggleFaqPanel(force) {
        const willOpen = force !== undefined ? force : !faqPanel.classList.contains('open');
        faqPanel.classList.toggle('open', willOpen);
        faqBtn.classList.toggle('active', willOpen);
    }

    /* ---------- Chat logic ---------- */

    function scrollBottom() { messagesEl.scrollTop = messagesEl.scrollHeight; }

    function renderMarkdown(text) {
        var html = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.+?)\*/g, '<em>$1</em>')
            .replace(/`(.+?)`/g, '<code>$1</code>')
            .replace(/\n/g, '<br>');
        return html;
    }

    function robotAvatarHtml() {
        return '<div class="cb-avatar"><img src="' + LOGO_SRC + '" alt=""></div>';
    }

    function addBotBubble(text, withChips) {
        const row = document.createElement('div');
        row.className = 'cb-row';
        row.innerHTML = robotAvatarHtml() +
            '<div class="cb-msg bot"></div>';
        row.querySelector('.cb-msg').innerHTML = renderMarkdown(text);
        messagesEl.appendChild(row);

        if (withChips) {
            const chips = document.createElement('div');
            chips.className = 'cb-chips';
            QUICK_ACTIONS.forEach(function(q) {
                const chip = document.createElement('button');
                chip.className = 'cb-chip';
                chip.type = 'button';
                chip.textContent = q;
                chip.addEventListener('click', function() { sendMessage(q); });
                chips.appendChild(chip);
            });
            messagesEl.appendChild(chips);
        }
        scrollBottom();
    }

    function addUserBubble(text) {
        const row = document.createElement('div');
        row.className = 'cb-row user';
        row.innerHTML = '<div class="cb-msg user"></div>';
        row.querySelector('.cb-msg').textContent = text;
        messagesEl.appendChild(row);
        scrollBottom();
    }

    function showTyping() {
        const row = document.createElement('div');
        row.className = 'cb-row';
        row.id = 'cbTypingRow';
        row.innerHTML = robotAvatarHtml() +
            '<div class="cb-typing"><span></span><span></span><span></span></div>';
        messagesEl.appendChild(row);
        scrollBottom();
    }
    function hideTyping() {
        const el = document.getElementById('cbTypingRow');
        if (el) el.remove();
    }

    function restoreHistory() {
        const msgs = loadChat();
        messagesEl.innerHTML = '';
        // Siempre se muestra el saludo + preguntas rápidas, sin duplicarlos
        addBotBubble(WELCOME, true);
        msgs.forEach(function(m) {
            if (m.type === 'bot' && m.text === WELCOME) return;
            if (m.type === 'bot') {
                addBotBubble(m.text);
                if (m.form) addContactForm(m);
            }
            else addUserBubble(m.text);
        });
        if (msgs.length === 0) {
            saveChat([{ text: WELCOME, type: 'bot' }]);
        }
        scrollBottom();
    }

    /* ---------- Contact form ---------- */

    function buildContactForm(saved) {
        const wrap = document.createElement('div');
        wrap.className = 'cb-form';
        wrap.innerHTML =
            '<div class="cb-form-title">Solicitud de contacto</div>' +
            '<label class="cb-form-label">Nombre completo</label>' +
            '<input class="cb-form-input" id="cfNombre" placeholder="Ej: Juan Pérez" maxlength="100">' +
            '<label class="cb-form-label">Cédula</label>' +
            '<input class="cb-form-input" id="cfCedula" placeholder="Ej: 123456789" maxlength="20">' +
            '<label class="cb-form-label">Correo</label>' +
            '<input class="cb-form-input" id="cfCorreo" type="email" placeholder="correo@ejemplo.com" maxlength="120">' +
            '<label class="cb-form-label">Teléfono</label>' +
            '<input class="cb-form-input" id="cfTelefono" placeholder="Ej: 3001234567" maxlength="20">' +
            '<label class="cb-form-label">Área encargada</label>' +
            '<select class="cb-form-input" id="cfArea">' +
            CONTACT_AREAS.map(function(a) { return '<option value="' + a + '">' + a + '</option>'; }).join('') +
            '</select>' +
            '<div class="cb-form-actions">' +
            '<button type="button" class="cb-form-btn primary" data-tipo="AGENDAR CITA">Agendar cita</button>' +
            '<button type="button" class="cb-form-btn" data-tipo="AGENDAR REUNIÓN">Agendar reunión</button>' +
            '<button type="button" class="cb-form-btn" data-tipo="SOLICITAR INFORMACIÓN">Solicitar información</button>' +
            '</div>' +
            '<div class="cb-form-status" id="cfStatus"></div>';

        // Si ya fue enviado antes (se restaura del historial), mostrarlo en estado completado
        if (saved && saved.submitted) {
            const statusEl = wrap.querySelector('#cfStatus');
            statusEl.textContent = '✓ ' + (saved.submittedMsg || 'Tu solicitud fue enviada. Un representante te contactará pronto.');
            statusEl.className = 'cb-form-status success';
            wrap.querySelectorAll('input,select,.cb-form-btn').forEach(function(el) { el.disabled = true; });
        }

        const send = function(tipo) {
            const nombre = wrap.querySelector('#cfNombre').value.trim();
            const cedula = wrap.querySelector('#cfCedula').value.trim();
            const correo = wrap.querySelector('#cfCorreo').value.trim();
            const telefono = wrap.querySelector('#cfTelefono').value.trim();
            const area = wrap.querySelector('#cfArea').value;
            const statusEl = wrap.querySelector('#cfStatus');

            if (!/^[A-Za-zÀ-ÿñÑ\s'.-]{3,120}$/.test(nombre)) {
                statusEl.textContent = 'El nombre solo puede contener letras (mínimo 3).';
                statusEl.className = 'cb-form-status error';
                return;
            }
            if (!/^[0-9]{4,12}$/.test(cedula)) {
                statusEl.textContent = 'La cédula debe contener solo números (4-12 dígitos).';
                statusEl.className = 'cb-form-status error';
                return;
            }
            if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(correo)) {
                statusEl.textContent = 'El correo no tiene un formato válido.';
                statusEl.className = 'cb-form-status error';
                return;
            }
            if (!/^[0-9+\-\s()]{7,20}$/.test(telefono)) {
                statusEl.textContent = 'El teléfono debe tener 7-20 caracteres (números, +, espacios, paréntesis).';
                statusEl.className = 'cb-form-status error';
                return;
            }

            wrap.querySelectorAll('.cb-form-btn').forEach(function(b) { b.disabled = true; });
            statusEl.textContent = 'Enviando...';
            statusEl.className = 'cb-form-status';

            fetch('/api/chatbot/contact', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nombre: nombre, cedula: cedula, correo: correo, telefono: telefono, area: area, tipo: tipo })
            })
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    if (data && data.ok === 'true') {
                        statusEl.textContent = '✓ ' + data.message;
                        statusEl.className = 'cb-form-status success';
                        wrap.querySelectorAll('input,select,.cb-form-btn').forEach(function(el) { el.disabled = true; });
                        // Marca el formulario como enviado en el historial para que al reabrir el chat se conserve
                        try {
                            const ms = JSON.parse(sessionStorage.getItem(STORAGE_KEY)) || [];
                            for (let i = ms.length - 1; i >= 0; i--) {
                                if (ms[i] && ms[i].type === 'bot' && ms[i].form) {
                                    ms[i].submitted = true;
                                    ms[i].submittedMsg = data.message;
                                    break;
                                }
                            }
                            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(ms));
                        } catch (e) {}
                    } else {
                        statusEl.textContent = (data && data.message) || 'No se pudo enviar. Intenta de nuevo.';
                        statusEl.className = 'cb-form-status error';
                        wrap.querySelectorAll('.cb-form-btn').forEach(function(b) { b.disabled = false; });
                    }
                })
                .catch(function() {
                    statusEl.textContent = 'Error de conexión. Intenta de nuevo.';
                    statusEl.className = 'cb-form-status error';
                    wrap.querySelectorAll('.cb-form-btn').forEach(function(b) { b.disabled = false; });
                });
        };

        wrap.querySelectorAll('.cb-form-btn').forEach(function(b) {
            b.addEventListener('click', function() { send(b.getAttribute('data-tipo')); });
        });

        return wrap;
    }

    function addContactForm(saved) {
        const row = document.createElement('div');
        row.className = 'cb-row';
        row.id = 'cbContactRow';
        row.innerHTML = robotAvatarHtml() + '<div class="cb-form-wrap"></div>';
        row.querySelector('.cb-form-wrap').appendChild(buildContactForm(saved));
        messagesEl.appendChild(row);
        scrollBottom();
    }

    /* ---------- Rating ---------- */

    function addRatingPrompt() {
        if (ratingSent) return;
        ratingSent = true;

        const msg = '¿Cómo calificarías la atención que recibiste? **1** (muy mala) a **5** (excelente).';
        const row = document.createElement('div');
        row.className = 'cb-row';
        row.id = 'cbRatingRow';
        row.innerHTML = robotAvatarHtml() + '<div class="cb-rating"></div>';
        const ratingBox = row.querySelector('.cb-rating');

        const text = document.createElement('div');
        text.className = 'cb-rating-text';
        text.innerHTML = renderMarkdown(msg);
        ratingBox.appendChild(text);

        const stars = document.createElement('div');
        stars.className = 'cb-rating-stars';
        for (let i = 1; i <= 5; i++) {
            const star = document.createElement('button');
            star.type = 'button';
            star.className = 'cb-star';
            star.dataset.value = i;
            star.textContent = '★';
            star.title = i + ' estrellas';
            star.addEventListener('click', function() { submitRating(i); });
            stars.appendChild(star);
        }
        ratingBox.appendChild(stars);

        const commentBox = document.createElement('div');
        commentBox.className = 'cb-rating-comment';
        commentBox.innerHTML =
            '<textarea class="cb-rating-input" placeholder="¿Algo que nos ayude a mejorar? (opcional)" maxlength="500"></textarea>' +
            '<button type="button" class="cb-rating-send" style="display:none;">Enviar comentario</button>';
        ratingBox.appendChild(commentBox);

        const input = commentBox.querySelector('.cb-rating-input');
        const sendCommentBtn = commentBox.querySelector('.cb-rating-send');

        function submitRating(value) {
            ratingBox.querySelectorAll('.cb-star').forEach(function(s) {
                s.classList.toggle('active', parseInt(s.dataset.value) <= value);
            });
            input.style.display = '';
            sendCommentBtn.style.display = '';

            fetch('/api/chatbot/rating', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rating: String(value), comment: input.value.trim() })
            })
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    const thanks = document.createElement('div');
                    thanks.className = 'cb-rating-thanks';
                    thanks.textContent = (data && data.message) || '¡Gracias por tu calificación!';
                    ratingBox.appendChild(thanks);
                    sendCommentBtn.remove();
                    input.remove();
                })
                .catch(function() {
                    const thanks = document.createElement('div');
                    thanks.className = 'cb-rating-thanks';
                    thanks.textContent = '¡Gracias por tu calificación!';
                    ratingBox.appendChild(thanks);
                    sendCommentBtn.remove();
                    input.remove();
                });
        }

        sendCommentBtn.addEventListener('click', function() {
            submitRating(parseInt(ratingBox.querySelector('.cb-star.active').dataset.value));
        });

        messagesEl.appendChild(row);
        scrollBottom();
    }

    /* ---------- Idle timers ---------- */

    function clearIdleTimers() {
        idleTimers.forEach(function(t) { clearTimeout(t); });
        idleTimers = [];
    }

    function scheduleIdleNudge() {
        clearIdleTimers();
        idleTimers.push(setTimeout(function() {
            if (!modal.classList.contains('open')) return;
            if (nudgeSent) return;
            nudgeSent = true;
            addBotBubble('¿Sigues ahí? ¿Hay algo más en lo que pueda ayudarte?');
            scheduleIdleRating();
        }, IDLE_NUDGE_MS));
    }

    function scheduleIdleRating() {
        idleTimers.push(setTimeout(function() {
            if (!modal.classList.contains('open')) return;
            addRatingPrompt();
        }, IDLE_RATING_MS));
    }

    function resetIdleTimer() {
        if (!modal.classList.contains('open')) return;
        clearIdleTimers();
        scheduleIdleNudge();
    }

    /* ---------- Send / receive ---------- */

    function sendMessage(question) {
        let msgs = loadChat();
        addUserBubble(question);
        msgs.push({ text: question, type: 'user' });
        saveChat(msgs);

        showTyping();
        sendBtn.disabled = true;
        inputEl.disabled = true;

        const FORM_DELAY_MS = 2000;
        let answer = '';
        let showForm = false;
        let wasBlocked = false;
        let botRow = null;
        let botMsg = null;
        let done = false;

        function createBotRow() {
            hideTyping();
            const row = document.createElement('div');
            row.className = 'cb-row';
            row.innerHTML = robotAvatarHtml() + '<div class="cb-msg bot"></div>';
            messagesEl.appendChild(row);
            botMsg = row.querySelector('.cb-msg');
            botRow = row;
        }

        function finalize() {
            if (done) return;
            done = true;
            hideTyping();
            if (botMsg) {
                botMsg.innerHTML = renderMarkdown(answer);
            } else if (answer) {
                addBotBubble(answer);
            } else {
                addBotBubble('No obtuve una respuesta. Intenta de nuevo en un momento.');
            }
            scrollBottom();
            msgs = loadChat();
            const botEntry = { text: answer, type: 'bot' };
            if (showForm) botEntry.form = true;
            msgs.push(botEntry);
            saveChat(msgs);
            if (showForm) {
                setTimeout(addContactForm, FORM_DELAY_MS);
            }
            if (wasBlocked) {
                blockedRepeat = Math.min(blockedRepeat + 1, 10);
            } else {
                blockedRepeat = 0;
            }
            sendBtn.disabled = false;
            inputEl.disabled = false;
            inputEl.focus();
            resetIdleTimer();
        }

        function handleSSE(line) {
            const idx = line.indexOf('data:');
            if (idx < 0) return;
            let json;
            try {
                json = JSON.parse(line.slice(idx + 5).trim());
            } catch (e) { return; }
            if (json.token) {
                answer += json.token;
                if (!botMsg) createBotRow();
                botMsg.innerHTML = renderMarkdown(answer);
                scrollBottom();
                if (json.form === true) showForm = true;
                if (json.blocked === true) wasBlocked = true;
            }
            if (json.done === true) finalize();
        }

        const REQUEST_TIMEOUT_MS = 25000;

        function requestJson() {
            const controller = new AbortController();
            const timeoutId = setTimeout(function() { controller.abort(); }, REQUEST_TIMEOUT_MS);
            return fetch('/api/chatbot/ask', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question: question, repeatCount: blockedRepeat, sessionId: sessionId }),
                signal: controller.signal
            })
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    clearTimeout(timeoutId);
                    answer = data.answer || 'Lo siento, no pude procesar tu consulta.';
                    showForm = data.form === true;
                    wasBlocked = data.blocked === true;
                    if (data.sessionId) saveSession(data.sessionId);
                    finalize();
                })
                .catch(function(err) {
                    clearTimeout(timeoutId);
                    throw err;
                });
        }

        const streamController = new AbortController();
        const streamTimeoutId = setTimeout(function() { streamController.abort(); }, REQUEST_TIMEOUT_MS);

        fetch('/api/chatbot/ask/stream', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question: question, repeatCount: blockedRepeat, sessionId: sessionId }),
            signal: streamController.signal
        })
            .then(function(response) {
                if (!response.ok || !response.body) {
                    throw new Error('stream not available');
                }
                const sid = response.headers.get('X-Session-Id');
                if (sid) saveSession(sid);
                var reader = response.body.getReader();
                var decoder = new TextDecoder('utf-8');
                var buffer = '';

                function pump() {
                    return reader.read().then(function(result) {
                        clearTimeout(streamTimeoutId);
                        if (result.done) {
                            finalize();
                            return;
                        }
                        buffer += decoder.decode(result.value, { stream: true });
                        var lines = buffer.split('\n');
                        buffer = lines.pop();
                        for (var i = 0; i < lines.length; i++) {
                            var l = lines[i].trim();
                            if (l) handleSSE(l);
                            if (done) return;
                        }
                        return pump();
                    });
                }
                return pump();
            })
            .catch(function() {
                clearTimeout(streamTimeoutId);
                if (done) return;
                // Si el streaming ya había traído texto parcial, no se vuelve a pedir
                // todo de cero (evita una segunda respuesta distinta pisando la primera).
                // Solo se reintenta con la ruta JSON si no llegó ningún token todavía.
                if (botMsg) {
                    finalize();
                    return;
                }
                requestJson().catch(function() {
                    answer = 'Error de conexión. Intenta de nuevo.';
                    finalize();
                });
            });
    }

    /* ---------- Counter ---------- */

    function updateCounter() {
        const len = inputEl.value.length;
        counterEl.textContent = len + ' / ' + MAX_CHARS;
        counterEl.classList.toggle('near-limit', len >= MAX_CHARS);
    }

    /* ---------- Launcher animations ---------- */

    function hideLauncher() {
        launcher.classList.remove('cb-entering');
        launcher.classList.add('cb-leaving');
        unreadEl.style.display = 'none';
    }

    function showLauncher() {
        launcher.classList.remove('cb-leaving');
        void launcher.offsetWidth;
        launcher.classList.add('cb-entering');
    }

    launcher.addEventListener('animationend', function(e) {
        if (e.animationName === 'cbLauncherIn') {
            launcher.classList.remove('cb-entering');
        }
    });

    /* ---------- Events ---------- */

    function openModal() {
        loadSession();
        loadFaqs();
        modal.classList.remove('closing');
        modal.classList.add('open');
        hideLauncher();
        restoreHistory();
        inputEl.focus();
        requestAnimationFrame(function() { headerSceneResize(); });
        nudgeSent = false;
        ratingSent = false;
        blockedRepeat = 0;
        resetIdleTimer();
    }

    function closeModal() {
        if (!modal.classList.contains('open')) return;
        modal.classList.remove('open');
        modal.classList.add('closing');
        showLauncher();
        toggleFaqPanel(false);
        clearIdleTimers();
        modal.addEventListener('animationend', function handler() {
            modal.classList.remove('closing');
            modal.removeEventListener('animationend', handler);
        }, { once: true });
    }

    btn.addEventListener('click', openModal);

    closeBtn.addEventListener('click', closeModal);

    faqBtn.addEventListener('click', function() {
        if (!faqLoaded && FAQS.length === 0) {
            loadFaqs();
        }
        toggleFaqPanel();
        if (faqPanel.classList.contains('open') && faqList.children.length === 0 && FAQS.length) {
            renderFaqList();
        }
    });

    sendBtn.addEventListener('click', function() {
        const text = inputEl.value.trim();
        if (!text) return;
        inputEl.value = '';
        updateCounter();
        toggleFaqPanel(false);
        resetIdleTimer();
        sendMessage(text);
    });

    inputEl.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendBtn.click();
        }
    });

    inputEl.addEventListener('input', updateCounter);

    inputEl.addEventListener('focus', resetIdleTimer);

    modal.addEventListener('click', function(e) {
        if (e.target === modal) closeModal();
    });

    window.addEventListener('beforeunload', function() {
        sessionStorage.removeItem(STORAGE_KEY);
        sessionStorage.removeItem(SESSION_KEY);
    });

    loadFaqs();

    setTimeout(function() {
        launcher.style.opacity = '';
        launcher.classList.add('cb-entering');
    }, 600);
})();