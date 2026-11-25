// Renderização das telas (views) do sistema e da navegação principal.
function stageBadge(i) {
  let c =
    i === 7 || i === 6
      ? "green"
      : i === 5
        ? "red"
        : i === 2 || i === 3
          ? "amber"
          : i === 4
            ? "cyan"
            : "blue";
  return `<span class="badge ${c}">${stages[i]}</span>`;
}

function head(e, t, d, a = "") {
  return `<div class="page-head"><div><div class="eyebrow">${e}</div><h1>${t}</h1><p>${d}</p></div><div class="actions">${a}</div></div>`;
}

function render() {
  const r = roles[role];
  $("#pageTitle").textContent =
    {
      dashboard: "Visão geral",
      kanban: "Kanban operacional",
      orders: "Ordens de serviço",
      clients: "Clientes e veículos",
      stock: "Estoque",
      reports: "Indicadores",
      notifications: "Notificações",
      portal: "Portal do cliente",
      users: "Usuários",
      audit: "Auditoria",
      settings: "Configurações",
    }[view] || "Visão geral";
  $("#currentUserName").textContent = r[0];
  $("#currentUserRole").textContent = r[1];
  $("#roleAvatar").textContent = r[2];
  $("#roleAvatar").className = "avatar " + r[3];
  $$(".nav-item").forEach((x) => x.classList.toggle("active", x.dataset.view === view));
  $$('[data-role="admin"]').forEach((x) => (x.style.display = role === "admin" ? "flex" : "none"));
  $("#stockCount").textContent = db.stock.filter((x) => x[2] < x[3]).length;
  $("#notifCount").textContent = db.notifications.filter((x) => !x[4]).length;
  $("#app").innerHTML = (
    {
      dashboard,
      kanban,
      orders,
      clients,
      stock,
      reports,
      notifications,
      portal,
      users,
      audit,
      settings,
    }[view] || dashboard
  )();
  bind();
  save();
}

function dashboard() {
  let c = stages.map((_, i) => db.orders.filter((o) => o.stage === i).length),
    low = db.stock.filter((x) => x[2] < x[3]).length;
  return (
    head(
      "OPERAÇÃO HOJE",
      "Tudo sob controle, " + roles[role][0].split(" ")[0] + ".",
      "Acompanhe veículos, gargalos e decisões que precisam de atenção.",
      `<button class="btn primary" data-a="new">+ Nova OS</button><button class="btn" data-v="kanban">Abrir Kanban</button>`,
    ) +
    `<div class="kpis"><div class="card kpi"><div class="kpi-top">Em andamento</div><strong>${db.orders.filter((o) => o.stage > 0 && o.stage < 7).length}</strong><span class="trend">fluxo ativo</span></div><div class="card kpi"><div class="kpi-top">Aguardando decisão</div><strong>${c[2]}</strong><span class="muted">orçamento pendente</span></div><div class="card kpi"><div class="kpi-top">Estoque crítico</div><strong>${low}</strong><span class="trend" style="color:var(--red)">necessitam reposição</span></div><div class="card kpi"><div class="kpi-top">Prontas para entrega</div><strong>${c[7]}</strong><span class="muted">checklist aprovado</span></div></div><div class="card" style="margin-bottom:16px"><div class="card-head"><h2>Fluxo operacional</h2><small>8 ETAPAS</small></div><div class="card-body"><div class="status-strip">${stages.map((s, i) => `<div class="status-chip"><b>${c[i]}</b><span>${s}</span><div class="bar"><i style="width:${Math.min(100, c[i] * 35 + 10)}%"></i></div></div>`).join("")}</div></div></div><div class="grid-2"><div class="card"><div class="card-head"><h2>Ordens que exigem atenção</h2></div><div class="card-body list">${
      db.orders
        .filter((o) => [2, 5, 6].includes(o.stage))
        .map(orderRow)
        .join("") || '<div class="empty">Nenhuma atenção pendente.</div>'
    }</div></div><div class="card"><div class="card-head"><h2>Regras de integridade</h2><small>AUTOMÁTICAS</small></div><div class="card-body"><div class="rule-box"><b>01 · ORÇAMENTO</b>R$ 0 não avança para aprovação.</div><div class="rule-box"><b>02 · EXECUÇÃO</b>Aprovação + peças disponíveis/reservadas.</div><div class="rule-box"><b>03 · QUALIDADE</b>Checklist aprovado antes da liberação.</div><div class="rule-box"><b>04 · ESTOQUE</b>Baixa definitiva no encerramento.</div></div></div></div>`
  );
}

