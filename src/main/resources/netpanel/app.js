// ===== NetPanel Frontend =====
(function () {
    'use strict';

    const API_BASE = ''; // Same origin
    const BACKEND_PORT_OFFSET = 100;

    const state = {
        connected: false,
        eventSource: null,
        logs: { console: [], chat: [] },
        panels: { console: true, chat: true, status: true, filemanager: false, settings: false },
        panelsLocked: false,
        clipboard: null,
        fmCurrentPath: '/',
        fmSelectedFiles: [],
        contextTarget: null,
        settings: {},
        users: [],
        backendPort: 0
    };

    const dom = {};

    function init() {
        cacheDom();
        bindEvents();
        startClock();
        loadInitialData();
        connectSSE();
        initPanelDragAndResize();
        loadSettings();
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
        dom.panelFilemanager = document.getElementById('panel-filemanager');
        dom.panelSettings = document.getElementById('panel-settings');
        dom.systemInfo = document.getElementById('system-info');
        dom.fmFileList = document.getElementById('fm-file-list');
        dom.fmCurrentPath = document.getElementById('fm-current-path');
        dom.settingsContent = document.getElementById('settings-content');
        dom.usersList = document.getElementById('users-list');
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
                case 'F4': e.preventDefault(); togglePanel('filemanager'); break;
                case 'F5': e.preventDefault(); togglePanel('settings'); break;
                case 'Escape': clearAllLogs(); hideContextMenu(); break;
            }
        });

        // Context menu
        document.addEventListener('contextmenu', function (e) {
            const logEntry = e.target.closest('.log-entry');
            const fmItem = e.target.closest('.fm-item');
            if (logEntry) {
                e.preventDefault();
                showChatContextMenu(e.clientX, e.clientY, logEntry);
            } else if (fmItem) {
                e.preventDefault();
                showFileManagerContextMenu(e.clientX, e.clientY, fmItem);
            } else {
                hideContextMenu();
            }
        });

        document.addEventListener('click', function (e) {
            if (!e.target.closest('.context-menu')) hideContextMenu();
        });

        // Menu bar
        document.querySelectorAll('.menu-item').forEach(function (item) {
            item.addEventListener('click', function (e) {
                e.stopPropagation();
                const menuId = 'menu-' + item.getAttribute('data-menu');
                const dropdown = document.getElementById(menuId);
                const wasActive = dropdown.classList.contains('active');
                document.querySelectorAll('.dropdown').forEach(d => d.classList.remove('active'));
                if (!wasActive) dropdown.classList.add('active');
            });
        });

        document.querySelectorAll('.dropdown-item').forEach(function (item) {
            item.addEventListener('click', function () {
                handleMenuAction(item.getAttribute('data-action'));
                document.querySelectorAll('.dropdown').forEach(d => d.classList.remove('active'));
            });
        });

        document.addEventListener('click', function () {
            document.querySelectorAll('.dropdown').forEach(d => d.classList.remove('active'));
        });

        // Panel controls
        document.querySelectorAll('.panel-ctrl-btn').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                const panel = btn.closest('.panel');
                const action = btn.getAttribute('data-action');
                if (action === 'lock') togglePanelLock(panel);
            });
        });

        // File manager toolbar
        document.querySelectorAll('.fm-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                handleFileManagerAction(btn.getAttribute('data-action'));
            });
        });

        // Settings controls
        var themeSelect = document.getElementById('setting-theme');
        if (themeSelect) themeSelect.addEventListener('change', applyTheme);

        var accentInput = document.getElementById('setting-accent');
        if (accentInput) accentInput.addEventListener('input', applyAccentColor);

        var scaleInput = document.getElementById('setting-scale');
        if (scaleInput) {
            scaleInput.addEventListener('input', function () {
                document.getElementById('scale-value').textContent = parseFloat(scaleInput.value).toFixed(1) + 'x';
                applyScale();
            });
        }

        var fontsizeInput = document.getElementById('setting-fontsize');
        if (fontsizeInput) {
            fontsizeInput.addEventListener('input', function () {
                document.getElementById('fontsize-value').textContent = fontsizeInput.value + 'px';
                applyFontSize();
            });
        }

        var compactCheckbox = document.getElementById('setting-compact');
        if (compactCheckbox) compactCheckbox.addEventListener('change', applyCompactMode);

        var addUserBtn = document.getElementById('add-user-btn');
        if (addUserBtn) addUserBtn.addEventListener('click', addUser);
    }

    // ===== MENU BAR ACTIONS =====
    function handleMenuAction(action) {
        switch (action) {
            case 'toggle-console': togglePanel('console'); break;
            case 'toggle-chat': togglePanel('chat'); break;
            case 'toggle-status': togglePanel('status'); break;
            case 'toggle-filemanager': togglePanel('filemanager'); loadDirectory(state.fmCurrentPath); break;
            case 'toggle-settings': togglePanel('settings'); loadSettings(); break;
            case 'lock-panels': lockAllPanels(); break;
            case 'unlock-panels': unlockAllPanels(); break;
            case 'clear-logs': clearAllLogs(); break;
            case 'new-file': handleFileManagerAction('new-file'); break;
            case 'new-folder': handleFileManagerAction('new-folder'); break;
            case 'open-folder': handleFileManagerAction('up'); break;
            case 'refresh': loadInitialData(); break;
            case 'download-url': showDownloadUrlDialog(); break;
            case 'copy': copyToClipboard(); break;
            case 'cut': cutToClipboard(); break;
            case 'paste': pasteFromClipboard(); break;
            case 'delete': deleteSelected(); break;
            case 'select-all': selectAllFiles(); break;
            case 'about': togglePanel('settings'); break;
            case 'shortcuts': showDialog('Keyboard Shortcuts',
                '<p>F1 — Console</p><p>F2 — Chat</p><p>F3 — Status</p><p>F4 — File Manager</p><p>F5 — Settings</p><p>ESC — Clear</p>'); break;
        }
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

    // ===== CONTEXT MENUS =====
    function showChatContextMenu(x, y, element) {
        hideContextMenu();
        var menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.id = 'context-menu';
        var message = element.getAttribute('data-message') || '';
        var items = [
            { icon: '📋', label: 'Копировать сообщение', action: function () { copyText(message); }},
            { icon: '📋', label: 'Копировать без времени', action: function () {
                var text = element.textContent.replace(/^\d{2}:\d{2}:\d{2}\.\d{3}\s+\w+\s+/, '');
                copyText(text);
            }},
            { separator: true },
            { icon: '💬', label: 'Ответить', action: function () { dom.chatInput.value = message; dom.chatInput.focus(); }},
            { icon: '🔍', label: 'Поиск похожих', action: function () { searchSimilar(message); }},
            { separator: true },
            { icon: '🗑️', label: 'Очистить лог', action: function () { clearAllLogs(); }}
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
    }

    function showFileManagerContextMenu(x, y, fmItem) {
        hideContextMenu();
        var menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.id = 'context-menu';
        var fileName = fmItem.getAttribute('data-name') || '';
        var isDir = fmItem.getAttribute('data-type') === 'directory';
        var items = [
            { icon: '📂', label: 'Открыть', action: function () { if (isDir) navigateToDir(fileName); else viewFileContent(fileName); }},
            { icon: '✂️', label: 'Вырезать', action: function () { cutFile(fileName); }},
            { icon: '📋', label: 'Копировать', action: function () { copyFile(fileName); }},
            { icon: '📌', label: 'Вставить', action: function () { pasteFile(); }, disabled: !state.clipboard },
            { separator: true },
            { icon: '✏️', label: 'Переименовать', action: function () { renameFile(fileName); }},
            { icon: '🗑️', label: 'Удалить', action: function () { deleteFile(fileName); }},
            { separator: true },
            { icon: 'ℹ️', label: 'Свойства', action: function () { showFileProperties(fileName); }},
            { icon: '⬇️', label: 'Скачать', action: function () { downloadFile(fileName); }}
        ];
        items.forEach(function (item) {
            if (item.separator) {
                var sep = document.createElement('div');
                sep.className = 'context-menu-separator';
                menu.appendChild(sep);
            } else {
                var el = document.createElement('div');
                el.className = 'context-menu-item' + (item.disabled ? ' disabled' : '');
                el.innerHTML = '<span>' + item.icon + '</span><span>' + item.label + '</span>';
                if (!item.disabled) {
                    el.addEventListener('click', function () { item.action(); hideContextMenu(); });
                }
                menu.appendChild(el);
            }
        });
        document.body.appendChild(menu);
        positionContextMenu(menu, x, y);
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

    function searchSimilar(text) {
        var query = prompt('Поиск:', text.substring(0, 20));
        if (query) {
            document.querySelectorAll('.log-entry').forEach(function (e) {
                e.style.display = e.textContent.toLowerCase().indexOf(query.toLowerCase()) >= 0 ? '' : 'none';
            });
        }
    }

    // ===== FILE MANAGER =====
    function handleFileManagerAction(action) {
        switch (action) {
            case 'up': navigateUp(); break;
            case 'refresh': loadDirectory(state.fmCurrentPath); break;
            case 'new-file': promptNewFile(); break;
            case 'new-folder': promptNewFolder(); break;
            case 'upload-url': showDownloadUrlDialog(); break;
        }
    }

    function navigateUp() {
        var parts = state.fmCurrentPath.split('/').filter(Boolean);
        parts.pop();
        navigateToDir('/' + parts.join('/') || '/');
    }

    function navigateToDir(path) {
        state.fmCurrentPath = path;
        dom.fmCurrentPath.textContent = path;
        loadDirectory(path);
    }

    function loadDirectory(path) {
        if (!state.connected) return;
        fetchJSON('/api/files/list?path=' + encodeURIComponent(path)).then(function (data) {
            state.fmCurrentPath = data.path || path;
            dom.fmCurrentPath.textContent = state.fmCurrentPath;
            renderFileList(data.files || []);
        }).catch(function () {
            dom.fmFileList.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">⚠️ Не удалось загрузить файлы</div>';
        });
    }

    function renderFileList(files) {
        if (!files.length) {
            dom.fmFileList.innerHTML = '<div style="padding:20px;color:var(--text-dim);text-align:center;">📂 Папка пуста</div>';
            return;
        }
        var html = '';
        files.sort(function (a, b) {
            if (a.type === 'directory' && b.type !== 'directory') return -1;
            if (a.type !== 'directory' && b.type === 'directory') return 1;
            return a.name.localeCompare(b.name);
        });
        files.forEach(function (file) {
            var icon = file.type === 'directory' ? '📁' : getFileIcon(file.name);
            var size = file.type === 'directory' ? '—' : formatFileSize(file.size);
            html += '<div class="fm-item" data-name="' + escapeHtml(file.name) + '" data-type="' + file.type + '" data-path="' + escapeHtml(state.fmCurrentPath + (state.fmCurrentPath.endsWith('/') ? '' : '/') + file.name) + '">' +
                '<span class="fm-icon">' + icon + '</span>' +
                '<span class="fm-name">' + escapeHtml(file.name) + (file.isArchive ? ' 📦' : '') + '</span>' +
                '<span class="fm-size">' + size + '</span>' +
                '</div>';
        });
        dom.fmFileList.innerHTML = html;
    }

    function getFileIcon(name) {
        var ext = name.split('.').pop().toLowerCase();
        var icons = { js: '📜', html: '🌐', css: '🎨', json: '📋', java: '☕', txt: '📄', log: '📋', png: '🖼️', jpg: '🖼️', gif: '🖼️', mp3: '🎵', mp4: '🎬', zip: '📦', jar: '📦' };
        return icons[ext] || '📄';
    }

    function formatFileSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        if (bytes < 1073741824) return (bytes / 1048576).toFixed(1) + ' MB';
        return (bytes / 1073741824).toFixed(1) + ' GB';
    }

    function promptNewFile() {
        var name = prompt('Имя нового файла:');
        if (name) {
            fetchJSON('/api/files/new-file', { method: 'POST', body: JSON.stringify({ path: state.fmCurrentPath, name: name }) }).then(function () {
                loadDirectory(state.fmCurrentPath);
            });
        }
    }

    function promptNewFolder() {
        var name = prompt('Имя новой папки:');
        if (name) {
            fetchJSON('/api/files/new-folder', { method: 'POST', body: JSON.stringify({ path: state.fmCurrentPath, name: name }) }).then(function () {
                loadDirectory(state.fmCurrentPath);
            });
        }
    }

    function viewFileContent(fileName) {
        var filePath = state.fmCurrentPath + (state.fmCurrentPath.endsWith('/') ? '' : '/') + fileName;
        fetchJSON('/api/files/read?path=' + encodeURIComponent(filePath)).then(function (data) {
            showDialog('📄 ' + fileName, '<pre style="max-height:300px;overflow:auto;background:var(--bg-primary);padding:10px;border-radius:4px;font-size:11px;white-space:pre-wrap;word-break:break-all;">' + escapeHtml(data.content || '') + '</pre>');
        }).catch(function () {
            showDialog('Ошибка', 'Не удалось прочитать файл');
        });
    }

    function cutFile(name) { state.clipboard = { action: 'cut', name: name, path: state.fmCurrentPath }; }
    function copyFile(name) { state.clipboard = { action: 'copy', name: name, path: state.fmCurrentPath }; }

    function pasteFile() {
        if (!state.clipboard) return;
        var destPath = state.fmCurrentPath;
        var srcPath = state.clipboard.path + (state.clipboard.path.endsWith('/') ? '' : '/') + state.clipboard.name;
        if (state.clipboard.action === 'cut') {
            fetchJSON('/api/files/rename', { method: 'POST', body: JSON.stringify({ path: srcPath, newName: state.clipboard.name }) }).then(function () {
                loadDirectory(destPath);
            });
        } else {
            // Copy: read + write
            fetchJSON('/api/files/read?path=' + encodeURIComponent(srcPath)).then(function (data) {
                fetchJSON('/api/files/write', { method: 'POST', body: JSON.stringify({ path: destPath + (destPath.endsWith('/') ? '' : '/') + state.clipboard.name, content: data.content }) }).then(function () {
                    loadDirectory(destPath);
                });
            });
        }
    }

    function deleteFile(name) {
        if (!confirm('Удалить ' + name + '?')) return;
        var filePath = state.fmCurrentPath + (state.fmCurrentPath.endsWith('/') ? '' : '/') + name;
        fetchJSON('/api/files/delete', { method: 'POST', body: JSON.stringify({ path: filePath }) }).then(function () {
            loadDirectory(state.fmCurrentPath);
        });
    }

    function renameFile(name) {
        var newName = prompt('Новое имя:', name);
        if (!newName || newName === name) return;
        var filePath = state.fmCurrentPath + (state.fmCurrentPath.endsWith('/') ? '' : '/') + name;
        fetchJSON('/api/files/rename', { method: 'POST', body: JSON.stringify({ path: filePath, newName: newName }) }).then(function () {
            loadDirectory(state.fmCurrentPath);
        });
    }

    function showFileProperties(name) {
        var filePath = state.fmCurrentPath + (state.fmCurrentPath.endsWith('/') ? '' : '/') + name;
        fetchJSON('/api/files/properties?path=' + encodeURIComponent(filePath)).then(function (data) {
            showDialog('📄 Свойства: ' + name,
                '<div style="font-size:12px;">' +
                '<p><b>Имя:</b> ' + escapeHtml(data.name) + '</p>' +
                '<p><b>Путь:</b> ' + escapeHtml(data.path) + '</p>' +
                '<p><b>Тип:</b> ' + (data.type === 'directory' ? 'Папка' : 'Файл') + '</p>' +
                '<p><b>Размер:</b> ' + formatFileSize(data.size || 0) + '</p>' +
                '<p><b>Создан:</b> ' + new Date(data.created).toLocaleString('ru-RU') + '</p>' +
                '<p><b>Изменён:</b> ' + new Date(data.modified).toLocaleString('ru-RU') + '</p>' +
                '</div>');
        });
    }

    function downloadFile(name) {
        var filePath = state.fmCurrentPath + (state.fmCurrentPath.endsWith('/') ? '' : '/') + name;
        fetchJSON('/api/files/read?path=' + encodeURIComponent(filePath)).then(function (data) {
            var blob = new Blob([data.content], { type: 'text/plain' });
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.href = url; a.download = name;
            document.body.appendChild(a); a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        });
    }

    function copyToClipboard() { /* handled by context menu */ }
    function cutToClipboard() { /* handled by context menu */ }
    function pasteFromClipboard() { /* handled by context menu */ }
    function deleteSelected() { /* handled by context menu */ }
    function selectAllFiles() { /* select all fm items */ }

    function showDownloadUrlDialog() {
        var overlay = document.createElement('div');
        overlay.className = 'dialog-overlay';
        overlay.innerHTML = '<div class="dialog">' +
            '<div class="dialog-title">⬇️ Скачать по URL</div>' +
            '<div class="dialog-body">' +
                '<input type="text" id="download-url-input" placeholder="https://example.com/file.zip" style="width:100%;margin-bottom:8px;">' +
                '<input type="text" id="download-filename-input" placeholder="filename.zip (optional)" style="width:100%;">' +
            '</div>' +
            '<div class="dialog-buttons">' +
                '<button class="dialog-btn" id="download-cancel">Отмена</button>' +
                '<button class="dialog-btn primary" id="download-ok">Скачать</button>' +
            '</div>' +
        '</div>';
        document.body.appendChild(overlay);
        overlay.querySelector('#download-cancel').addEventListener('click', function () { overlay.remove(); });
        overlay.querySelector('#download-ok').addEventListener('click', function () {
            var url = document.getElementById('download-url-input').value.trim();
            var fileName = document.getElementById('download-filename-input').value.trim() || url.substring(url.lastIndexOf('/') + 1);
            if (url) {
                fetchJSON('/api/files/download-url', { method: 'POST', body: JSON.stringify({ url: url, path: state.fmCurrentPath, fileName: fileName }) }).then(function () {
                    loadDirectory(state.fmCurrentPath);
                });
            }
            overlay.remove();
        });
    }

    // ===== SETTINGS =====
    function loadSettings() {
        fetchJSON('/api/settings/get').then(function (data) {
            state.settings = data;
            // Apply settings
            var theme = data.theme || 'dark';
            var accent = data.accentColor || '#58a6ff';
            var scale = data.scale || 1.0;
            var fontSize = data.fontSize || 12;
            var compact = data.compactMode || false;

            document.getElementById('setting-theme').value = theme;
            document.getElementById('setting-accent').value = accent;
            document.getElementById('setting-scale').value = scale;
            document.getElementById('setting-fontsize').value = fontSize;
            document.getElementById('setting-compact').checked = compact;
            document.getElementById('scale-value').textContent = parseFloat(scale).toFixed(1) + 'x';
            document.getElementById('fontsize-value').textContent = fontSize + 'px';

            applyTheme();
            applyAccentColor();
            applyScale();
            applyFontSize();
            applyCompactMode();

            // Load about info
            document.getElementById('about-java').textContent = data.javaVersion || '--';
            document.getElementById('about-os').textContent = data.osName || '--';
            document.getElementById('about-uptime').textContent = data.uptime || '--';
            document.getElementById('about-threads').textContent = data.threadCount || '--';
        }).catch(function () {});

        // Load users
        fetchJSON('/api/settings/users/list').then(function (data) {
            state.users = data;
            renderUsers();
        }).catch(function () {});
    }

    function applyTheme() {
        var theme = document.getElementById('setting-theme').value;
        var root = document.documentElement;
        switch (theme) {
            case 'dark':
                root.style.setProperty('--bg-primary', '#0a0e14');
                root.style.setProperty('--bg-secondary', '#0d1117');
                root.style.setProperty('--bg-panel', '#111820');
                root.style.setProperty('--bg-panel-header', '#161b22');
                root.style.setProperty('--border-color', '#30363d');
                root.style.setProperty('--text-primary', '#c9d1d9');
                root.style.setProperty('--text-secondary', '#8b949e');
                root.style.setProperty('--text-dim', '#484f58');
                break;
            case 'light':
                root.style.setProperty('--bg-primary', '#ffffff');
                root.style.setProperty('--bg-secondary', '#f6f8fa');
                root.style.setProperty('--bg-panel', '#ffffff');
                root.style.setProperty('--bg-panel-header', '#f0f3f6');
                root.style.setProperty('--border-color', '#d0d7de');
                root.style.setProperty('--text-primary', '#1f2328');
                root.style.setProperty('--text-secondary', '#656d76');
                root.style.setProperty('--text-dim', '#8b949e');
                break;
            case 'amoled':
                root.style.setProperty('--bg-primary', '#000000');
                root.style.setProperty('--bg-secondary', '#0a0a0a');
                root.style.setProperty('--bg-panel', '#0d0d0d');
                root.style.setProperty('--bg-panel-header', '#141414');
                root.style.setProperty('--border-color', '#1f1f1f');
                root.style.setProperty('--text-primary', '#e0e0e0');
                root.style.setProperty('--text-secondary', '#a0a0a0');
                root.style.setProperty('--text-dim', '#606060');
                break;
            case 'nord':
                root.style.setProperty('--bg-primary', '#2e3440');
                root.style.setProperty('--bg-secondary', '#3b4252');
                root.style.setProperty('--bg-panel', '#3b4252');
                root.style.setProperty('--bg-panel-header', '#434c5e');
                root.style.setProperty('--border-color', '#4c566a');
                root.style.setProperty('--text-primary', '#d8dee9');
                root.style.setProperty('--text-secondary', '#e5e9f0');
                root.style.setProperty('--text-dim', '#616e88');
                break;
        }
        // Save
        fetchJSON('/api/settings/save', { method: 'POST', body: JSON.stringify({ theme: theme }) }).catch(function () {});
    }

    function applyAccentColor() {
        var color = document.getElementById('setting-accent').value;
        document.documentElement.style.setProperty('--accent-blue', color);
        fetchJSON('/api/settings/save', { method: 'POST', body: JSON.stringify({ accentColor: color }) }).catch(function () {});
    }

    function applyScale() {
        var scale = document.getElementById('setting-scale').value;
        document.getElementById('app').style.transform = 'scale(' + scale + ')';
        document.getElementById('app').style.transformOrigin = 'top left';
        document.getElementById('app').style.width = (100 / scale) + '%';
        document.getElementById('app').style.height = (100 / scale) + '%';
        fetchJSON('/api/settings/save', { method: 'POST', body: JSON.stringify({ scale: parseFloat(scale) }) }).catch(function () {});
    }

    function applyFontSize() {
        var size = document.getElementById('setting-fontsize').value;
        document.documentElement.style.setProperty('--font-size-sm', size + 'px');
        document.documentElement.style.setProperty('--font-size-xs', (size - 1) + 'px');
        document.documentElement.style.setProperty('--font-size-md', (size + 1) + 'px');
        document.documentElement.style.setProperty('--font-size-lg', (size + 2) + 'px');
        fetchJSON('/api/settings/save', { method: 'POST', body: JSON.stringify({ fontSize: parseInt(size) }) }).catch(function () {});
    }

    function applyCompactMode() {
        var compact = document.getElementById('setting-compact').checked;
        if (compact) {
            document.documentElement.style.setProperty('--panel-radius', '0px');
            document.documentElement.style.setProperty('--font-size-xs', '10px');
        } else {
            document.documentElement.style.setProperty('--panel-radius', '4px');
            document.documentElement.style.setProperty('--font-size-xs', '11px');
        }
        fetchJSON('/api/settings/save', { method: 'POST', body: JSON.stringify({ compactMode: compact }) }).catch(function () {});
    }

    function renderUsers() {
        var html = '';
        state.users.forEach(function (user, i) {
            var initial = user.name.charAt(0).toUpperCase();
            html += '<div class="user-item">' +
                '<div class="user-avatar" style="background:' + (user.color || '#58a6ff') + ';">' + initial + '</div>' +
                '<div class="user-info"><div class="user-name">' + escapeHtml(user.name) + '</div><div class="user-role">' + escapeHtml(user.role || 'user') + '</div></div>' +
                '<span class="user-remove-btn" data-index="' + i + '" title="Remove">✕</span>' +
                '</div>';
        });
        dom.usersList.innerHTML = html || '<div style="color:var(--text-dim);font-size:11px;padding:8px;">Нет пользователей</div>';

        // Bind remove buttons
        dom.usersList.querySelectorAll('.user-remove-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var idx = parseInt(btn.getAttribute('data-index'));
                var user = state.users[idx];
                if (user && confirm('Удалить ' + user.name + '?')) {
                    fetchJSON('/api/settings/users/remove', { method: 'POST', body: JSON.stringify({ name: user.name }) }).then(function () {
                        loadSettings();
                    });
                }
            });
        });
    }

    function addUser() {
        var name = document.getElementById('new-user-name').value.trim();
        var role = document.getElementById('new-user-role').value;
        var color = document.getElementById('new-user-color').value;
        if (!name) return;
        fetchJSON('/api/settings/users/add', { method: 'POST', body: JSON.stringify({ name: name, role: role, color: color }) }).then(function () {
            document.getElementById('new-user-name').value = '';
            loadSettings();
        });
    }

    // ===== DIALOG HELPER =====
    function showDialog(title, bodyHtml) {
        var overlay = document.createElement('div');
        overlay.className = 'dialog-overlay';
        overlay.innerHTML = '<div class="dialog">' +
            '<div class="dialog-title">' + title + '</div>' +
            '<div class="dialog-body">' + bodyHtml + '</div>' +
            '<div class="dialog-buttons">' +
                '<button class="dialog-btn primary" id="dialog-close">OK</button>' +
            '</div>' +
        '</div>';
        document.body.appendChild(overlay);
        overlay.querySelector('#dialog-close').addEventListener('click', function () { overlay.remove(); });
        overlay.addEventListener('click', function (e) { if (e.target === overlay) overlay.remove(); });
    }

    // ===== PANELS =====
    function togglePanel(name) {
        state.panels[name] = !state.panels[name];
        var panelMap = { console: dom.panelConsole, chat: dom.panelChat, status: dom.panelStatus, filemanager: dom.panelFilemanager, settings: dom.panelSettings };
        var panel = panelMap[name];
        if (panel) panel.classList.toggle('hidden', !state.panels[name]);
        if (name === 'filemanager' && state.panels[name]) loadDirectory(state.fmCurrentPath);
        if (name === 'settings' && state.panels[name]) loadSettings();
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
            try { updateSystemInfo(JSON.parse(e.data)); } catch (err) {}
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

    // ===== SYSTEM INFO (no GC) =====
    function updateSystemInfo(data) {
        var fps = data.fps || 0;
        dom.fpsDisplay.innerHTML = 'FPS: <span class="val">' + fps + '</span>';
        var sys = data.system;
        if (!sys) return;
        var cpuPct = sys.cpuPercent != null ? sys.cpuPercent : -1;
        var sysCpuPct = sys.systemCpuPercent != null ? sys.systemCpuPercent : -1;
        dom.memDisplay.innerHTML = 'MEM: <span class="val">' + (sys.usedMemory || 0) + ' / ' + sys.totalMemory + ' MB</span>';
        dom.cpuDisplay.innerHTML = cpuPct >= 0 ? 'CPU: <span class="val">' + cpuPct + '%</span>' : 'CPU: <span class="val">N/A</span>';

        var html = '';
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
            var msg = formatMinecraftColors(escapeHtml(entry.message || ''));
            html += '<div class="log-entry" data-message="' + escapeHtml(entry.message || '') + '"><span class="log-time">' + time + '</span><span class="log-level ' + level + '">' + level + '</span><span class="log-msg">' + msg + '</span></div>\n';
        }
        container.innerHTML = html;
        container.scrollTop = container.scrollHeight;
    }

    function formatTime(ts) {
        if (!ts) return '--:--:--';
        var d = new Date(ts);
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
