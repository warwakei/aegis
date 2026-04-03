// ===== NetPanel Frontend =====
(function () {
    'use strict';

    // State
    const state = {
        connected: false,
        eventSource: null,
        logs: {
            console: [],
            hitreg: [],
            packets: [],
            chat: []
        },
        maxEntries: 500,
        panels: {
            console: true,
            hitreg: true,
            chat: true,
            packets: true
        }
    };

    // DOM refs
    const dom = {};

    // ===== INIT =====
    function init() {
        cacheDom();
        bindEvents();
        startClock();
        loadInitialData();
        connectSSE();
    }

    function cacheDom() {
        dom.consoleLog = document.getElementById('console-log');
        dom.hitregLog = document.getElementById('hitreg-log');
        dom.packetsLog = document.getElementById('packets-log');
        dom.chatLog = document.getElementById('chat-log');
        dom.chatInput = document.getElementById('chat-input');
        dom.chatSendBtn = document.getElementById('chat-send-btn');
        dom.statusText = document.getElementById('status-text');
        dom.fpsDisplay = document.getElementById('fps-display');
        dom.clockEl = document.getElementById('clock');
        dom.consoleCount = document.getElementById('console-count');
        dom.hitregCount = document.getElementById('hitreg-count');
        dom.packetsCount = document.getElementById('packets-count');
        dom.chatCount = document.getElementById('chat-count');
        dom.panelConsole = document.getElementById('panel-console');
        dom.panelHitreg = document.getElementById('panel-hitreg');
        dom.panelChat = document.getElementById('panel-chat');
        dom.panelPackets = document.getElementById('panel-packets');
    }

    function bindEvents() {
        dom.chatSendBtn.addEventListener('click', sendChat);
        dom.chatInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') sendChat();
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', function (e) {
            if (e.target.tagName === 'INPUT') return;
            switch (e.key) {
                case 'F1': e.preventDefault(); togglePanel('console'); break;
                case 'F2': e.preventDefault(); togglePanel('hitreg'); break;
                case 'F3': e.preventDefault(); togglePanel('chat'); break;
                case 'F4': e.preventDefault(); togglePanel('packets'); break;
                case 'Escape': clearFocusedPanel(); break;
            }
        });
    }

    // ===== CLOCK =====
    function startClock() {
        function update() {
            const now = new Date();
            dom.clockEl.textContent = now.toLocaleTimeString('ru-RU', { hour12: false });
        }
        update();
        setInterval(update, 1000);
    }

    // ===== PANELS =====
    function togglePanel(name) {
        state.panels[name] = !state.panels[name];
        const panel = dom['panel' + capitalize(name)];
        if (panel) {
            panel.classList.toggle('hidden', !state.panels[name]);
        }
    }

    function capitalize(s) {
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    function clearFocusedPanel() {
        // Clear all logs
        state.logs.console = [];
        state.logs.hitreg = [];
        state.logs.packets = [];
        state.logs.chat = [];
        renderAll();
    }

    // ===== SSE CONNECTION =====
    function connectSSE() {
        if (state.eventSource) {
            state.eventSource.close();
        }

        state.eventSource = new EventSource('/api/stream');

        state.eventSource.addEventListener('console', function (e) {
            try {
                const data = JSON.parse(e.data);
                state.logs.console = data;
                renderLog(dom.consoleLog, data);
                dom.consoleCount.textContent = data.length;
            } catch (err) { }
        });

        state.eventSource.addEventListener('hitreg', function (e) {
            try {
                const data = JSON.parse(e.data);
                state.logs.hitreg = data;
                renderLog(dom.hitregLog, data);
                dom.hitregCount.textContent = data.length;
            } catch (err) { }
        });

        state.eventSource.addEventListener('packets', function (e) {
            try {
                const data = JSON.parse(e.data);
                state.logs.packets = data;
                renderLog(dom.packetsLog, data);
                dom.packetsCount.textContent = data.length;
            } catch (err) { }
        });

        state.eventSource.addEventListener('chat', function (e) {
            try {
                const data = JSON.parse(e.data);
                state.logs.chat = data;
                renderLog(dom.chatLog, data);
                dom.chatCount.textContent = data.length;
            } catch (err) { }
        });

        state.eventSource.addEventListener('heartbeat', function () {
            if (!state.connected) {
                state.connected = true;
                dom.statusText.textContent = '● Connected';
                dom.statusText.className = 'status connected';
            }
        });

        state.eventSource.onerror = function () {
            state.connected = false;
            dom.statusText.textContent = '● Disconnected';
            dom.statusText.className = 'status disconnected';
        };
    }

    // ===== INITIAL DATA LOAD =====
    async function loadInitialData() {
        try {
            const [consoleData, hitregData, packetsData, chatData, status] = await Promise.all([
                fetchJSON('/api/console?limit=100'),
                fetchJSON('/api/hitreg'),
                fetchJSON('/api/packets'),
                fetchJSON('/api/chat'),
                fetchJSON('/api/status')
            ]);

            state.logs.console = consoleData || [];
            state.logs.hitreg = hitregData || [];
            state.logs.packets = packetsData || [];
            state.logs.chat = chatData || [];

            renderAll();

            if (status) {
                dom.fpsDisplay.textContent = 'FPS: ' + (status.fps || '--');
            }

            dom.statusText.textContent = '● Connected';
            dom.statusText.className = 'status connected';
            state.connected = true;
        } catch (err) {
            dom.statusText.textContent = '● Error';
            dom.statusText.className = 'status disconnected';
            console.error('Failed to load initial data:', err);
        }
    }

    async function fetchJSON(url) {
        const res = await fetch(url);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
    }

    // ===== RENDERING =====
    function renderAll() {
        renderLog(dom.consoleLog, state.logs.console);
        renderLog(dom.hitregLog, state.logs.hitreg);
        renderLog(dom.packetsLog, state.logs.packets);
        renderLog(dom.chatLog, state.logs.chat);

        dom.consoleCount.textContent = state.logs.console.length;
        dom.hitregCount.textContent = state.logs.hitreg.length;
        dom.packetsCount.textContent = state.logs.packets.length;
        dom.chatCount.textContent = state.logs.chat.length;
    }

    function renderLog(container, entries) {
        if (!entries || entries.length === 0) {
            container.innerHTML = '<div class="log-entry" style="color:var(--text-dim)">No data...</div>';
            return;
        }

        let html = '';
        // Show last 200 entries
        const start = Math.max(0, entries.length - 200);
        for (let i = start; i < entries.length; i++) {
            const entry = entries[i];
            const time = formatTime(entry.timestamp);
            const level = entry.level || 'INFO';
            const msg = entry.message || '';

            // Check if this is a JSON chat message with colors
            if (msg.startsWith('[JSON]')) {
                const jsonMatch = msg.match(/^\[JSON\]\[([^\]]+)\]\s(.*)$/);
                if (jsonMatch) {
                    const sender = escapeHtml(jsonMatch[1]);
                    const jsonStr = jsonMatch[2];
                    const renderedMsg = renderMinecraftJson(jsonStr);
                    html += '<div class="log-entry">' +
                        '<span class="log-time">' + time + '</span>' +
                        '<span class="log-level ' + level + '">' + level + '</span>' +
                        '<span class="log-msg">[' + sender + '] ' + renderedMsg + '</span>' +
                        '</div>\n';
                    continue;
                }
            }

            html += '<div class="log-entry">' +
                '<span class="log-time">' + time + '</span>' +
                '<span class="log-level ' + level + '">' + level + '</span>' +
                '<span class="log-msg">' + escapeHtml(msg) + '</span>' +
                '</div>\n';
        }
        container.innerHTML = html;
        container.scrollTop = container.scrollHeight;
    }

    /**
     * Render Minecraft JSON text with color codes.
     */
    function renderMinecraftJson(jsonStr) {
        try {
            const data = JSON.parse(jsonStr);
            return renderTextComponent(data);
        } catch (e) {
            return escapeHtml(jsonStr);
        }
    }

    function renderTextComponent(comp) {
        if (typeof comp === 'string') {
            return escapeHtml(comp);
        }

        if (comp.text !== undefined) {
            return renderTextComponent(comp.text);
        }

        if (comp.translate !== undefined) {
            return '<i>' + escapeHtml(comp.translate) + '</i>';
        }

        if (comp.score !== undefined) {
            return escapeHtml(comp.score.name + ':' + comp.score.objective);
        }

        let result = '';

        // Render children (extra)
        if (comp.extra && Array.isArray(comp.extra)) {
            for (const child of comp.extra) {
                result += renderTextComponent(child);
            }
        }

        // Get the main text
        let mainText = '';
        if (typeof comp.text === 'string') {
            mainText = comp.text;
        } else if (comp.literal !== undefined) {
            mainText = comp.literal;
        }

        if (mainText) {
            const styles = buildStyle(comp);
            result = '<span style="' + styles + '">' + escapeHtml(mainText) + '</span>' + result;
        }

        return result;
    }

    function buildStyle(comp) {
        let style = '';

        // Color
        const color = comp.color;
        if (color) {
            style += 'color:' + mcColorToCss(color) + ';';
        }

        // Formatting
        if (comp.bold) style += 'font-weight:bold;';
        if (comp.italic) style += 'font-style:italic;';
        if (comp.underlined) style += 'text-decoration:underline;';
        if (comp.strikikethrough) style += 'text-decoration:line-through;';
        if (comp.obfuscated) style += 'opacity:0.5;';

        return style;
    }

    function mcColorToCss(color) {
        if (!color) return '';
        const c = color.toLowerCase();

        // Named Minecraft colors
        const colors = {
            'black': '#000000',
            'dark_blue': '#0000AA',
            'dark_green': '#00AA00',
            'dark_aqua': '#00AAAA',
            'dark_red': '#AA0000',
            'dark_purple': '#AA00AA',
            'gold': '#FFAA00',
            'gray': '#AAAAAA',
            'grey': '#AAAAAA',
            'dark_gray': '#555555',
            'dark_grey': '#555555',
            'blue': '#5555FF',
            'green': '#55FF55',
            'aqua': '#55FFFF',
            'red': '#FF5555',
            'light_purple': '#FF55FF',
            'yellow': '#FFFF55',
            'white': '#FFFFFF'
        };

        if (colors[c]) return colors[c];

        // Hex color (#RRGGBB or #RGB)
        if (c.startsWith('#')) return c;

        // RGB int
        if (!isNaN(c)) {
            const num = parseInt(c);
            if (!isNaN(num)) {
                return '#' + (num & 0xFFFFFF).toString(16).padStart(6, '0');
            }
        }

        return '';
    }

    function formatTime(ts) {
        if (!ts) return '--:--:--';
        const d = new Date(ts);
        return d.toLocaleTimeString('ru-RU', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
    }

    function escapeHtml(str) {
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // ===== CHAT =====
    async function sendChat() {
        const msg = dom.chatInput.value.trim();
        if (!msg) return;

        try {
            const res = await fetch('/api/chat/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: msg })
            });
            const data = await res.json();
            if (data.success) {
                dom.chatInput.value = '';
            }
        } catch (err) {
            console.error('Failed to send chat:', err);
        }
    }

    // ===== START =====
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