function orderRow(o) {
  return `<div class="list-row" data-a="open" data-id="${o.id}"><div class="list-main"><div class="car-thumb">🚗</div><div><strong>${o.id} · ${o.plate}</strong><span>${o.car} · ${o.client}</span></div></div><span class="badge ${stages[o.stage] === "Aguardando Peças" ? "red" : "blue"}">${stages[o.stage]}</span></div>`;
}

function kanban() {
  return (
    head(
      "ACOMPANHAMENTO VISUAL",
      "Kanban operacional",
      "As 8 etapas oficiais do fluxo. As transições são validadas pelas regras de negócio.",
      `<button class="btn primary" data-a="new">+ Nova OS</button>`,
    ) +
    `<div class="card"><div class="card-body"><div class="kanban">${stages
      .map(
        (s, i) =>
          `<div class="kanban-col"><div class="kanban-title"><strong>${s}</strong><span>${db.orders.filter((o) => o.stage === i).length}</span></div>${
            db.orders
              .filter((o) => o.stage === i)
              .map(
                (o) =>
                  `<div class="kanban-card" data-a="open" data-id="${o.id}"><span class="tag">${o.id} · ${o.plate}</span><h4>${o.car}</h4><p>${o.problem}</p><span class="badge ${i === 5 ? "red" : i === 7 ? "green" : "blue"}">${o.days} dia(s)</span><div class="kanban-foot"><div class="mini-avatar">${o.owner}</div><button class="btn sm" data-a="advance" data-id="${o.id}">${i < 7 ? "Avançar →" : "Liberar"}</button></div></div>`,
              )
              .join("") || '<div class="empty">—</div>'
          }</div>`,
      )
      .join("")}</div></div></div>`
  );
}

function orders() {
  return (
    head(
      "OPERAÇÃO",
      "Ordens de serviço",
      "Pesquise por placa, cliente ou identificador da OS.",
      `<button class="btn primary" data-a="new">+ Nova OS</button>`,
    ) +
    `<div class="card"><div class="card-body"><div class="filters"><input class="field" id="q" placeholder="⌕ Buscar placa, OS ou cliente"><select class="field" id="sf"><option value="">Todos os status</option>${stages.map((s, i) => `<option value="${i}">${s}</option>`).join("")}</select></div><div class="table-wrap"><table class="data-table"><thead><tr><th>OS</th><th>Cliente / veículo</th><th>Problema</th><th>Status</th><th>Valor</th><th>Prazo</th><th></th></tr></thead><tbody id="ot">${db.orders.map(orderTable).join("")}</tbody></table></div></div></div>`
  );
}

function orderTable(o) {
  return `<tr data-s="${(o.id + o.plate + o.client + o.car).toLowerCase()}" data-st="${o.stage}"><td><strong>${o.id}</strong><div class="muted">${o.plate}</div></td><td><strong>${o.client}</strong><div class="muted">${o.car}</div></td><td>${o.problem}</td><td>${stageBadge(o.stage)}</td><td>${money(o.value)}</td><td>${o.days}d</td><td><button class="btn sm" data-a="open" data-id="${o.id}">Abrir</button></td></tr>`;
}

