// Ponto de entrada: login, troca de perfil, logout, menu mobile,
// atalho de busca, fechamento de modal por clique fora e retomada
// de sessão salva.
$("#loginForm").onsubmit = (e) => {
  e.preventDefault();
  role = $("#loginRole").value;
  save();
  $("#login").classList.add("hidden");
  $("#shell").classList.remove("hidden");
  render();
  toastMsg(`Bem-vindo, ${roles[role][0].split(" ")[0]}.`);
};

$("#roleBtn").onclick = () => {
  let ks = Object.keys(roles),
    n = ks[(ks.indexOf(role) + 1) % ks.length];
  role = n;
  save();
  render();
  toastMsg(`Perfil: ${roles[role][1]}`);
};

$("#logoutBtn").onclick = () => {
  $("#shell").classList.add("hidden");
  $("#login").classList.remove("hidden");
};

$("#mobileMenu").onclick = () => $("#sidebar").classList.toggle("open");

$("#searchBtn").onclick = () => {
  view = "orders";
  render();
  setTimeout(() => $("#q")?.focus(), 50);
};

$("#modalBackdrop").onclick = (e) => {
  if (e.target.id === "modalBackdrop") closeModal();
};
if (localStorage.getItem("armenguecar-v2")) {
  $("#login").classList.add("hidden");
  $("#shell").classList.remove("hidden");
  render();
}
