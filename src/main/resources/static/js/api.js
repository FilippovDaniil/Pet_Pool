// api.js — CSRF-aware REST client for Billiard Club

// Read XSRF-TOKEN cookie set by CookieCsrfTokenRepository
function getCsrfToken() {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
}

async function apiRequest(url, method, body) {
    const opts = {
        method,
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCsrfToken()
        }
    };
    if (body !== undefined) opts.body = JSON.stringify(body);
    return fetch(url, opts);
}

function showToast(msg, type) {
    type = type || 'success';
    const div = document.createElement('div');
    div.className = 'alert alert-' + type + ' alert-dismissible position-fixed top-0 start-50 translate-middle-x mt-3 shadow';
    div.style.zIndex = '9999';
    div.style.minWidth = '300px';
    div.innerHTML = msg + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
    document.body.appendChild(div);
    setTimeout(function() { div.remove(); }, 4000);
}

// ── Booking actions ──────────────────────────────────────────────────────────

async function cancelBooking(bookingId) {
    if (!confirm('Отменить бронирование?')) return;
    const res = await apiRequest('/api/bookings/' + bookingId, 'PATCH', { status: 'CANCELLED' });
    if (res.ok) {
        location.reload();
    } else {
        const data = await res.json().catch(function() { return {}; });
        showToast(data.message || 'Ошибка при отмене бронирования', 'danger');
    }
}

async function payBooking(bookingId) {
    const radios = document.querySelectorAll('input[name="paymentMethod-' + bookingId + '"]');
    let method = 'CASH';
    radios.forEach(function(r) { if (r.checked) method = r.value; });
    const res = await apiRequest('/api/bookings/' + bookingId + '/payments', 'POST', { paymentMethod: method });
    if (res.ok) {
        bootstrap.Modal.getInstance(document.getElementById('payModal-' + bookingId)).hide();
        location.reload();
    } else {
        const data = await res.json().catch(function() { return {}; });
        showToast(data.message || 'Ошибка при оплате', 'danger');
    }
}

async function startGame(bookingId) {
    const select = document.getElementById('opponent-' + bookingId);
    const opponentId = select ? parseInt(select.value) : null;
    if (!opponentId) { showToast('Выберите второго игрока', 'warning'); return; }
    const res = await apiRequest('/api/bookings/' + bookingId, 'PATCH', { status: 'ACTIVE', opponentId: opponentId });
    if (res.ok) {
        bootstrap.Modal.getInstance(document.getElementById('startGameModal-' + bookingId)).hide();
        location.reload();
    } else {
        const data = await res.json().catch(function() { return {}; });
        showToast(data.message || 'Ошибка при старте игры', 'danger');
    }
}

async function finishGame(bookingId) {
    const radios = document.querySelectorAll('input[name="winnerId-' + bookingId + '"]');
    let winnerId = null;
    radios.forEach(function(r) { if (r.checked) winnerId = parseInt(r.value); });
    if (!winnerId) { showToast('Выберите победителя', 'warning'); return; }
    const res = await apiRequest('/api/bookings/' + bookingId, 'PATCH', { status: 'COMPLETED', winnerId: winnerId });
    if (res.ok) {
        bootstrap.Modal.getInstance(document.getElementById('finishGameModal-' + bookingId)).hide();
        location.reload();
    } else {
        const data = await res.json().catch(function() { return {}; });
        showToast(data.message || 'Ошибка при завершении игры', 'danger');
    }
}

// ── Tournament actions ───────────────────────────────────────────────────────

async function deleteTournamentRecord(recordId) {
    if (!confirm('Удалить запись из турнирной доски?')) return;
    const res = await apiRequest('/api/tournament/' + recordId, 'DELETE');
    if (res.ok) {
        location.reload();
    } else {
        showToast('Ошибка при удалении записи', 'danger');
    }
}

async function saveTournamentRecord(recordId) {
    const winnerInput = document.getElementById('winnerName');
    const loserInput  = document.getElementById('loserName');
    const winnerName  = winnerInput ? winnerInput.value.trim() : '';
    const loserName   = loserInput  ? loserInput.value.trim()  : '';
    if (!winnerName || !loserName) { showToast('Заполните оба поля', 'warning'); return; }
    const res = await apiRequest('/api/tournament/' + recordId, 'PUT', { winnerName, loserName });
    if (res.ok) {
        location.href = '/tournament';
    } else {
        const data = await res.json().catch(function() { return {}; });
        showToast(data.message || 'Ошибка при сохранении', 'danger');
    }
}