function clients() {
  return (
    head(
      "CADASTROS / RECEPÇÃO",
      "Clientes e veículos",
      "Cadastro e localização rápida de clientes por telefone ou placa.",
      `<button class="btn primary" data-a="newclient">+ Novo cliente</button>`,
    ) +
    `<div class="grid-2"><div class="card"><div class="card-head"><h2>Clientes cadastrados</h2><small>${db.clients.length} REGISTROS</small></div><div class="card-body"><div class="table-wrap"><table class="data-table"><thead><tr><th>CLIENTE</th><th>CONTATO</th><th>VEÍCULO</th></tr></thead><tbody>${db.clients.map((c, i) => `<tr data-a="client" data-i="${i}"><td><strong>${c[0]}</strong><div class="muted">${c[2]}</div></td><td>${c[1]}</td><td><b>${c[3]}</b><div class="muted">${c[4]}</div></td></tr>`).join("")}</tbody></table></div></div></div><div class="card"><div class="card-head"><h2>Busca operacional</h2></div><div class="card-body"><input class="field" id="cq" style="width:100%" placeholder="Nome, telefone ou placa"><div id="cr" style="margin-top:12px"></div></div></div></div>`
  );
}

function stock() {
  return (
    head(
      "SUPRIMENTOS",
      "Estoque e peças",
      "Saldo, ponto de reposição, reservas e movimentações.",
      `<button class="btn primary" data-a="move">+ Movimentar estoque</button>`,
    ) +
    `<div class="card"><div class="card-head"><h2>Itens cadastrados</h2><small>${db.stock.length} ITENS</small></div><div class="card-body"><div class="table-wrap"><table class="data-table"><thead><tr><th>ITEM</th><th>SALDO</th><th>PONTO</th><th>STATUS</th><th></th></tr></thead><tbody>${db.stock.map((x, i) => `<tr><td><strong>${x[0]}</strong><div class="muted">${x[1]}</div></td><td>${x[2]} ${x[4]}</td><td>${x[3]} ${x[4]}</td><td>${x[2] < x[3] ? '<span class="stock-low">Abaixo do ponto</span>' : '<span class="stock-ok">Disponível</span>'}</td><td><button class="btn sm" data-a="editstock" data-i="${i}">Editar item</button></td></tr>`).join("")}</tbody></table></div></div></div>`
  );
}

function reports() {
  let approved = db.orders.filter((o) => o.approved).reduce((a, o) => a + o.value, 0),
    done = db.orders.filter((o) => o.stage === 7),
    late = db.orders.filter((o) => o.stage < 7 && o.days >= 5);
  return (
    head(
      "GESTÃO",
      "Indicadores da oficina",
      "Produtividade, faturamento, tempos e gargalos.",
      `<button class="btn" data-a="export">↓ Exportar CSV detalhado</button>`,
    ) +
    `<div class="kpis"><div class="card kpi"><div class="kpi-top">Faturamento aprovado</div><strong>${money(approved)}</strong><span class="muted">orçamentos aprovados</span></div><div class="card kpi"><div class="kpi-top">Tempo médio</div><strong>5,2d</strong><span class="muted">ciclo estimado</span></div><div class="card kpi kpi-click" data-a="kpilist" data-kind="done"><div class="kpi-top">Concluídas</div><strong>${done.length}</strong><span class="muted">prontas para entrega · ver lista</span></div><div class="card kpi kpi-click" data-a="kpilist" data-kind="late"><div class="kpi-top">Atrasadas</div><strong>${late.length}</strong><span class="trend" style="color:var(--red)">acompanhar · ver lista</span></div></div><div class="grid-2"><div class="card"><div class="card-head"><h2>Ordens por etapa</h2></div><div class="card-body"><div class="mini-bars">${stages
      .map((s, i) => {
        let n = db.orders.filter((o) => o.stage === i).length;
        return `<div><b>${n}</b><i style="height:${Math.max(5, n * 35)}px"></i><span>${i + 1}</span></div>`;
      })
      .join(
        "",
      )}</div></div></div><div class="card"><div class="card-head"><h2>Gargalos</h2></div><div class="card-body">${[2, 4, 5].map((i) => `<div class="list-row"><span>${stages[i]}</span><b>${db.orders.filter((o) => o.stage === i).length}</b></div>`).join("")}</div></div></div>`
  );
}

