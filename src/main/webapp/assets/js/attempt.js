"use strict";
const forms = Array.from(document.querySelectorAll("form.answer"));
const pending = new Map();
const dirty = new Set();
async function save(form) {
    if (pending.has(form)) return pending.get(form);
    const output = form.querySelector("output");
    const work = (async () => {
        do {
            dirty.delete(form);
            output.textContent = "Đang lưu…";
            try {
                const response = await fetch("attempts", {
                    method: "POST", headers: {Accept: "application/json"},
                    body: new URLSearchParams(new FormData(form))
                });
                if (!response.ok || !response.headers.get("Content-Type")?.includes("application/json")) {
                    throw new Error(response.status === 409 ? "Dữ liệu đã thay đổi hoặc hết giờ. Hãy tải lại." : "Chưa lưu được. Kiểm tra kết nối và phiên đăng nhập.");
                }
                form.elements.revision.value = (await response.json()).revision;
                output.textContent = form.querySelector(":checked") ? "Đã lưu" : "Đã lưu · Chưa trả lời";
            } catch (error) {
                dirty.add(form);
                output.textContent = error instanceof TypeError ? "Mất kết nối. Hãy bấm Lưu để thử lại." : error.message;
                break;
            }
        } while (dirty.has(form));
    })();
    pending.set(form, work);
    try { await work; } finally { pending.delete(form); }
}
for (const form of forms) {
    form.addEventListener("change", () => { dirty.add(form); save(form); });
    form.addEventListener("submit", event => { event.preventDefault(); save(form); });
}
document.querySelector("#confirm").addEventListener("click", async event => {
    event.preventDefault();
    const target = event.currentTarget.href;
    await Promise.all([...pending.values()]);
    if (dirty.size) {
        dirty.values().next().value.scrollIntoView();
        return;
    }
    location.assign(target);
});
const timer = document.querySelector("#remaining");
const end = performance.now() + Number(timer.dataset.seconds) * 1000;
const tick = setInterval(() => {
    const seconds = Math.max(0, Math.ceil((end - performance.now()) / 1000));
    timer.textContent = Math.floor(seconds / 60) + ":" + String(seconds % 60).padStart(2,"0");
    if (!seconds) { clearInterval(tick); document.querySelector("#timeout").requestSubmit(); }
}, 250);
