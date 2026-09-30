// Estado global da aplicação (persistido em localStorage) e helpers
// de formatação monetária / persistência.
let db = JSON.parse(localStorage.getItem("armenguecar-v2") || "null") || structuredClone(seed),
  role = localStorage.getItem("armenguecar-role") || "gerente",
  view = "dashboard",
  currentOrder = null,
  photosStore = {};

const money = (n) => n.toLocaleString("pt-BR", { style: "currency", currency: "BRL" }),
  save = () => {
    localStorage.setItem("armenguecar-v2", JSON.stringify(db));
    localStorage.setItem("armenguecar-role", role);
  };