function kpiList(kind) {
  let list =
      kind === "done"
        ? db.orders.filter((o) => o.stage === 7)
        : db.orders.filter((o) => o.stage < 7 && o.days >= 5),
    title = kind === "done" ? "Ordens concluídas" : "Ordens atrasadas";
  openModal(
    title,
    `<div class="table-wrap"><table class="data-table"><thead><tr><th>OS</th><th>Cliente / veículo</th><th>Etapa</th><th>Dias no fluxo</th><th>Valor</th><th></th></tr></thead><tbody>${list.map((o) => `<tr><td><strong>${o.id}</strong><div class="muted">${o.plate}</div></td><td>${o.client}<div class="muted">${o.car}</div></td><td>${stageBadge(o.stage)}</td><td>${o.days}d</td><td>${money(o.value)}</td><td><button class="btn sm" data-a="open" data-id="${o.id}">Abrir</button></td></tr>`).join("") || '<tr><td colspan="6" class="empty">Nenhum registro.</td></tr>'}</tbody></table></div><div class="modal-actions"><button class="btn" data-a="close">Fechar</button></div>`,
  );
}

function notifications() {
  return (
    head(
      "COMUNICAÇÃO",
      "Central de notificações",
      "Decisões, bloqueios, início de execução e veículos prontos.",
      `<button class="btn" data-a="readall">Marcar todas como lidas</button>`,
    ) +
    `<div class="card"><div class="card-body">${db.notifications.map((n, i) => `<div class="notification notification-click" style="opacity:${n[4] ? "0.5" : "1"}" data-a="opennotif" data-i="${i}"><i class="n-dot" style="background:var(--${n[3] === "red" ? "red" : n[3] === "green" ? "green" : "royal"})"></i><div><h4>${n[0]}</h4><p>${n[1]}</p><time>${n[2]}</time></div><button class="btn sm" data-a="read" data-i="${i}">${n[4] ? "Lida" : "Marcar lida"}</button></div>`).join("")}</div></div>`
  );
}

function portal() {
  let o =
    role === "cliente"
      ? db.orders.find((x) => x.client === "Eduardo Ramos")
      : db.orders.find((x) => x.client === "Rafael Souza") || db.orders[0];
  return (
    head(
      "ÁREA EXTERNA / CLIENTE",
      "Portal do cliente",
      "Acesso limitado ao veículo e à ordem autorizada.",
      `<button class="btn" data-a="open" data-id="${o.id}">Ver detalhes</button>`,
    ) +
    `<div class="card"><div class="card-head"><div><h2>${o.car} · ${o.plate}</h2><small>${o.client}</small></div>${stageBadge(o.stage)}</div><div class="card-body"><div class="portal-track">${stages.map((s, i) => `<div class="portal-step ${i < o.stage ? "done" : i === o.stage ? "current" : ""}"><i></i>${i + 1}. ${s.split(" ")[0]}</div>`).join("")}</div><div class="progress" style="margin-top:14px"><i style="width:${o.progress}%"></i></div><div class="grid-2" style="margin-top:18px"><div><h3>Orçamento</h3><div class="rule-box"><b>${money(o.value)}</b>${o.approved ? "Aprovado" : "Aguardando decisão"}</div>${o.stage === 2 ? `<div class="detail-actions"><button class="btn primary" data-a="approve" data-id="${o.id}">Aprovar</button><button class="btn" data-a="reject" data-id="${o.id}">Recusar</button></div>` : ""}</div><div><h3>Evidências</h3><div class="evidence-grid">${evidenceTiles(o, 5)}<div class="evidence-photo evidence-add" data-a="addphoto" data-id="${o.id}">+ Anexar<br>foto</div></div></div></div></div></div>`
  );
}

