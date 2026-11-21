(() => {
    "use strict";
    const monitor = document.querySelector("#monitor");
    const form = document.querySelector("#timeout");
    if (!monitor || !form) return;
    const message = document.querySelector("#monitor-status");
    const deadline = performance.now() + Number(document.querySelector("#remaining").dataset.seconds) * 1000;
    const queue = [];
    const last = new Map();
    let active = true, busy = false, timer, controller, next = 0, failures = 0;
    function stop() {
        active = false;
        clearTimeout(timer);
        controller?.abort();
        queue.length = 0;
    }
    function schedule() {
        clearTimeout(timer);
        if (active && queue.length && !busy && navigator.onLine)
            timer = setTimeout(send, Math.max(0, next - performance.now()));
    }
    async function send() {
        if (!active || performance.now() >= deadline) { stop(); return; }
        if (busy || !queue.length) return;
        busy = true;
        controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 5000);
        next = performance.now() + 2200;
        try {
            const body = new URLSearchParams({...queue[0], attempt: form.elements.id.value, csrf: form.elements.csrf.value});
            const r = await fetch("proctoring/events", {method:"POST", body, signal:controller.signal, redirect:"manual"});
            if (r.status === 201 || r.status === 204) {
                queue.shift(); failures = 0;
            } else if (r.status === 429) {
                next = performance.now() + Math.max(60, Number(r.headers.get("Retry-After")) || 60) * 1000;
                message.textContent = "Ghi nhận tín hiệu tạm nghỉ. Bạn vẫn có thể làm và nộp bài.";
            } else if (r.status >= 500) {
                throw new Error();
            } else {
                stop();
                message.textContent = "Ghi nhận tín hiệu đã dừng. Hãy kiểm tra phiên và thời gian làm bài.";
            }
        } catch {
            if (active) {
                next = performance.now() + Math.min(60000, 2200 * 2 ** Math.min(++failures,5));
                message.textContent = "Chưa gửi được tín hiệu. Bạn vẫn có thể làm bài.";
            }
        } finally {
            clearTimeout(timeout); busy = false; schedule();
        }
    }
    function report(code, note) {
        const now = performance.now();
        if (!active || now >= deadline || now - (last.get(code) ?? -Infinity) < 1500) return;
        last.set(code,now);
        if (queue.length >= 20) { message.textContent = "Hàng đợi tín hiệu đã đầy. Bạn vẫn có thể làm bài."; return; }
        // randomUUID requires a secure context. getRandomValues also supports local HTTP deployments.
        const bytes = crypto.getRandomValues(new Uint8Array(16));
        bytes[6] = (bytes[6] & 15) | 64; bytes[8] = (bytes[8] & 63) | 128;
        const hex = Array.from(bytes,b => b.toString(16).padStart(2,"0")).join("");
        const key = [hex.slice(0,8),hex.slice(8,12),hex.slice(12,16),hex.slice(16,20),hex.slice(20)].join("-");
        queue.push({code,key,occurred:new Date().toISOString()});
        message.textContent = note;
        schedule();
    }
    let blurTimer;
    if (monitor.dataset.focus === "true") {
        let hidden = document.hidden, blurred = !document.hasFocus();
        document.addEventListener("visibilitychange", () => {
            if (document.hidden === hidden) return;
            hidden = document.hidden;
            if (hidden) {
                clearTimeout(blurTimer);
                report("TAB_HIDDEN","Trang thi vừa bị ẩn. Hãy quay lại tab thi để tiếp tục.");
            }
        });
        window.addEventListener("blur", () => {
            if (blurred) return;
            blurred = true;
            blurTimer = setTimeout(() => {
                if (blurred && !document.hidden) report("WINDOW_BLUR","Cửa sổ thi vừa mất focus. Hãy quay lại để tiếp tục.");
            },350);
        });
        window.addEventListener("focus", () => { blurred = false; clearTimeout(blurTimer); });
    }
    const button = document.querySelector("#enter-fullscreen");
    if (button) {
        const state = document.querySelector("#fullscreen-state");
        let full = !!document.fullscreenElement;
        function render() {
            button.hidden = full || !document.fullscreenEnabled;
            state.textContent = !document.fullscreenEnabled ? "Trình duyệt không hỗ trợ toàn màn hình." :
                full ? "Đang toàn màn hình." : "Chưa ở chế độ toàn màn hình.";
        }
        button.addEventListener("click",async () => {
            try { await document.documentElement.requestFullscreen(); }
            catch { state.textContent = "Không thể vào toàn màn hình. Bạn vẫn có thể làm bài."; }
        });
        document.addEventListener("fullscreenchange",() => {
            const previous = full; full = !!document.fullscreenElement; render();
            if (previous && !full) report("FULLSCREEN_EXIT","Bạn vừa thoát toàn màn hình. Có thể bật lại bằng nút ở trên.");
        });
        render();
    }
    for (const event of ["copy","paste"]) document.addEventListener(event,e => {
        const selection = event === "copy" ? window.getSelection() : null;
        const anchor = selection?.anchorNode;
        const selectedQuestion = selection && !selection.isCollapsed &&
            (anchor instanceof Element ? anchor : anchor?.parentElement)?.closest("form.answer");
        if (!e.target?.closest?.("form.answer") && !selectedQuestion) return;
        if (event === "paste" && monitor.dataset.paste !== "true") return;
        report(event === "copy" ? "COPY_ATTEMPT" : "PASTE_ATTEMPT","Đã quan sát thao tác " + (event === "copy" ? "sao chép." : "dán."));
    });
    window.addEventListener("online",schedule);
    window.addEventListener("pagehide",stop);
    window.addEventListener("attempt:leaving",stop);
    window.addEventListener("pageshow",e => { if(e.persisted) location.reload(); });
    form.addEventListener("submit",stop);
})();
