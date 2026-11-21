(() => {
    "use strict";
    const form = document.querySelector("#timeout");
    const output = document.querySelector("#contact-status");
    if (!form || !output) return;
    const deadline = performance.now() + Number(document.querySelector("#remaining").dataset.seconds) * 1000;
    const retry = document.querySelector("#retry-contact");
    let timer, request, failures = 0, stopped = false;
    function stop() {
        stopped = true;
        clearTimeout(timer);
        request?.abort();
        retry.hidden = true;
    }
    function schedule(delay) {
        clearTimeout(timer);
        if (!stopped && navigator.onLine) timer = setTimeout(check, delay);
    }
    function offline() {
        clearTimeout(timer);
        output.textContent = "Trình duyệt báo ngoại tuyến. Đáp án đã được máy chủ xác nhận vẫn được giữ.";
    }
    async function check() {
        if (stopped || request) return;
        if (performance.now() >= deadline) { stop(); return; }
        if (!navigator.onLine) { offline(); return; }
        retry.hidden = true;
        const controller = new AbortController();
        request = controller;
        const timeout = setTimeout(() => controller.abort(), 8000);
        try {
            const body = new URLSearchParams({csrf:form.elements.csrf.value, attempt:form.elements.id.value});
            const response = await fetch("proctoring/heartbeat", {method:"POST", body, signal:controller.signal, redirect:"manual"});
            if (stopped) return;
            if (response.status === 204) {
                output.textContent = "Máy chủ vừa xác nhận kết nối của lượt thi.";
                failures = 0;
                schedule(30000);
            } else if ([401,403,404,409].includes(response.status) || response.type === "opaqueredirect") {
                stop();
                window.dispatchEvent(new Event("attempt:contact-ended"));
                output.textContent = response.status === 409 ? "Lượt thi đã kết thúc hoặc điều kiện dự thi đã thay đổi. Hãy kiểm tra lại lượt thi." :
                    response.status === 401 || response.type === "opaqueredirect" ? "Phiên đã hết hiệu lực. Hãy đăng nhập lại." : "Máy chủ từ chối quyền truy cập. Hãy kiểm tra lại lượt thi.";
            } else throw new Error();
        } catch {
            if (stopped) return;
            failures++;
            output.textContent = failures === 1 ? "Chưa nhận được phản hồi. Đang thử kết nối lại." : "Chưa liên lạc được với máy chủ. Hãy kiểm tra kết nối.";
            if (failures < 5) schedule(Math.min(60000, 5000 * 2 ** failures));
            else retry.hidden = false;
            if (!navigator.onLine) offline();
        } finally {
            clearTimeout(timeout);
            request = null;
        }
    }
    retry.addEventListener("click", () => { failures = 0; check(); });
    window.addEventListener("offline", offline);
    window.addEventListener("online", () => { if (!stopped) { output.textContent = "Đang xác minh lại kết nối với máy chủ."; schedule(0); } });
    window.addEventListener("pagehide", stop);
    window.addEventListener("attempt:leaving", stop);
    form.addEventListener("submit", stop);
    check();
})();