function users() {
  if (role !== "admin") return denied("Usuários");
  return (
    head(
      "ADMINISTRAÇÃO / RBAC",
      "Usuários e permissões",
      "Gerencie contas, perfis, status de acesso e permissões finas por módulo.",
      `<button class="btn primary" data-a="newuser">+ Novo usuário</button>`,
    ) +
    `<div class="card"><div class="table-wrap"><table class="data-table"><thead><tr><th>USUÁRIO</th><th>PERFIL</th><th>STATUS</th><th></th></tr></thead><tbody>${db.users.map((u, i) => `<tr><td><strong>${u[0]}</strong></td><td>${u[1]}</td><td><span class="badge ${u[4] ? "green" : "red"}">${u[4] ? "Ativo" : "Inativo"}</span></td><td class="row-actions"><button class="btn sm primary" data-a="editperms" data-i="${i}">Editar permissões</button><button class="btn sm" data-a="toggleuser" data-i="${i}">${u[4] ? "Desativar" : "Reativar"}</button></td></tr>`).join("")}</tbody></table></div></div>`
  );
}

function editPermissions(i) {
  let u = db.users[i],
    p = u[5] || {};
  openModal(
    `Permissões · ${u[0]}`,
    `<div class="perm-head"><div class="avatar avatar-purple">${u[3]}</div><div><strong>${u[0]}</strong><span class="muted">${u[1]}</span></div></div><p class="muted">Marque exatamente os módulos que este usuário pode acessar, independente do perfil geral.</p><div class="permission-grid">${permModules.map(([k, label]) => `<label class="check-row"><input type="checkbox" data-perm="${k}" ${p[k] ? "checked" : ""}><span>${label}</span></label>`).join("")}</div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="saveperms" data-i="${i}">Salvar permissões</button></div>`,
  );
}

function audit() {
  if (role !== "admin") return denied("Auditoria");
  return (
    head(
      "RASTREABILIDADE",
      "Log de auditoria",
      "Criação, alteração, aprovação, bloqueio, baixa e encerramento.",
      `<button class="btn" data-a="exportaudit">↓ Exportar</button>`,
    ) +
    `<div class="card"><div class="card-body"><div class="filters"><input class="field" id="aq" placeholder="Filtrar usuário, ação ou OS"></div><div class="table-wrap"><table class="data-table"><thead><tr><th>DATA</th><th>USUÁRIO</th><th>AÇÃO</th><th>ALVO</th><th>DETALHE</th></tr></thead><tbody id="at">${db.audit.map((a) => `<tr><td>${a[0]}</td><td>${a[1]}</td><td>${a[2]}</td><td><b>${a[3]}</b></td><td>${a[4]}</td></tr>`).join("")}</tbody></table></div></div></div>`
  );
}

function settings() {
  return (
    head("SISTEMA", "Configurações", "Preferências e regras de integridade do protótipo.") +
    `<div class="grid-2"><div class="card"><div class="card-head"><h2>Regras de processo</h2></div><div class="card-body">${["Bloquear orçamento com R$ 0", "Exigir aprovação antes da execução", "Bloquear execução sem peças", "Exigir checklist de qualidade", "Baixa definitiva no encerramento", "Registrar alterações na auditoria"].map((x) => `<div class="check"><input type="checkbox" checked><span>${x}</span></div>`).join("")}</div></div><div class="card"><div class="card-head"><h2>Escopo</h2></div><div class="card-body"><div class="rule-box">Fornecedor externo de peças permanece fora do escopo.</div><div class="rule-box">Pagamento online permanece fora do escopo.</div><div class="rule-box">Notificações estão simuladas no frontend.</div><button class="btn danger" data-a="reset">Restaurar dados de demonstração</button></div></div></div>`
  );
}

function denied(n) {
  return (
    head("ACESSO RESTRITO", n, "Seu perfil não possui permissão para esta área.") +
    `<div class="card"><div class="empty"><strong>Permissão necessária</strong><span>Troque o perfil pelo botão ↕.</span></div></div>`
  );
}
