(() => {
  let theme = matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
  try {
    const saved = localStorage.getItem("companion-site-theme");
    if (saved === "light" || saved === "dark") theme = saved;
  } catch {}
  document.documentElement.dataset.theme = theme;
})();
