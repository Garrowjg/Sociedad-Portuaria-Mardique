(function() {
    const STORAGE_KEY = 'mardique_chat_history';

    function loadChat() { try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)) || []; } catch(e) { return []; } }
    function saveChat(msgs) { try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(msgs)); } catch(e) {} }

    const btn = document.createElement('button');
    btn.className = 'cb-button';
    btn.id = 'cbButton';
    btn.setAttribute('aria-label', 'Abrir chat');
    btn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 5.8 2 10.5c0 2.65 1.36 5.02 3.5 6.55V22l4.6-2.76c.92.26 1.9.41 2.9.41 5.52 0 10-3.8 10-8.5S17.52 2 12 2z"/></svg>';

    const modal = document.createElement('div');
    modal.className = 'cb-modal';
    modal.id = 'cbModal';
    modal.innerHTML =
        '<div class="cb-header">' +
            '<div class="cb-header-icon">💬</div>' +
            '<div class="cb-header-text">' +
                '<div class="cb-header-title">Asistente Mardique</div>' +
                '<div class="cb-header-sub">Respondemos tus dudas</div>' +
            '</div>' +
            '<button class="cb-close" id="cbClose" aria-label="Cerrar chat">&times;</button>' +
        '</div>' +
        '<div class="cb-messages" id="cbMessages"></div>' +
        '<div class="cb-input-area">' +
            '<input class="cb-input" id="cbInput" placeholder="Escribe tu pregunta..." autocomplete="off">' +
            '<button class="cb-send" id="cbSend" aria-label="Enviar">' +
                '<svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>' +
            '</button>' +
        '</div>';

    document.body.appendChild(btn);
    document.body.appendChild(modal);

    const messagesEl = document.getElementById('cbMessages');
    const inputEl = document.getElementById('cbInput');
    const sendBtn = document.getElementById('cbSend');
    const closeBtn = document.getElementById('cbClose');

    function scrollBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function addMessage(text, type) {
        const div = document.createElement('div');
        div.className = 'cb-msg ' + type;
        div.textContent = text;
        messagesEl.appendChild(div);
        scrollBottom();
    }

    function showTyping() {
        const div = document.createElement('div');
        div.className = 'cb-typing';
        div.id = 'cbTyping';
        div.innerHTML = '<span></span><span></span><span></span>';
        messagesEl.appendChild(div);
        scrollBottom();
    }

    function hideTyping() {
        const el = document.getElementById('cbTyping');
        if (el) el.remove();
    }

    function restoreHistory() {
        var msgs = loadChat();
        messagesEl.innerHTML = '';
        if (msgs.length === 0) {
            addMessage('¡Hola! Soy el asistente virtual de Sociedad Portuaria Mardique. ¿En qué puedo ayudarte?', 'bot');
            saveChat([{ text: '¡Hola! Soy el asistente virtual de Sociedad Portuaria Mardique. ¿En qué puedo ayudarte?', type: 'bot' }]);
        } else {
            msgs.forEach(function(m) { addMessage(m.text, m.type); });
        }
    }

    function sendMessage(question) {
        var msgs = loadChat();
        addMessage(question, 'user');
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
            var answer = data.answer || 'Lo siento, no pude procesar tu consulta.';
            addMessage(answer, 'bot');
            msgs = loadChat();
            msgs.push({ text: answer, type: 'bot' });
            saveChat(msgs);
        })
        .catch(function() {
            hideTyping();
            var errMsg = 'Error de conexión. Intenta de nuevo.';
            addMessage(errMsg, 'bot');
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

    btn.addEventListener('click', function() {
        modal.classList.add('open');
        btn.style.display = 'none';
        restoreHistory();
        inputEl.focus();
    });

    closeBtn.addEventListener('click', function() {
        modal.classList.remove('open');
        btn.style.display = 'flex';
    });

    sendBtn.addEventListener('click', function() {
        var text = inputEl.value.trim();
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
        if (e.target === modal) {
            modal.classList.remove('open');
            btn.style.display = 'flex';
        }
    });
})();
