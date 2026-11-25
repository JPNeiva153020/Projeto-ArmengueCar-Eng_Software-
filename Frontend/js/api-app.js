"use strict";

const API = "http://localhost:8080/api";
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (c) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
const money = (n) => Number(n || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const labels = {
  RECEPCAO_CHECKIN: "Recepção / Check-in", DIAGNOSTICO: "Em diagnóstico",
  AGUARDANDO_APROVACAO: "Aguardando aprovação", AGUARDANDO_INICIO: "Aguardando início",
  EXECUCAO: "Em execução", AGUARDANDO_PECAS: "Aguardando peças",
  CONTROLE_QUALIDADE: "Controle de qualidade", PRONTO_ENTREGA: "Pronto para entrega",
};
let token = sessionStorage.getItem("armenguecar-token");
let user = null;
let currentView = "dashboard";
let orders = [], clients = [], stock = [], notifications = [], users = [];

async function api(path, options = {}) {
  let response;
  try {
    response = await fetch(API + path, {
      ...options,
      headers: { ...(options.body ? { "Content-Type": "application/json" } : {}), ...(token ? { Authorization: "Bearer " + token } : {}), ...options.headers },
    });
  } catch {
    throw new Error("Não foi possível conectar à API. Verifique se o backend está em execução na porta 8080.");
  }
  if (response.status === 401 && path === "/auth/login") throw new Error("E-mail ou senha inválidos.");
  if (response.status === 401 || (response.status === 403 && token && path === "/me")) {
    logout();
    throw new Error("Sessão encerrada. Entre novamente.");
  }
  if (!response.ok) {
    let body;
    try { body = await response.json(); } catch { body = {}; }
    throw new Error(body.message || (response.status === 403 ? "Seu perfil não tem permissão para esta ação." : "Erro na API (" + response.status + ")."));
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
function request(method, body) { return { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) }; }
function showError(error) { $("#error").textContent = error.message || String(error); }
function logout() {
  token = null; user = null; sessionStorage.removeItem("armenguecar-token");
  $("#shell").classList.add("hidden"); $("#login").classList.remove("hidden");
  $("#password").value = ""; $("#app").innerHTML = "";
}
async function start() {
  user = await api("/me");
  if (user.role === "CLIENTE") {
    logout();
    throw new Error("O portal do cliente ainda não está disponível. Use uma conta da equipe.");
  }
  $("#login").classList.add("hidden"); $("#shell").classList.remove("hidden");
  $("#currentUser").textContent = user.name + " · " + user.role;
  document.querySelector('[data-view="users"]').hidden = user.role !== "ADMIN";
  await show(currentView);
}
$("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault(); $("#loginError").textContent = "";
  const button = event.submitter; button.disabled = true;
  try {
    token = null;
    const result = await api("/auth/login", request("POST", { email: $("#email").value.trim(), password: $("#password").value }));
    token = result.token;
    sessionStorage.setItem("armenguecar-token", token);
    $("#password").value = "";
    await start();
  } catch (error) { logout(); $("#loginError").textContent = error.message; }
  finally { button.disabled = false; }
});
$("#logoutBtn").addEventListener("click", logout);
document.querySelectorAll("[data-view]").forEach((button) => button.addEventListener("click", () => show(button.dataset.view)));

async function show(view) {
  currentView = view; $("#error").textContent = "";
  document.querySelectorAll("[data-view]").forEach((b) => b.setAttribute("aria-current", b.dataset.view === view ? "page" : "false"));
  $("#app").innerHTML = "<p>Carregando…</p>";
  try {
    if (view === "dashboard") {
      const data = await api("/dashboard");
      $("#app").innerHTML = `<h2>Visão geral</h2><div class="integration-grid">
        <div class="integration-card">Ordens totais<h2>${data.totalOrders}</h2></div>
        <div class="integration-card">Ordens em andamento<h2>${data.activeOrders}</h2></div>
        <div class="integration-card">Total orçado<h2>${money(data.totalBudget)}</h2></div>
        <div class="integration-card">Itens em reposição<h2>${data.criticalStock}</h2></div></div>`;
    } else if (view === "orders") {
      [orders, clients] = await Promise.all([api("/orders"), api("/clients")]);
      $("#app").innerHTML = `<div class="integration-head"><h2>Ordens de serviço</h2><button class="btn primary" data-action="new-order">+ Nova ordem</button></div>
        <div class="integration-table"><table><thead><tr><th>Ordem</th><th>Cliente / veículo</th><th>Etapa</th><th>Valor</th><th></th></tr></thead><tbody>
        ${orders.map((o) => `<tr><td>${escapeHtml(o.number)}</td><td>${escapeHtml(o.client.name)}<br>${escapeHtml(o.vehicle)} · ${escapeHtml(o.plate)}</td><td>${escapeHtml(labels[o.stage] || o.stage)}</td><td>${money(o.value)}</td><td><button class="btn" data-action="order" data-id="${escapeHtml(o.id)}">Abrir</button></td></tr>`).join("") || '<tr><td colspan="5">Nenhuma ordem cadastrada.</td></tr>'}
        </tbody></table></div>`;
    } else if (view === "clients") {
      clients = await api("/clients");
      $("#app").innerHTML = `<div class="integration-head"><h2>Clientes</h2><button class="btn primary" data-action="new-client">+ Novo cliente</button></div>
        <div class="integration-table"><table><thead><tr><th>Nome</th><th>Telefone</th><th>E-mail</th><th>Veículo / placa</th></tr></thead><tbody>
        ${clients.map((c) => `<tr><td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.phone)}</td><td>${escapeHtml(c.email)}</td><td>${escapeHtml(c.vehicle)} · ${escapeHtml(c.plate)}</td></tr>`).join("") || '<tr><td colspan="4">Nenhum cliente cadastrado.</td></tr>'}</tbody></table></div>`;
    } else if (view === "stock") {
      stock = await api("/stock");
      $("#app").innerHTML = `<div class="integration-head"><h2>Estoque</h2><button class="btn primary" data-action="move-stock">+ Movimentar</button></div>
        <div class="integration-table"><table><thead><tr><th>Item</th><th>Código</th><th>Saldo</th><th>Reposição</th><th></th></tr></thead><tbody>
        ${stock.map((s) => `<tr><td>${escapeHtml(s.name)}</td><td>${escapeHtml(s.code)}</td><td>${s.quantity} ${escapeHtml(s.unit)}</td><td>${s.reorderPoint}</td><td><button class="btn" data-action="edit-stock" data-id="${escapeHtml(s.id)}">Editar</button></td></tr>`).join("") || '<tr><td colspan="5">Nenhum item cadastrado.</td></tr>'}</tbody></table></div>`;
    } else if (view === "notifications") {
      notifications = await api("/notifications");
      $("#app").innerHTML = `<div class="integration-head"><h2>Notificações</h2><button class="btn" data-action="read-all">Marcar todas como lidas</button></div>${notifications.map((n) => `<div class="integration-card"><strong>${escapeHtml(n.title)}</strong><p>${escapeHtml(n.message)}</p><small>${n.read ? "Lida" : "Não lida"}</small> ${n.read ? "" : `<button class="btn" data-action="read" data-id="${escapeHtml(n.id)}">Marcar lida</button>`}</div>`).join("") || "<p>Nenhuma notificação.</p>"}`;
    } else if (view === "users" && user.role === "ADMIN") {
      users = await api("/users");
      $("#app").innerHTML = `<h2>Usuários</h2><div class="integration-table"><table><thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Situação</th><th></th></tr></thead><tbody>${users.map((u) => `<tr><td>${escapeHtml(u.name)}</td><td>${escapeHtml(u.email)}</td><td>${escapeHtml(u.role)}</td><td>${u.active ? "Ativo" : "Inativo"}</td><td><button class="btn" data-action="toggle-user" data-id="${escapeHtml(u.id)}" ${u.id === user.id ? "disabled" : ""}>${u.active ? "Desativar" : "Ativar"}</button></td></tr>`).join("")}</tbody></table></div>`;
    }
  } catch (error) { $("#app").innerHTML = ""; showError(error); }
}

