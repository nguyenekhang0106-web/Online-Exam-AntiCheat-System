"use strict";
(() => {
    const toggle = document.querySelector("[data-nav-toggle]");
    const backdrop = document.querySelector("[data-nav-backdrop]");
    const setOpen = open => {
        document.body.classList.toggle("nav-open", open);
        if (toggle) toggle.setAttribute("aria-expanded", String(open));
    };
    if (toggle) {
        toggle.addEventListener("click", () => setOpen(!document.body.classList.contains("nav-open")));
    }
    if (backdrop) {
        backdrop.addEventListener("click", () => setOpen(false));
    }
    document.addEventListener("keydown", e => {
        if (e.key === "Escape") setOpen(false);
    });

    // Sync selection mode & question count visual hint on exam config forms
    for (const modeSelect of document.querySelectorAll("select[name='mode']")) {
        const form = modeSelect.closest("form");
        const countInput = form ? form.querySelector("input[name='count']") : null;
        if (!countInput) continue;
        const sync = () => {
            const random = modeSelect.value === "RANDOM_PER_ATTEMPT";
            countInput.placeholder = random ? "Ví dụ: 40" : "Bỏ trống khi chọn cố định";
            countInput.required = random;
        };
        modeSelect.addEventListener("change", sync);
        sync();
    }
})();
