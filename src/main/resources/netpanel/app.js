// ===== NetPanel Frontend =====
(function () {
    'use strict';

    const state = {
        connected: false,
        eventSource: null,
        logs: {
            console: [],
            chat: []
        },
        panels: {
            console: true,
            chat: true,
            system: true
        }
    };

    const dom = {};

    function init() {
        cacheDom();
        bindEvents();
        startClock();
        loadInitialData();
        connectSSE();
    }

    function cacheDom() {
        dom.consoleLog = document.getElementById('console-log');
        dom.chatLog = document.getElementById('chat-log');
        dom.chatInput = document.getElementById('chat-input');
        dom.chatSendBtn = document.getElementById('chat-send-btn');
        dom.statusText = document.getElementById('status-text');
        dom.fpsDisplay = document.getElementById('fps-display');
        dom.memDisplay = document.getElementById('mem-display');
        dom.cpuDisplay = document.getElementById('cpu-display');
        dom.clockEl = document.getElementById('clock');
        dom.consoleCount = document.getElementById('console-count');
        dom.chatCount = document.getElementById('chat-count');
        dom.panelConsole = document.getElementById('panel-console');
        dom.panelChat = document.getElementById('panel-chat');
        dom.panelSystem = document.getElementById('panel-system');
        dom.systemInfo = document.getElementById('system-info');
    }

    function bindEvents() {
        dom.chatSendBtn.addEventListener('click', sendChat);
        dom.chatInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') sendChat();
        });

        document.addEventListener('keydown', function (e) {
            if (e.target.tagName === 'INPUT') return;
            switch (e.key) {
                case 'F1': e.preventDefault(); togglePanel('console'); break;
                case 'F2': e.preventDefault(); togglePanel('chat'); break;
                case 'F3': e.preventDefault(); togglePanel('system'); break;
                case 'Escape': clearAllLogs(); break;
            }
        });

        // Context menu for copying messages
        document.addEventListener('contextmenu', function (e) {
            const logEntry = e.target.closest('.log-entry');
            if (logEntry) {
                e.preventDefault();
                showContextMenu(e.clientX, e.clientY, logEntry);
            }
        });

        // Close context menu on click elsewhere
        document.addEventListener('click', function () {
            hideContextMenu();
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') hideContextMenu();
        });
    }

    function showContextMenu(x, y, element) {
        hideContextMenu();

        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.id = 'context-menu';

        const copyItem = document.createElement('div');
        copyItem.className = 'context-menu-item';
        copyItem.textContent = '📋 Копировать сообщение';
        copyItem.addEventListener('click', function () {
            const message = element.getAttribute('data-message');
            if (message) {
                navigator.clipboard.writeText(message).then(function () {
                    // Visual feedback
                    copyItem.textContent = '✓ Скопировано!';
                    setTimeout(hideContextMenu, 500);
                }).catch(function () {
                    // Fallback for older browsers
                    const textarea = document.createElement('textarea');
                    textarea.value = message;
                    document.body.appendChild(textarea);
                    textarea.select();
                    document.execCommand('copy');
                    document.body.removeChild(textarea);
                    copyItem.textContent = '✓ Скопировано!';
                    setTimeout(hideContextMenu, 500);
                });
            }
        });

        menu.appendChild(copyItem);
        document.body.appendChild(menu);

        // Position menu
        const rect = menu.getBoundingClientRect();
        let posX = x;
        let posY = y;

        if (x + rect.width > window.innerWidth) {
            posX = x - rect.width;
        }
        if (y + rect.height > window.innerHeight) {
            posY = y - rect.height;
        }

        menu.style.left = posX + 'px';
        menu.style.top = posY + 'px';
    }

    function hideContextMenu() {
        const menu = document.getElementById('context-menu');
        if (menu) menu.remove();
    }

    function startClock() {
        function update() {
            const now = new Date();
            dom.clockEl.textContent = now.toLocaleTimeString('ru-RU', { hour12: false });
        }
        update();
        setInterval(update, 1000);
    }

    function togglePanel(name) {
        state.panels[name] = !state.panels[name];
        const panel = dom['panel' + capitalize(name)];
        if (panel) panel.classList.toggle('hidden', !state.panels[name]);
    }

    function capitalize(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

    function clearAllLogs() {
        state.logs.console = [];
        state.logs.chat = [];
        renderAll();
    }

    // ===== SSE =====
    function connectSSE() {
        if (state.eventSource) state.eventSource.close();

        state.eventSource = new EventSource('/api/stream');

        state.eventSource.addEventListener('console', function (e) {
            try {
                state.logs.console = JSON.parse(e.data);
                renderLog(dom.consoleLog, state.logs.console);
                dom.consoleCount.textContent = state.logs.console.length;
            } catch (err) {}
        });

        state.eventSource.addEventListener('chat', function (e) {
            try {
                state.logs.chat = JSON.parse(e.data);
                renderLog(dom.chatLog, state.logs.chat);
                dom.chatCount.textContent = state.logs.chat.length;
            } catch (err) {}
        });

        state.eventSource.addEventListener('system', function (e) {
            try {
                const data = JSON.parse(e.data);
                updateSystemInfo(data);
            } catch (err) {}
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

    // ===== INITIAL DATA =====
    async function loadInitialData() {
        try {
            const [consoleData, chatData, status] = await Promise.all([
                fetchJSON('/api/console?limit=200'),
                fetchJSON('/api/chat'),
                fetchJSON('/api/status')
            ]);

            state.logs.console = consoleData || [];
            state.logs.chat = chatData || [];

            renderAll();

            if (status) {
                updateSystemInfo(status);
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

    // ===== SYSTEM INFO =====
    function updateSystemInfo(data) {
        // FPS
        const fps = data.fps || data.smoothedFps || 0;
        const fpsClass = fps >= 60 ? 'val' : fps >= 30 ? 'val' : 'val low';
        dom.fpsDisplay.innerHTML = 'FPS: <span class="' + fpsClass + '">' + fps + '</span>';

        // System info
        const sys = data.system;
        if (sys) {
            const memUsed = sys.usedMemory || 0;
            const memMax = sys.maxMemory || 1;
            const memPct = sys.memoryPercent || 0;
            const cpuPct = sys.cpuPercent != null ? sys.cpuPercent : -1;
            const sysCpuPct = sys.systemCpuPercent != null ? sys.systemCpuPercent : -1;

            dom.memDisplay.innerHTML = 'MEM: <span class="val">' + memUsed + ' / ' + sys.totalMemory + ' MB</span>';

            if (cpuPct >= 0) {
                dom.cpuDisplay.innerHTML = 'CPU: <span class="val">' + cpuPct + '%</span>';
            } else {
                dom.cpuDisplay.innerHTML = 'CPU: <span class="val">N/A</span>';
            }

            // Build system panel HTML
            let html = '';

            // Memory section
            html += '<div class="sys-section-title">Memory</div>';
            html += '<div class="sys-bar-container">' +
                '<div class="sys-bar-label"><span>Heap</span><span>' + (sys.heapUsed || 0) + ' / ' + (sys.heapMax || 0) + ' MB</span></div>' +
                '<div class="sys-bar"><div class="sys-bar-fill memory" style="width:' + Math.min((sys.heapUsed || 0) / (sys.heapMax || 1) * 100, 100) + '%"></div></div>' +
            '</div>';
            html += '<div class="sys-bar-container">' +
                '<div class="sys-bar-label"><span>Non-Heap</span><span>' + (sys.nonHeapUsed || 0) + ' MB</span></div>' +
                '<div class="sys-bar"><div class="sys-bar-fill nonheap" style="width:' + Math.min((sys.nonHeapUsed || 0) / Math.max((sys.nonHeapMax || 1), 1) * 100, 100) + '%"></div></div>' +
            '</div>';

            // CPU section
            html += '<div class="sys-section-title">CPU</div>';
            html += '<div class="sys-bar-container">' +
                '<div class="sys-bar-label"><span>Process</span><span>' + (cpuPct >= 0 ? cpuPct + '%' : 'N/A') + '</span></div>' +
                '<div class="sys-bar"><div class="sys-bar-fill cpu" style="width:' + (cpuPct >= 0 ? Math.min(cpuPct, 100) : 0) + '%"></div></div>' +
            '</div>';
            if (sysCpuPct >= 0) {
                html += '<div class="sys-bar-container">' +
                    '<div class="sys-bar-label"><span>System</span><span>' + sysCpuPct + '%</span></div>' +
                    '<div class="sys-bar"><div class="sys-bar-fill syscpu" style="width:' + Math.min(sysCpuPct, 100) + '%"></div></div>' +
                '</div>';
            }
            html += '<div class="sys-row"><span class="sys-label">Cores</span><span class="sys-value">' + (sys.availableProcessors || '?') + '</span></div>';

            // Uptime
            html += '<div class="sys-section-title">Runtime</div>';
            html += '<div class="sys-row"><span class="sys-label">Uptime</span><span class="sys-value">' + (sys.uptime || 'N/A') + '</span></div>';
            html += '<div class="sys-row"><span class="sys-label">Threads</span><span class="sys-value">' + (sys.threadCount || '?') + '</span></div>';

            // GC info
            if (sys.gc && sys.gc.length > 0) {
                html += '<div class="sys-section-title">Garbage Collector</div>';
                for (let i = 0; i < sys.gc.length; i++) {
                    const gc = sys.gc[i];
                    html += '<div class="sys-row"><span class="sys-label">' + escapeHtml(gc.name) + '</span><span class="sys-value">' + gc.collections + ' (' + gc.time + 'ms)</span></div>';
                }
            }

            // Disk space
            if (sys.diskTotal > 0) {
                html += '<div class="sys-section-title">Disk</div>';
                html += '<div class="sys-bar-container">' +
                    '<div class="sys-bar-label"><span>Disk</span><span>' + sys.diskUsed + ' / ' + sys.diskTotal + ' GB</span></div>' +
                    '<div class="sys-bar"><div class="sys-bar-fill disk" style="width:' + Math.min(sys.diskPercent || 0, 100) + '%"></div></div>' +
                '</div>';
                html += '<div class="sys-row"><span class="sys-label">Free</span><span class="sys-value">' + sys.diskFree + ' GB</span></div>';
            }

            // Java & OS info
            html += '<div class="sys-section-title">Environment</div>';
            html += '<div class="sys-row"><span class="sys-label">Java</span><span class="sys-value">' + escapeHtml(sys.javaVersion || 'N/A') + '</span></div>';
            html += '<div class="sys-row"><span class="sys-label">OS</span><span class="sys-value">' + escapeHtml(sys.osName || 'N/A') + ' ' + escapeHtml(sys.osArch || '') + '</span></div>';

            dom.systemInfo.innerHTML = html;
        }
    }

    // ===== RENDERING =====
    function renderAll() {
        renderLog(dom.consoleLog, state.logs.console);
        renderLog(dom.chatLog, state.logs.chat);

        dom.consoleCount.textContent = state.logs.console.length;
        dom.chatCount.textContent = state.logs.chat.length;
    }

    function renderLog(container, entries) {
        if (!entries || entries.length === 0) {
            container.innerHTML = '<div class="log-entry" style="color:var(--text-dim)">No data...</div>';
            return;
        }

        let html = '';
        const start = Math.max(0, entries.length - 700);
        for (let i = start; i < entries.length; i++) {
            const entry = entries[i];
            const time = formatTime(entry.timestamp);
            const level = entry.level || 'INFO';
            const msg = formatMinecraftColors(escapeHtml(entry.message || ''));
            html += '<div class="log-entry" data-message="' + escapeHtml(entry.message || '') + '">' +
                '<span class="log-time">' + time + '</span>' +
                '<span class="log-level ' + level + '">' + level + '</span>' +
                '<span class="log-msg">' + msg + '</span>' +
                '</div>\n';
        }
        container.innerHTML = html;
        container.scrollTop = container.scrollHeight;
    }

    function formatTime(ts) {
        if (!ts) return '--:--:--';
        const d = new Date(ts);
        return d.toLocaleTimeString('ru-RU', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
    }

    function escapeHtml(str) {
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // Minecraft color codes to HTML
    function formatMinecraftColors(text) {
        const colorMap = {
            '0': '#000000', // black
            '1': '#0000AA', // dark_blue
            '2': '#00AA00', // dark_green
            '3': '#00AAAA', // dark_aqua
            '4': '#AA0000', // dark_red
            '5': '#AA00AA', // dark_purple
            '6': '#FFAA00', // gold
            '7': '#AAAAAA', // gray
            '8': '#555555', // dark_gray
            '9': '#5555FF', // blue
            'a': '#55FF55', // green
            'b': '#55FFFF', // aqua
            'c': '#FF5555', // red
            'd': '#FF55FF', // light_purple
            'e': '#FFFF55', // yellow
            'f': '#FFFFFF'  // white
        };

        const formatMap = {
            'l': 'font-weight: bold;',
            'o': 'font-style: italic;',
            'n': 'text-decoration: underline;',
            'm': 'text-decoration: line-through;'
        };

        // Split text by § codes
        const parts = text.split(/(§[0-9a-fk-or])/gi);
        let result = '';
        let currentColor = '';
        let currentFormat = '';

        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];

            if (part.startsWith('§') && part.length === 2) {
                const code = part.charAt(1).toLowerCase();

                if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f') {
                    // Color code
                    currentColor = colorMap[code] || '';
                    currentFormat = '';
                } else if (code === 'r') {
                    // Reset
                    currentColor = '';
                    currentFormat = '';
                } else if (formatMap[code]) {
                    // Format code
                    currentFormat += formatMap[code];
                } else if (code === 'k') {
                    // Obfuscated - skip
                }
            } else if (part) {
                // Regular text
                if (currentColor || currentFormat) {
                    let style = '';
                    if (currentColor) style += 'color: ' + currentColor + ';';
                    if (currentFormat) style += currentFormat;
                    result += '<span style="' + style + '">' + part + '</span>';
                } else {
                    result += part;
                }
            }
        }

        return result;
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
