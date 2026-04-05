// ===== NetPanel Frontend =====
(function () {
    'use strict';

    const state = {
        connected: false,
        eventSource: null,
        logs: { console: [], chat: [] },
        panels: { console: false, chat: true, status: true, world: false, potions: false, server: false },
        panelsLocked: false,
        // View settings
        stripChatTag: false,
        shortTime: false,
        stripRecv: false,
        // Context menu
        contextMenuTarget: null
    };

    const dom = {};

    function init() {
        cacheDom();
        bindEvents();
        startClock();
        loadInitialData();
        connectSSE();
        initPanelDragAndResize();
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
        dom.panelStatus = document.getElementById('panel-status');
        dom.panelWorld = document.getElementById('panel-world');
        dom.panelPotions = document.getElementById('panel-potions');
        dom.panelServer = document.getElementById('panel-server');
        dom.systemInfo = document.getElementById('system-info');
        dom.worldInfo = document.getElementById('world-info');
        dom.potionsInfo = document.getElementById('potions-info');
        dom.serverInfo = document.getElementById('server-info');
        dom.menuViewBtn = document.getElementById('menu-view-btn');
        dom.menuView = document.getElementById('menu-view');
        dom.menuStripChat = document.getElementById('menu-strip-chat');
        dom.menuShortTime = document.getElementById('menu-short-time');
        dom.menuStripRecv = document.getElementById('menu-strip-recv');
    }

    function bindEvents() {
        dom.chatSendBtn.addEventListener('click', sendChat);
        dom.chatInput.addEventListener('keydown', function (e) { if (e.key === 'Enter') sendChat(); });

        // Keyboard shortcuts
        document.addEventListener('keydown', function (e) {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
            switch (e.key) {
                case 'F1': e.preventDefault(); togglePanel('console'); break;
                case 'F2': e.preventDefault(); togglePanel('chat'); break;
                case 'F3': e.preventDefault(); togglePanel('status'); break;
                case 'Escape': clearAllLogs(); hideContextMenu(); break;
            }
        });

        // View menu toggle
        dom.menuViewBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            dom.menuView.classList.toggle('active');
            updateMenuCheckmarks();
        });

        // Menu actions
        document.querySelectorAll('.dropdown-item').forEach(function (item) {
            item.addEventListener('click', function () {
                handleMenuAction(item.getAttribute('data-action'));
                dom.menuView.classList.remove('active');
            });
        });

        document.addEventListener('click', function () {
            dom.menuView.classList.remove('active');
            hideContextMenu();
        });

        // Panel controls
        document.querySelectorAll('.panel-ctrl-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var panel = btn.closest('.panel');
                var action = btn.getAttribute('data-action');
                if (action === 'lock') togglePanelLock(panel);
            });
        });

        // Context menu on all log entries
        document.addEventListener('contextmenu', function (e) {
            var logEntry = e.target.closest('.log-entry');
            if (logEntry) {
                e.preventDefault();
                showContextMenu(e.clientX, e.clientY, logEntry);
            } else {
                hideContextMenu();
            }
        });
    }

    // ===== MENU ACTIONS =====
    function handleMenuAction(action) {
        switch (action) {
            case 'toggle-console': togglePanel('console'); break;
            case 'toggle-chat': togglePanel('chat'); break;
            case 'toggle-status': togglePanel('status'); break;
            case 'toggle-world': togglePanel('world'); loadWorldInfo(); break;
            case 'toggle-potions': togglePanel('potions'); loadPotionsInfo(); break;
            case 'toggle-server': togglePanel('server'); loadServerInfo(); break;
            case 'lock-panels': lockAllPanels(); break;
            case 'unlock-panels': unlockAllPanels(); break;
            case 'clear-logs': clearAllLogs(); break;
            case 'strip-chat-tag':
                state.stripChatTag = !state.stripChatTag;
                renderAll();
                break;
            case 'short-time':
                state.shortTime = !state.shortTime;
                renderAll();
                break;
            case 'strip-recv':
                state.stripRecv = !state.stripRecv;
                renderAll();
                break;
        }
        updateMenuCheckmarks();
    }

    function updateMenuCheckmarks() {
        var items = {
            'strip-chat-tag': state.stripChatTag,
            'short-time': state.shortTime,
            'strip-recv': state.stripRecv
        };
        for (var action in items) {
            var el = document.querySelector('[data-action="' + action + '"]');
            if (el) {
                el.textContent = items[action] ? '✅ ' + el.textContent.trim().replace(/^[✅❌]\s*/, '') : '❌ ' + el.textContent.trim().replace(/^[✅❌]\s*/, '');
            }
        }
    }

    // ===== CONTEXT MENU =====
    function showContextMenu(x, y, element) {
        hideContextMenu();
        var menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.id = 'context-menu';

        // Get raw message from data attribute
        var rawMsg = element.getAttribute('data-raw') || element.textContent || '';
        var cleanMsg = element.textContent || '';

        var items = [
            { icon: '📋', label: 'Copy message', action: function () { copyText(cleanMsg); }},
            { icon: '📋', label: 'Copy raw', action: function () { copyText(rawMsg); }},
            { separator: true },
            { icon: '🔍', label: 'Copy sender name', action: function () {
                var match = cleanMsg.match(/\[([^\]]+)\]/);
                if (match) copyText(match[1]);
            }},
            { separator: true },
            { icon: '🧹', label: 'Clear logs', action: function () { clearAllLogs(); }}
        ];

        items.forEach(function (item) {
            if (item.separator) {
                var sep = document.createElement('div');
                sep.className = 'context-menu-separator';
                menu.appendChild(sep);
            } else {
                var el = document.createElement('div');
                el.className = 'context-menu-item';
                el.innerHTML = '<span>' + item.icon + '</span><span>' + item.label + '</span>';
                el.addEventListener('click', function () { item.action(); hideContextMenu(); });
                menu.appendChild(el);
            }
        });

        document.body.appendChild(menu);
        positionContextMenu(menu, x, y);
        state.contextMenuTarget = element;
    }

    function positionContextMenu(menu, x, y) {
        var rect = menu.getBoundingClientRect();
        var posX = Math.min(x, window.innerWidth - rect.width - 10);
        var posY = Math.min(y, window.innerHeight - rect.height - 10);
        menu.style.left = posX + 'px';
        menu.style.top = posY + 'px';
    }

    function hideContextMenu() {
        var menu = document.getElementById('context-menu');
        if (menu) menu.remove();
    }

    function copyText(text) {
        navigator.clipboard.writeText(text).catch(function () {
            var ta = document.createElement('textarea');
            ta.value = text; document.body.appendChild(ta);
            ta.select(); document.execCommand('copy');
            document.body.removeChild(ta);
        });
    }

    // ===== PANEL DRAG & RESIZE =====
    function initPanelDragAndResize() {
        document.querySelectorAll('.panel-draggable').forEach(function (panel) {
            var dragHandle = panel.querySelector('.panel-drag-handle');
            var resizeHandle = panel.querySelector('.panel-resize-handle');
            var isDragging = false, isResizing = false;
            var startX, startY, startLeft, startTop, startWidth, startHeight;

            dragHandle.addEventListener('mousedown', function (e) {
                if (panel.classList.contains('locked') || state.panelsLocked) return;
                isDragging = true;
                panel.classList.add('dragging');
                startX = e.clientX; startY = e.clientY;
                var rect = panel.getBoundingClientRect();
                startLeft = rect.left; startTop = rect.top;
                panel.style.position = 'fixed';
                panel.style.left = startLeft + 'px';
                panel.style.top = startTop + 'px';
                panel.style.width = rect.width + 'px';
                panel.style.height = rect.height + 'px';
                panel.style.zIndex = '100';
                e.preventDefault();
            });

            resizeHandle.addEventListener('mousedown', function (e) {
                if (panel.classList.contains('locked') || state.panelsLocked) return;
                isResizing = true;
                startX = e.clientX; startY = e.clientY;
                var rect = panel.getBoundingClientRect();
                startWidth = rect.width; startHeight = rect.height;
                e.preventDefault();
            });

            document.addEventListener('mousemove', function (e) {
                if (isDragging) {
                    panel.style.left = (startLeft + e.clientX - startX) + 'px';
                    panel.style.top = (startTop + e.clientY - startY) + 'px';
                }
                if (isResizing) {
                    panel.style.width = Math.max(200, startWidth + e.clientX - startX) + 'px';
                    panel.style.height = Math.max(100, startHeight + e.clientY - startY) + 'px';
                }
            });

            document.addEventListener('mouseup', function () {
                if (isDragging) { isDragging = false; panel.classList.remove('dragging'); }
                if (isResizing) isResizing = false;
            });
        });
    }

    function togglePanelLock(panel) {
        var isLocked = panel.classList.toggle('locked');
        var btn = panel.querySelector('[data-action="lock"]');
        if (btn) btn.textContent = isLocked ? '🔒' : '🔓';
    }

    function lockAllPanels() {
        state.panelsLocked = true;
        document.querySelectorAll('.panel-draggable').forEach(function (p) {
            p.classList.add('locked');
            var btn = p.querySelector('[data-action="lock"]');
            if (btn) btn.textContent = '🔒';
        });
    }

    function unlockAllPanels() {
        state.panelsLocked = false;
        document.querySelectorAll('.panel-draggable').forEach(function (p) {
            p.classList.remove('locked');
            var btn = p.querySelector('[data-action="lock"]');
            if (btn) btn.textContent = '🔓';
        });
    }

    // ===== PANELS =====
    function togglePanel(name) {
        state.panels[name] = !state.panels[name];
        var panelMap = {
            console: dom.panelConsole, chat: dom.panelChat, status: dom.panelStatus,
            world: dom.panelWorld, potions: dom.panelPotions, server: dom.panelServer
        };
        var panel = panelMap[name];
        if (panel) panel.classList.toggle('hidden', !state.panels[name]);
    }

    function clearAllLogs() {
        state.logs.console = [];
        state.logs.chat = [];
        renderAll();
    }

    // ===== CLOCK =====
    function startClock() {
        function update() {
            var now = new Date();
            dom.clockEl.textContent = now.toLocaleTimeString('ru-RU', { hour12: false });
        }
        update();
        setInterval(update, 1000);
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
                var data = JSON.parse(e.data);
                updateSystemInfo(data);
                // Auto-refresh panels when visible
                if (state.panels.world) loadWorldInfo();
                if (state.panels.potions) loadPotionsInfo();
                if (state.panels.server) loadServerInfo();
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
            var results = await Promise.all([
                fetchJSON('/api/console?limit=200'),
                fetchJSON('/api/chat'),
                fetchJSON('/api/status')
            ]);
            state.logs.console = results[0] || [];
            state.logs.chat = results[1] || [];
            renderAll();
            if (results[2]) updateSystemInfo(results[2]);
            dom.statusText.textContent = '● Connected';
            dom.statusText.className = 'status connected';
            state.connected = true;
        } catch (err) {
            dom.statusText.textContent = '● Error';
            dom.statusText.className = 'status disconnected';
        }
    }

    async function fetchJSON(url, options) {
        var res = await fetch(url, options || {});
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
    }

    // ===== SYSTEM INFO =====
    function updateSystemInfo(data) {
        var fps = data.fps || 0;
        var tps = data.tps || 0;
        dom.fpsDisplay.innerHTML = 'FPS: <span class="val">' + fps + '</span>';
        var sys = data.system;
        if (!sys) return;
        var cpuPct = sys.cpuPercent != null ? sys.cpuPercent : -1;
        var sysCpuPct = sys.systemCpuPercent != null ? sys.systemCpuPercent : -1;
        dom.memDisplay.innerHTML = 'MEM: <span class="val">' + (sys.usedMemory || 0) + ' / ' + sys.totalMemory + ' MB</span>';
        dom.cpuDisplay.innerHTML = cpuPct >= 0 ? 'CPU: <span class="val">' + cpuPct + '%</span>' : 'CPU: <span class="val">N/A</span>';

        var html = '';
        html += '<div class="sys-row sys-tps"><span class="sys-label">TPS</span><span class="sys-value tps-' + (tps >= 18 ? 'good' : tps >= 15 ? 'warn' : 'bad') + '">' + tps.toFixed(1) + '</span></div>';
        html += '<div class="sys-section-title">Memory</div>';
        html += '<div class="sys-bar-container"><div class="sys-bar-label"><span>Heap</span><span>' + (sys.heapUsed || 0) + ' / ' + (sys.heapMax || 0) + ' MB</span></div><div class="sys-bar"><div class="sys-bar-fill memory" style="width:' + Math.min((sys.heapUsed || 0) / Math.max(sys.heapMax || 1, 1) * 100, 100) + '%"></div></div></div>';
        html += '<div class="sys-bar-container"><div class="sys-bar-label"><span>Non-Heap</span><span>' + (sys.nonHeapUsed || 0) + ' MB</span></div><div class="sys-bar"><div class="sys-bar-fill nonheap" style="width:' + Math.min((sys.nonHeapUsed || 0) / Math.max(sys.nonHeapMax || 1, 1) * 100, 100) + '%"></div></div></div>';
        html += '<div class="sys-section-title">CPU</div>';
        html += '<div class="sys-bar-container"><div class="sys-bar-label"><span>Process</span><span>' + (cpuPct >= 0 ? cpuPct + '%' : 'N/A') + '</span></div><div class="sys-bar"><div class="sys-bar-fill cpu" style="width:' + (cpuPct >= 0 ? Math.min(cpuPct, 100) : 0) + '%"></div></div></div>';
        if (sysCpuPct >= 0) html += '<div class="sys-bar-container"><div class="sys-bar-label"><span>System</span><span>' + sysCpuPct + '%</span></div><div class="sys-bar"><div class="sys-bar-fill syscpu" style="width:' + Math.min(sysCpuPct, 100) + '%"></div></div></div>';
        html += '<div class="sys-row"><span class="sys-label">Cores</span><span class="sys-value">' + (sys.availableProcessors || '?') + '</span></div>';
        html += '<div class="sys-section-title">Runtime</div>';
        html += '<div class="sys-row"><span class="sys-label">Uptime</span><span class="sys-value">' + (sys.uptime || 'N/A') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Threads</span><span class="sys-value">' + (sys.threadCount || '?') + '</span></div>';
        if (sys.diskTotal > 0) {
            html += '<div class="sys-section-title">Disk</div>';
            html += '<div class="sys-bar-container"><div class="sys-bar-label"><span>Disk</span><span>' + sys.diskUsed + ' / ' + sys.diskTotal + ' GB</span></div><div class="sys-bar"><div class="sys-bar-fill disk" style="width:' + Math.min(sys.diskPercent || 0, 100) + '%"></div></div></div>';
            html += '<div class="sys-row"><span class="sys-label">Free</span><span class="sys-value">' + sys.diskFree + ' GB</span></div>';
        }
        html += '<div class="sys-section-title">Environment</div>';
        html += '<div class="sys-row"><span class="sys-label">Java</span><span class="sys-value">' + escapeHtml(sys.javaVersion || 'N/A') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">OS</span><span class="sys-value">' + escapeHtml(sys.osName || 'N/A') + ' ' + escapeHtml(sys.osArch || '') + '</span></div>';
        dom.systemInfo.innerHTML = html;
    }

    // ===== WORLD INFO =====
    async function loadWorldInfo() {
        try {
            var data = await fetchJSON('/api/world');
            renderWorldInfo(data);
        } catch (err) {
            dom.worldInfo.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">⚠️ Failed to load world info</div>';
        }
    }

    function renderWorldInfo(data) {
        if (!data || !data.position) {
            dom.worldInfo.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">Not connected to world</div>';
            return;
        }
        var html = '';
        html += '<div class="sys-section-title">Position</div>';
        html += '<div class="sys-row"><span class="sys-label">X</span><span class="sys-value">' + data.position.x + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Y</span><span class="sys-value">' + data.position.y + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Z</span><span class="sys-value">' + data.position.z + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Yaw</span><span class="sys-value">' + data.position.yaw + '°</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Pitch</span><span class="sys-value">' + data.position.pitch + '°</span></div>';
        html += '<div class="sys-section-title">World</div>';
        html += '<div class="sys-row"><span class="sys-label">Dimension</span><span class="sys-value">' + escapeHtml(data.dimension || 'N/A') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Biome</span><span class="sys-value">' + escapeHtml(data.biome || 'Unknown') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Time</span><span class="sys-value">' + escapeHtml(data.timeOfDay || 'N/A') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Weather</span><span class="sys-value">' + escapeHtml(data.weather || 'Clear') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Seed</span><span class="sys-value">' + data.seed + '</span></div>';
        dom.worldInfo.innerHTML = html;
    }

    // ===== POTIONS INFO =====
    async function loadPotionsInfo() {
        try {
            var data = await fetchJSON('/api/potions');
            renderPotionsInfo(data);
        } catch (err) {
            dom.potionsInfo.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">⚠️ Failed to load potions</div>';
        }
    }

    function renderPotionsInfo(data) {
        if (!data || !data.length) {
            dom.potionsInfo.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">No active effects</div>';
            return;
        }
        var html = '<div class="sys-section-title">Active Effects (' + data.length + ')</div>';
        data.forEach(function (p) {
            var levelText = p.level > 1 ? ' ' + romanNumeral(p.level) : '';
            var colorClass = p.amplifier >= 3 ? 'potion-bad' : p.amplifier >= 1 ? 'potion-neutral' : 'potion-good';
            html += '<div class="potion-item ' + colorClass + '">';
            html += '<span class="potion-name">' + escapeHtml(p.name) + levelText + '</span>';
            html += '<span class="potion-duration">' + p.durationFormatted + '</span>';
            html += '</div>';
        });
        dom.potionsInfo.innerHTML = html;
    }

    function romanNumeral(n) {
        var numerals = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X'];
        return numerals[n] || n;
    }

    // ===== SERVER INFO =====
    async function loadServerInfo() {
        try {
            var data = await fetchJSON('/api/server');
            renderServerInfo(data);
        } catch (err) {
            dom.serverInfo.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">⚠️ Failed to load server info</div>';
        }
    }

    function renderServerInfo(data) {
        if (!data || !data.connected) {
            dom.serverInfo.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">Not connected to server</div>';
            return;
        }
        var tpsClass = data.tps >= 18 ? 'good' : data.tps >= 15 ? 'warn' : 'bad';
        var pingClass = data.ping <= 50 ? 'good' : data.ping <= 150 ? 'warn' : 'bad';
        var html = '';
        html += '<div class="sys-section-title">Server</div>';
        html += '<div class="sys-row"><span class="sys-label">IP</span><span class="sys-value">' + escapeHtml(data.ip || 'N/A') + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Port</span><span class="sys-value">' + (data.port || 25565) + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Brand</span><span class="sys-value">' + escapeHtml(data.brand || 'Vanilla') + '</span></div>';
        html += '<div class="sys-section-title">Performance</div>';
        html += '<div class="sys-row"><span class="sys-label">TPS</span><span class="sys-value tps-' + tpsClass + '">' + data.tps + '</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Ping</span><span class="sys-value ping-' + pingClass + '">' + (data.ping || 0) + ' ms</span></div>';
        html += '<div class="sys-row"><span class="sys-label">Players</span><span class="sys-value">' + data.players + '</span></div>';
        dom.serverInfo.innerHTML = html;
    }

    // ===== RENDERING =====
    function renderAll() {
        renderLog(dom.consoleLog, state.logs.console);
        renderLog(dom.chatLog, state.logs.chat);
        dom.consoleCount.textContent = state.logs.console.length;
        dom.chatCount.textContent = state.logs.chat.length;
    }

    function renderLog(container, entries) {
        if (!entries || !entries.length) {
            container.innerHTML = '<div class="log-entry" style="color:var(--text-dim)">No data...</div>';
            return;
        }
        var html = '';
        var start = Math.max(0, entries.length - 700);
        for (var i = start; i < entries.length; i++) {
            var entry = entries[i];
            var time = formatTime(entry.timestamp);
            var level = entry.level || 'INFO';
            var rawMsg = entry.message || '';
            var msg = processMessage(rawMsg, level);

            // Skip RECV entries if stripRecv is enabled
            if (state.stripRecv && level === 'RECV') continue;
            // Strip [CHAT] tag
            if (state.stripChatTag && level === 'CHAT') {
                level = 'INFO';
            }

            html += '<div class="log-entry" data-raw="' + escapeHtml(rawMsg) + '"><span class="log-time">' + time + '</span><span class="log-level ' + level + '">' + level + '</span><span class="log-msg">' + msg + '</span></div>\n';
        }
        container.innerHTML = html;
        container.scrollTop = container.scrollHeight;
    }

    function processMessage(msg, level) {
        // If it's a CHAT level message, strip the [CHAT] prefix for display
        if (level === 'CHAT' && state.stripChatTag) {
            msg = msg.replace(/^\[CHAT\]\s*/i, '');
        }
        return formatMinecraftColors(escapeHtml(msg));
    }

    function formatTime(ts) {
        if (!ts) return '--:--:--';
        var d = new Date(ts);
        if (state.shortTime) {
            return d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', hour12: false });
        }
        return d.toLocaleTimeString('ru-RU', { hour12: false }) + '.' + String(d.getMilliseconds()).padStart(3, '0');
    }

    function escapeHtml(str) {
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function formatMinecraftColors(text) {
        var colorMap = { '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA', '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA', '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF', 'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF' };
        var formatMap = { 'l': 'font-weight:bold;', 'o': 'font-style:italic;', 'n': 'text-decoration:underline;', 'm': 'text-decoration:line-through;' };
        var parts = text.split(/(§[0-9a-fk-or])/gi);
        var result = '', currentColor = '', currentFormat = '';
        for (var i = 0; i < parts.length; i++) {
            var part = parts[i];
            if (part.indexOf('§') === 0 && part.length === 2) {
                var code = part.charAt(1).toLowerCase();
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) { currentColor = colorMap[code] || ''; currentFormat = ''; }
                else if (code === 'r') { currentColor = ''; currentFormat = ''; }
                else if (formatMap[code]) { currentFormat += formatMap[code]; }
            } else if (part) {
                if (currentColor || currentFormat) {
                    var style = '';
                    if (currentColor) style += 'color:' + currentColor + ';';
                    if (currentFormat) style += currentFormat;
                    result += '<span style="' + style + '">' + part + '</span>';
                } else { result += part; }
            }
        }
        return result;
    }

    // ===== CHAT =====
    async function sendChat() {
        var msg = dom.chatInput.value.trim();
        if (!msg) return;
        try {
            var res = await fetch('/api/chat/send', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ message: msg }) });
            var data = await res.json();
            if (data.success) dom.chatInput.value = '';
        } catch (err) { console.error('Failed to send chat:', err); }
    }

    // ===== START =====
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
