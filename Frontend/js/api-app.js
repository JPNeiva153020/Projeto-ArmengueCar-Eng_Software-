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
let orders = [], clients = [], vehicles = [], stock = [], notifications = [], users = [];

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
  const isClient = user.role === "CLIENTE";
  document.querySelectorAll("[data-view]").forEach((button) => {
    button.hidden = isClient
      ? !["portal", "notifications"].includes(button.dataset.view)
      : button.dataset.view === "portal" || (button.dataset.view === "users" && user.role !== "ADMIN");
  });
  currentView = isClient ? "portal" : "dashboard";
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
    if (view === "portal") {
      const ownOrders = await api("/portal/orders");
      $("#app").innerHTML = `<h2>Meus veículos</h2><p class="integration-note">O acompanhamento mostra apenas as ordens vinculadas à sua conta. Orçamentos e fotos ainda estão em desenvolvimento.</p>${ownOrders.map((o) => `<div class="integration-card"><h2>${escapeHtml(o.number)} · ${escapeHtml(o.plate)}</h2><p>${escapeHtml(o.vehicle)}</p><p>${escapeHtml(o.problem)}</p><p>Etapa: ${escapeHtml(labels[o.stage] || o.stage)} · Progresso: ${Number(o.progress)}%</p></div>`).join("") || "<p>Nenhuma ordem vinculada à sua conta. Se precisar, procure a oficina.</p>"}`;
    } else if (view === "dashboard") {
      const data = await api("/dashboard");
      $("#app").innerHTML = `<h2>Visão geral</h2><div class="integration-grid">
        <div class="integration-card">Ordens totais<h2>${data.totalOrders}</h2></div>
        <div class="integration-card">Ordens em andamento<h2>${data.activeOrders}</h2></div>
        <div class="integration-card">Total orçado<h2>${money(data.totalBudget)}</h2></div>
        <div class="integration-card">Itens em reposição<h2>${data.criticalStock}</h2></div></div>`;
    } else if (view === "kanban") {
      orders = await api("/orders");
      const active = orders.filter((o) => !o.closedAt);
      $("#app").innerHTML = `<h2>Kanban operacional</h2>
        <label for="kanbanFilter">Pesquisar por ordem, veículo, placa ou responsável</label>
        <input id="kanbanFilter" class="kanban-filter" type="search" placeholder="Pesquisar no quadro">
        <div class="kanban-board">${Object.entries(labels).map(([stage, label]) => {
          const column = active.filter((o) => o.stage === stage);
          return `<section class="kanban-column"><h3>${escapeHtml(label)} · ${column.length}</h3>
            ${column.map((o) => `<article class="kanban-card-real" data-kanban-card>
              <strong>${escapeHtml(o.number)}</strong><p>${escapeHtml(o.plate)} · ${escapeHtml(o.vehicle)}</p>
              <p>${escapeHtml(o.client.name)} · Resp.: ${escapeHtml(o.ownerInitials)}</p>
              <button class="btn" data-action="order" data-id="${escapeHtml(o.id)}">Abrir ordem</button>
            </article>`).join("") || '<p class="muted">Nenhuma ordem</p>'}</section>`;
        }).join("")}</div>`;
      $("#kanbanFilter").addEventListener("input", (event) => {
        const query = event.target.value.trim().toLocaleLowerCase("pt-BR");
        document.querySelectorAll("[data-kanban-card]").forEach((card) => {
          card.hidden = !card.textContent.toLocaleLowerCase("pt-BR").includes(query);
        });
      });
    } else if (view === "orders") {
      [orders, vehicles] = await Promise.all([api("/orders"), api("/vehicles")]);
      $("#app").innerHTML = `<div class="integration-head"><h2>Ordens de serviço</h2><button class="btn primary" data-action="new-order">+ Nova ordem</button></div>
        <div class="integration-table"><table><thead><tr><th>Ordem</th><th>Cliente / veículo</th><th>Etapa</th><th>Valor</th><th></th></tr></thead><tbody>
        ${orders.map((o) => `<tr><td>${escapeHtml(o.number)}</td><td>${escapeHtml(o.client.name)}<br>${escapeHtml(o.vehicle)} · ${escapeHtml(o.plate)}</td><td>${escapeHtml(labels[o.stage] || o.stage)}</td><td>${money(o.value)}</td><td><button class="btn" data-action="order" data-id="${escapeHtml(o.id)}">Abrir</button></td></tr>`).join("") || '<tr><td colspan="5">Nenhuma ordem cadastrada.</td></tr>'}
        </tbody></table></div>`;
    } else if (view === "clients") {
      clients = await api("/clients");
      if (user.role === "ADMIN") users = await api("/users");
      $("#app").innerHTML = `<div class="integration-head"><h2>Clientes</h2><button class="btn primary" data-action="new-client">+ Novo cliente</button></div>
        <div class="integration-table"><table><thead><tr><th>Nome</th><th>Telefone</th><th>E-mail</th><th>Veículo / placa</th>${user.role === "ADMIN" ? "<th>Conta</th>" : ""}</tr></thead><tbody>
        ${clients.map((c) => `<tr><td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.phone)}</td><td>${escapeHtml(c.email)}</td><td>${escapeHtml(c.vehicle)} · ${escapeHtml(c.plate)}</td>${user.role === "ADMIN" ? `<td>${c.userId ? "Vinculada" : "Sem vínculo"} <button class="btn" data-action="link-client" data-id="${escapeHtml(c.id)}">Alterar vínculo</button></td>` : ""}</tr>`).join("") || '<tr><td colspan="5">Nenhum cliente cadastrado.</td></tr>'}</tbody></table></div>`;
    } else if (view === "vehicles") {
      [vehicles, clients] = await Promise.all([api("/vehicles"), api("/clients")]);
      $("#app").innerHTML = `<div class="integration-head"><h2>Veículos</h2><button class="btn primary" data-action="new-vehicle">+ Novo veículo</button></div>
        <div class="integration-table"><table><thead><tr><th>Placa</th><th>Modelo</th><th>Marca / ano</th><th>Cliente</th><th></th></tr></thead><tbody>
        ${vehicles.map((v) => `<tr><td>${escapeHtml(v.plate)}</td><td>${escapeHtml(v.model)}</td><td>${escapeHtml(v.brand)} ${escapeHtml(v.modelYear)}</td><td>${escapeHtml(v.client.name)}</td><td><button class="btn" data-action="edit-vehicle" data-id="${escapeHtml(v.id)}">Editar</button></td></tr>`).join("") || '<tr><td colspan="5">Nenhum veículo cadastrado.</td></tr>'}</tbody></table></div>`;
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
      $("#app").innerHTML = `<div class="integration-head"><h2>Usuários</h2><button class="btn primary" data-action="new-user">+ Novo usuário</button></div><div class="integration-table"><table><thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Situação</th><th></th></tr></thead><tbody>${users.map((u) => `<tr><td>${escapeHtml(u.name)}</td><td>${escapeHtml(u.email)}</td><td>${escapeHtml(u.role)}</td><td>${u.active ? "Ativo" : "Inativo"}</td><td><button class="btn" data-action="edit-user" data-id="${escapeHtml(u.id)}">Editar</button> <button class="btn" data-action="reset-password" data-id="${escapeHtml(u.id)}">Redefinir senha</button> <button class="btn" data-action="toggle-user" data-id="${escapeHtml(u.id)}" ${u.id === user.id ? "disabled" : ""}>${u.active ? "Desativar" : "Ativar"}</button></td></tr>`).join("")}</tbody></table></div>`;
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
    <button class="btn" data-action="diagnosis" data-id="${id}">Diagnóstico</button>
    <button class="btn" data-action="planned" data-id="${id}">Peças e serviços previstos</button>
    <button class="btn" data-action="budget" data-id="${id}">Orçamento</button>
    <button class="btn" data-action="decision" data-id="${id}" data-approved="true">Aprovar</button>
    <button class="btn" data-action="decision" data-id="${id}" data-approved="false">Recusar</button>
    <button class="btn" data-action="checklist" data-id="${id}">Checklist</button></div>
    <p class="integration-note">Fotos e baixa automática de peças ainda não estão disponíveis nesta integração.</p>
    <button class="btn" data-action="back">Voltar</button></div>`;
}
async function plannedItems(orderId) {
  const [items, inventory] = await Promise.all([api("/orders/" + orderId + "/planned-items"), api("/stock")]);
  stock = inventory;
  $("#app").innerHTML = `<h2>Peças e serviços previstos</h2><p class="integration-note">Registrar previsões não reserva nem movimenta o estoque. Orçamento por itens e reserva ainda estão em implementação.</p>
    <div class="integration-table"><table><thead><tr><th>Tipo</th><th>Descrição</th><th>Quantidade</th><th>Valor unitário</th><th></th></tr></thead><tbody>
    ${items.map((x) => `<tr><td>${escapeHtml(x.itemType)}</td><td>${escapeHtml(x.description)}</td><td>${escapeHtml(x.quantity)}</td><td>${money(x.unitPrice)}</td><td><button class="btn" data-action="remove-planned" data-id="${escapeHtml(orderId)}" data-item="${escapeHtml(x.id)}">Remover</button></td></tr>`).join("") || '<tr><td colspan="5">Nenhum item previsto.</td></tr>'}</tbody></table></div>
    <div class="integration-actions"><button class="btn primary" data-action="add-planned" data-id="${escapeHtml(orderId)}">+ Adicionar item</button><button class="btn" data-action="order" data-id="${escapeHtml(orderId)}">Voltar à ordem</button></div>`;
}
$("#app").addEventListener("click", async (event) => {
  const b = event.target.closest("[data-action]"); if (!b) return;
  const action = b.dataset.action, id = b.dataset.id; $("#error").textContent = ""; b.disabled = true;
  try {
    if (action === "back") return await show(currentView);
    if (action === "new-vehicle" || action === "edit-vehicle") {
      const v = action === "edit-vehicle" ? vehicles.find((item) => item.id === id) : {};
      if (!clients.length) throw new Error("Cadastre um cliente antes do veículo.");
      return form(action === "new-vehicle" ? "Novo veículo" : "Editar veículo", `<input type="hidden" name="id" value="${escapeHtml(v.id || "")}"><label>Cliente<select name="clientId" required>${clients.map((c) => `<option value="${escapeHtml(c.id)}" ${v.client && v.client.id === c.id ? "selected" : ""}>${escapeHtml(c.name)}</option>`).join("")}</select></label><label>Placa<input name="plate" value="${escapeHtml(v.plate || "")}" required maxlength="255"></label><label>Modelo<input name="model" value="${escapeHtml(v.model || "")}" required maxlength="255"></label><label>Marca<input name="brand" value="${escapeHtml(v.brand || "")}" maxlength="255"></label><label>Ano<input name="modelYear" type="number" min="1886" max="2100" value="${escapeHtml(v.modelYear || "")}"></label>`, "vehicle");
    }
    if (action === "link-client") {
      const c = clients.find((item) => item.id === id);
      return form("Vincular conta ao cliente", `<p>Cliente: ${escapeHtml(c.name)}</p><input type="hidden" name="id" value="${escapeHtml(c.id)}"><label>Conta com perfil CLIENTE<select name="userId"><option value="">Sem conta vinculada</option>${users.filter((u) => u.role === "CLIENTE").map((u) => `<option value="${escapeHtml(u.id)}" ${u.id === c.userId ? "selected" : ""}>${escapeHtml(u.name)} · ${escapeHtml(u.email)}</option>`).join("")}</select></label>`, "link-client");
    }
    if (action === "new-user") return form("Novo usuário", '<label>Nome<input name="name" required maxlength="255"></label><label>E-mail<input name="email" type="email" required maxlength="255"></label><label>Senha inicial<input name="password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></label><label>Perfil<select name="role"><option value="MECANICO">Mecânico</option><option value="GERENTE">Gerente</option><option value="ADMIN">Administrador</option><option value="CLIENTE">Cliente (portal inicial)</option></select></label>', "create-user");
    if (action === "edit-user") {
      const u = users.find((item) => item.id === id);
      return form("Editar usuário", `<input type="hidden" name="id" value="${escapeHtml(u.id)}"><label>Nome<input name="name" value="${escapeHtml(u.name)}" required maxlength="255"></label><label>E-mail<input name="email" type="email" value="${escapeHtml(u.email)}" required maxlength="255"></label><label>Perfil<select name="role">${["MECANICO","GERENTE","ADMIN","CLIENTE"].map((role) => `<option value="${role}" ${u.role === role ? "selected" : ""}>${role}</option>`).join("")}</select></label>`, "update-user");
    }
    if (action === "reset-password") return form("Redefinir senha", `<input type="hidden" name="id" value="${escapeHtml(id)}"><label>Nova senha<input name="password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required></label>`, "reset-password");
    if (action === "new-client") return form("Novo cliente", '<label>Nome<input name="name" required></label><label>Telefone<input name="phone" required></label><label>E-mail<input name="email" type="email"></label><label>Placa<input name="plate"></label><label>Veículo<input name="vehicle"></label>', "client");
    if (action === "new-order") {
      if (!vehicles.length) throw new Error("Cadastre um veículo antes de criar uma ordem.");
      return form("Nova ordem de serviço", `<label>Veículo e proprietário<select name="vehicleId" required>${vehicles.map((v) => `<option value="${escapeHtml(v.id)}">${escapeHtml(v.plate)} · ${escapeHtml(v.model)} · ${escapeHtml(v.client.name)}</option>`).join("")}</select></label><label>Problema<textarea name="problem" required></textarea></label>`, "order");
    }
    if (action === "order") return orderDetail(id);
    if (action === "planned") return await plannedItems(id);
    if (action === "remove-planned") {
      await api("/orders/" + id + "/planned-items/" + b.dataset.item, request("DELETE"));
      return await plannedItems(id);
    }
    if (action === "add-planned") return form("Adicionar item previsto", `<input type="hidden" name="orderId" value="${escapeHtml(id)}">
      <label>Tipo<select name="itemType"><option value="PECA">Peça</option><option value="MAO_DE_OBRA">Mão de obra</option></select></label>
      <label>Peça cadastrada (somente para peça)<select name="stockItemId"><option value="">Selecione</option>${stock.map((s) => `<option value="${escapeHtml(s.id)}">${escapeHtml(s.name)} · saldo ${escapeHtml(s.quantity)} ${escapeHtml(s.unit)}</option>`).join("")}</select></label>
      <label>Descrição (somente mão de obra)<input name="description" maxlength="255"></label>
      <label>Quantidade<input name="quantity" type="number" min="0.01" step="0.01" required></label>
      <label>Valor unitário previsto<input name="unitPrice" type="number" min="0" step="0.01" required></label>`, "planned-item");
    if (action === "diagnosis") {
      const d = await api("/orders/" + id + "/diagnosis") || {};
      return form("Diagnóstico", `<input type="hidden" name="id" value="${escapeHtml(id)}"><label>Avarias<textarea name="damages" maxlength="2000">${escapeHtml(d.damages || "")}</textarea></label><label>Sintomas<textarea name="symptoms" maxlength="2000">${escapeHtml(d.symptoms || "")}</textarea></label><label>Observações técnicas<textarea name="observations" maxlength="2000">${escapeHtml(d.observations || "")}</textarea></label><label><span><input name="completed" type="checkbox" style="width:auto" ${d.completed ? "checked" : ""}> Diagnóstico concluído</span></label>`, "diagnosis");
    }
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
    if (action === "planned-item") {
      await api("/orders/" + d.orderId + "/planned-items", request("POST", {
        itemType: d.itemType, stockItemId: d.itemType === "PECA" ? (d.stockItemId || null) : null,
        description: d.itemType === "MAO_DE_OBRA" ? d.description : null,
        quantity: Number(d.quantity), unitPrice: Number(d.unitPrice),
      }));
      return await plannedItems(d.orderId);
    }
    if (action === "diagnosis") await api("/orders/" + d.id + "/diagnosis", request("PUT", { damages: d.damages, symptoms: d.symptoms, observations: d.observations, completed: Boolean(d.completed) }));
    else if (action === "vehicle") await api("/vehicles" + (d.id ? "/" + d.id : ""), request(d.id ? "PUT" : "POST", { clientId: d.clientId, plate: d.plate, model: d.model, brand: d.brand, modelYear: d.modelYear ? Number(d.modelYear) : null }));
    else if (action === "link-client") await api("/clients/" + d.id + "/user", request("PATCH", { userId: d.userId || null }));
    else if (action === "create-user") await api("/users", request("POST", d));
    else if (action === "update-user") await api("/users/" + d.id, request("PUT", { name: d.name, email: d.email, role: d.role }));
    else if (action === "reset-password") await api("/users/" + d.id + "/password", request("PATCH", { password: d.password }));
    else if (action === "client") await api("/clients", request("POST", d));
    else if (action === "order") await api("/orders", request("POST", d));
    else if (action === "budget") await api("/orders/" + d.id + "/budget", request("PATCH", { value: Number(d.value) }));
    else if (action === "checklist") await api("/orders/" + d.id + "/checklist", request("PATCH", { checklist: [...f.querySelectorAll('[name="check"]')].map((x) => x.checked) }));
    else if (action === "movement") await api("/stock/movements", request("POST", { ...d, quantity: Number(d.quantity) }));
    else if (action === "stock") await api("/stock/" + d.id, request("PUT", { name: d.name, code: d.code, quantity: Number(d.quantity), reorderPoint: Number(d.reorderPoint), unit: d.unit }));
    await show(currentView);
  } catch (error) { showError(error); } finally { button.disabled = false; }
});
if (token) start().catch((error) => { logout(); $("#loginError").textContent = error.message; });
