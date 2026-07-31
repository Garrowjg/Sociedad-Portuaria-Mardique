(function() {
    const STORAGE_KEY = 'mardique_chat_history';

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

    const QUICK_QUESTIONS = ['¿Qué servicios ofrecen?', '¿Dónde están ubicados?', '¿Cómo los contacto?'];

    /* ---------- Build DOM ---------- */

    const LOGO_SRC = '/images/Chatbot.png';

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
        '<img src="' + LOGO_SRC + '" alt="Asistente Mardique" class="cb-robot-img" id="cbRobot">' +
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
        '<div class="cb-header-badge"><img src="' + LOGO_SRC + '" alt="Asistente Mardique"></div>' +
        '<div class="cb-header-text">' +
        '<div class="cb-header-title">Asistente Mardique</div>' +
        '<div class="cb-header-sub"><span class="cb-status-dot"></span>En línea · Respondemos tus dudas</div>' +
        '</div>' +
        '<button class="cb-close" id="cbClose" aria-label="Cerrar chat">&#10005;</button>' +
        '</div>' +
        '</div>' +
        '<div class="cb-messages" id="cbMessages"></div>' +
        '<div class="cb-input-area">' +
        '<input class="cb-input" id="cbInput" placeholder="Escribe tu pregunta..." autocomplete="off">' +
        '<button class="cb-send" id="cbSend" aria-label="Enviar">' +
        '<svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>' +
        '</button>' +
        '</div>';

    document.body.appendChild(launcher);
    document.body.appendChild(modal);

    const btn = document.getElementById('cbButton');
    const ringFill = document.getElementById('cbRingFill');
    const unreadEl = document.getElementById('cbUnread');
    const messagesEl = document.getElementById('cbMessages');
    const inputEl = document.getElementById('cbInput');
    const sendBtn = document.getElementById('cbSend');
    const closeBtn = document.getElementById('cbClose');

    /* ---------- Scroll: progress ring + header-style hue on the launcher ---------- */

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

            // distant faint ships crossing the header, evoking the homepage trade-route motif
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
            QUICK_QUESTIONS.forEach(function(q) {
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
        if (msgs.length === 0) {
            const welcome = '¡Hola! Soy el asistente virtual de Sociedad Portuaria Mardique. ¿En qué puedo ayudarte?';
            addBotBubble(welcome, true);
            saveChat([{ text: welcome, type: 'bot' }]);
        } else {
            msgs.forEach(function(m) {
                if (m.type === 'bot') addBotBubble(m.text);
                else addUserBubble(m.text);
            });
        }
    }

    function sendMessage(question) {
        let msgs = loadChat();
        addUserBubble(question);
        msgs.push({ text: question, type: 'user' });
        saveChat(msgs);

        showTyping();
        sendBtn.disabled = true;
        inputEl.disabled = true;

        fetch('/api/chatbot/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question: question })
        })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                hideTyping();
                const answer = data.answer || 'Lo siento, no pude procesar tu consulta.';
                addBotBubble(answer);
                msgs = loadChat();
                msgs.push({ text: answer, type: 'bot' });
                saveChat(msgs);
            })
            .catch(function() {
                hideTyping();
                const errMsg = 'Error de conexión. Intenta de nuevo.';
                addBotBubble(errMsg);
                msgs = loadChat();
                msgs.push({ text: errMsg, type: 'bot' });
                saveChat(msgs);
            })
            .finally(function() {
                sendBtn.disabled = false;
                inputEl.disabled = false;
                inputEl.focus();
            });
    }

    /* ---------- Events ---------- */

    function openModal() {
        modal.classList.remove('closing');
        modal.classList.add('open');
        launcher.style.display = 'none';
        unreadEl.style.display = 'none';
        restoreHistory();
        inputEl.focus();
        requestAnimationFrame(function() { headerSceneResize(); });
    }

    function closeModal() {
        if (!modal.classList.contains('open')) return;
        modal.classList.remove('open');
        modal.classList.add('closing');
        launcher.style.display = 'flex';
        modal.addEventListener('animationend', function handler() {
            modal.classList.remove('closing');
            modal.removeEventListener('animationend', handler);
        }, { once: true });
    }

    btn.addEventListener('click', openModal);

    closeBtn.addEventListener('click', closeModal);

    sendBtn.addEventListener('click', function() {
        const text = inputEl.value.trim();
        if (!text) return;
        inputEl.value = '';
        sendMessage(text);
    });

    inputEl.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendBtn.click();
        }
    });

    modal.addEventListener('click', function(e) {
        if (e.target === modal) closeModal();
    });

    window.addEventListener('beforeunload', function() {
        sessionStorage.removeItem(STORAGE_KEY);
    });
})();