function form(title, contents, action) {
  $("#app").innerHTML = `<h2>${title}</h2><form class="integration-form" id="editForm" data-submit="${action}">${contents}<div class="integration-actions"><button class="btn primary" type="submit">Salvar</button><button class="btn" type="button" data-action="back">Cancelar</button></div></form>`;
}
function orderDetail(id) {
  const o = orders.find((item) => item.id === id); if (!o) return;
  $("#app").innerHTML = `<div class="integration-card"><h2>${escapeHtml(o.number)} · ${escapeHtml(o.plate)}</h2>
    <p>${escapeHtml(o.client.name)} · ${escapeHtml(o.vehicle)}</p><p>${escapeHtml(o.problem)}</p>
    <p>Etapa: ${escapeHtml(labels[o.stage] || o.stage)} · Orçamento: ${money(o.value)} · Aprovado: ${o.approved ? "Sim" : "Não"}</p>
    <div class="integration-actions"><button class="btn" data-action="order-patch" data-id="${id}" data-op="retreat">Voltar etapa</button>
    <button class="btn primary" data-action="order-patch" data-id="${id}" data-op="advance">Avançar etapa</button>
    <button class="btn" data-action="budget" data-id="${id}">Orçamento</button>
    <button class="btn" data-action="decision" data-id="${id}" data-approved="true">Aprovar</button>
    <button class="btn" data-action="decision" data-id="${id}" data-approved="false">Recusar</button>
    <button class="btn" data-action="checklist" data-id="${id}">Checklist</button></div>
    <p class="integration-note">Fotos e baixa automática de peças ainda não estão disponíveis nesta integração.</p>
    <button class="btn" data-action="back">Voltar</button></div>`;
}
$("#app").addEventListener("click", async (event) => {
  const b = event.target.closest("[data-action]"); if (!b) return;
  const action = b.dataset.action, id = b.dataset.id; $("#error").textContent = ""; b.disabled = true;
  try {
    if (action === "back") return await show(currentView);
    if (action === "new-client") return form("Novo cliente", '<label>Nome<input name="name" required></label><label>Telefone<input name="phone" required></label><label>E-mail<input name="email" type="email"></label><label>Placa<input name="plate"></label><label>Veículo<input name="vehicle"></label>', "client");
    if (action === "new-order") {
      if (!clients.length) throw new Error("Cadastre um cliente antes de criar uma ordem.");
      return form("Nova ordem de serviço", `<label>Cliente<select name="clientId" required>${clients.map((c) => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.name)}</option>`).join("")}</select></label><label>Placa<input name="plate" required></label><label>Veículo<input name="vehicle" required></label><label>Problema<textarea name="problem" required></textarea></label>`, "order");
    }
    if (action === "order") return orderDetail(id);
    if (action === "order-patch") await api("/orders/" + id + "/" + b.dataset.op, request("PATCH"));
    else if (action === "decision") await api("/orders/" + id + "/decision", request("PATCH", { approved: b.dataset.approved === "true" }));
    else if (action === "budget") return form("Orçamento", `<input type="hidden" name="id" value="${escapeHtml(id)}"><label>Valor<input type="number" name="value" min="0.01" step="0.01" required></label>`, "budget");
    else if (action === "checklist") {
      const order = orders.find((item) => item.id === id);
      let checks = [];
      try { checks = JSON.parse(order.checklist); } catch { checks = []; }
      return form("Checklist de qualidade", `<input type="hidden" name="id" value="${escapeHtml(id)}">${["Estrutura e alinhamento", "Funilaria", "Pintura", "Montagem", "Limpeza e entrega técnica"].map((x, i) => `<label><span><input style="width:auto" type="checkbox" name="check" ${checks[i] ? "checked" : ""}> ${x}</span></label>`).join("")}`, "checklist");
    }
    else if (action === "move-stock") return form("Movimentar estoque", '<label>Item<input name="itemName" required></label><label>Tipo<select name="type"><option value="ENTRADA">Entrada</option><option value="SAIDA">Saída</option></select></label><label>Quantidade<input name="quantity" type="number" min="0.01" step="0.01" required></label><label>Unidade<input name="unit" value="un" required></label><label>Motivo<input name="reason"></label>', "movement");
    else if (action === "edit-stock") {
      const s = stock.find((item) => item.id === id);
      return form("Editar estoque", `<input type="hidden" name="id" value="${escapeHtml(s.id)}"><label>Nome<input name="name" value="${escapeHtml(s.name)}" required></label><label>Código<input name="code" value="${escapeHtml(s.code)}" required></label><label>Saldo<input type="number" name="quantity" min="0" step="0.01" value="${s.quantity}" required></label><label>Ponto de reposição<input type="number" name="reorderPoint" min="0" step="0.01" value="${s.reorderPoint}" required></label><label>Unidade<input name="unit" value="${escapeHtml(s.unit)}" required></label>`, "stock");
    } else if (action === "read") await api("/notifications/" + id + "/read", request("PATCH"));
    else if (action === "read-all") await api("/notifications/read-all", request("POST"));
    else if (action === "toggle-user") await api("/users/" + id + "/toggle", request("PATCH"));
    await show(currentView);
  } catch (error) { showError(error); } finally { b.disabled = false; }
});
$("#app").addEventListener("submit", async (event) => {
  const f = event.target.closest("[data-submit]"); if (!f) return;
  event.preventDefault(); $("#error").textContent = ""; const button = f.querySelector('[type="submit"]'); button.disabled = true;
  const d = Object.fromEntries(new FormData(f)); const action = f.dataset.submit;
  try {
    if (action === "client") await api("/clients", request("POST", d));
    else if (action === "order") await api("/orders", request("POST", d));
    else if (action === "budget") await api("/orders/" + d.id + "/budget", request("PATCH", { value: Number(d.value) }));
    else if (action === "checklist") await api("/orders/" + d.id + "/checklist", request("PATCH", { checklist: [...f.querySelectorAll('[name="check"]')].map((x) => x.checked) }));
    else if (action === "movement") await api("/stock/movements", request("POST", { ...d, quantity: Number(d.quantity) }));
    else if (action === "stock") await api("/stock/" + d.id, request("PUT", { name: d.name, code: d.code, quantity: Number(d.quantity), reorderPoint: Number(d.reorderPoint), unit: d.unit }));
    await show(currentView);
  } catch (error) { showError(error); } finally { button.disabled = false; }
});
if (token) start().catch((error) => { logout(); $("#loginError").textContent = error.message; });
