// Ligação dos elementos da tela aos eventos (bind), filtros de busca
// e o dispatcher central de ações dos botões (data-a).
function bind() {
  $$(".nav-item").forEach(
    (x) =>
      (x.onclick = () => {
        view = x.dataset.view;
        $("#sidebar").classList.remove("open");
        render();
      }),
  );
  $$("[data-v]").forEach(
    (x) =>
      (x.onclick = () => {
        view = x.dataset.v;
        render();
      }),
  );
  $$("[data-a]").forEach(
    (x) =>
      (x.onclick = (ev) => {
        ev.stopPropagation();
        act(x.dataset.a, x);
      }),
  );
  const q = $("#q");
  if (q) q.oninput = filterOrders;
  const sf = $("#sf");
  if (sf) sf.onchange = filterOrders;
  const cq = $("#cq");
  if (cq) cq.oninput = searchClients;
  const aq = $("#aq");
  if (aq) aq.oninput = filterAudit;
}

function filterOrders() {
  let q = ($("#q")?.value || "").toLowerCase(),
    s = $("#sf")?.value || "";
  $$("#ot tr").forEach(
    (r) => (r.style.display = r.dataset.s.includes(q) && (!s || r.dataset.st === s) ? "" : "none"),
  );
}

function searchClients() {
  let q = $("#cq").value.toLowerCase();
  $("#cr").innerHTML =
    db.clients
      .filter((c) => (c[0] + c[1] + c[3]).toLowerCase().includes(q))
      .map((c) => `<div class="rule-box"><b>${c[0]}</b>${c[1]} · ${c[3]} · ${c[4]}</div>`)
      .join("") || '<div class="empty">Nenhum resultado.</div>';
}

function filterAudit() {
  let q = $("#aq").value.toLowerCase();
  $$("#at tr").forEach(
    (r) => (r.style.display = r.textContent.toLowerCase().includes(q) ? "" : "none"),
  );
}

function act(a, e) {
  let id = e.dataset.id,
    i = +e.dataset.i;
  if (a === "close") return $("#modalBackdrop").classList.add("hidden");
  if (a === "new") return newOrder();
  if (a === "newclient") return newClient();
  if (a === "move") return moveStock();
  if (a === "open") return openOrder(id);
  if (a === "advance") return advance(id);
  if (a === "retreat") return retreat(id);
  if (a === "addphoto") return triggerPhotoUpload(id);
  if (a === "quality") return quality(id);
  if (a === "budget") return budget(id);
  if (a === "kpilist") return kpiList(e.dataset.kind);
  if (a === "approve" || a === "reject") {
    let o = db.orders.find((x) => x.id === id);
    if (a === "approve") {
      o.approved = true;
      o.stage = 3;
      log("Orçamento aprovado", id, "Decisão registrada.");
      toastMsg("Orçamento aprovado. OS → Aguardando Início.");
    } else {
      o.approved = false;
      o.stage = 0;
      o.value = 0;
      o.reserved = false;
      log("Orçamento recusado", id, "Serviço encerrado; reserva estornada.");
      toastMsg("Orçamento recusado e reservas estornadas.");
    }
    save();
    render();
    return;
  }
  if (a === "deliver") {
    let o = db.orders.find((x) => x.id === id);
    if (o.stage !== 7 || !o.check.every(Boolean))
      return toastMsg("Entrega bloqueada: checklist não aprovado.");
    o.parts.forEach((p) => {
      let s = db.stock.find((x) => x[0] === p[0]);
      if (s) s[2] = Math.max(0, s[2] - p[1]);
    });
    o.reserved = false;
    log("Encerramento / entrega", id, "Baixa definitiva das peças registrada.");
    save();
    closeModal();
    toastMsg("Entrega registrada e baixa definitiva realizada.");
    render();
    return;
  }
  if (a === "create") {
    let c = $("#nc").value.trim(),
      pl = $("#npl").value.trim().toUpperCase(),
      car = $("#ncar").value.trim(),
      pr = $("#nprob").value.trim();
    if (!c || !pl || !car || !pr) return toastMsg("Preencha cliente, placa, veículo e problema.");
    if (db.orders.some((o) => o.plate === pl && o.stage < 7))
      return toastMsg("Este veículo já possui uma OS ativa.");
    let id = "OS-" + (1052 + db.orders.length);
    db.orders.unshift({
      id,
      plate: pl,
      car,
      client: c,
      problem: pr,
      stage: 0,
      value: 0,
      days: 0,
      owner: "MA",
      approved: false,
      reserved: false,
      progress: 5,
      parts: [],
      check: [0, 0, 0, 0, 0],
      photos: 0,
    });
    log("Criação de OS", id, "Ordem criada em Recepção / Check-in.");
    save();
    closeModal();
    toastMsg(`${id} criada em Recepção / Check-in.`);
    view = "orders";
    render();
    return;
  }
  if (a === "createclient") {
    let n = $("#cn").value.trim(),
      p = $("#cp").value.trim();
    if (!n || !p) return toastMsg("Nome e telefone são obrigatórios.");
    db.clients.push([n, p, $("#ce").value, $("#cpl").value.toUpperCase(), $("#cc").value]);
    log("Cadastro de cliente", "Cliente", "Novo cliente cadastrado.");
    save();
    closeModal();
    toastMsg("Cliente cadastrado.");
    render();
    return;
  }
  if (a === "savemove") {
    let name = $("#mi").value.trim(),
      type = $("#mt").value,
      q = +$("#mq").value,
      unit = $("#mu").value.trim() || "un",
      reason = $("#mr").value.trim() || "sem motivo";
    if (!name || !q || q <= 0) return toastMsg("Informe o item e uma quantidade válida.");
    let s = db.stock.find((x) => x[0].toLowerCase() === name.toLowerCase());
    if (!s) {
      if (type === "out") return toastMsg("Item não encontrado no estoque para dar saída.");
      s = [name, "NOVO-" + String(db.stock.length + 1).padStart(3, "0"), 0, 5, unit];
      db.stock.push(s);
      log("Item de estoque cadastrado", s[1], `${name} adicionado via movimentação.`);
    }
    if (type === "out" && s[2] < q) return toastMsg("Saída bloqueada: saldo insuficiente.");
    s[2] += type === "in" ? q : -q;
    log(type === "in" ? "Entrada de estoque" : "Saída de estoque", s[1], `${q}${s[4]} · ${reason}`);
    save();
    closeModal();
    toastMsg("Movimentação registrada.");
    render();
    return;
  }
  if (a === "editstock") return editStockItem(i);
  if (a === "saveeditstock") {
    let s = db.stock[i],
      name = $("#en").value.trim(),
      code = $("#ec").value.trim();
    if (!name || !code) return toastMsg("Nome e código são obrigatórios.");
    s[0] = name;
    s[1] = code;
    s[2] = Math.max(0, +$("#eq").value);
    s[3] = Math.max(0, +$("#ep").value);
    s[4] = $("#eu").value.trim() || "un";
    log("Item de estoque editado", s[1], `Saldo: ${s[2]}${s[4]} · Ponto: ${s[3]}${s[4]}`);
    save();
    closeModal();
    toastMsg("Item atualizado.");
    render();
    return;
  }
  if (a === "savequality") {
    let o = db.orders.find((x) => x.id === id);
    o.check = $$("[data-check]").map((x) => (x.checked ? 1 : 0));
    if (o.check.every(Boolean)) {
      o.stage = 7;
      o.progress = 100;
      db.notifications.unshift([
        "Veículo pronto",
        `${id} passou no controle de qualidade.`,
        "agora",
        "green",
        0,
        { view: "orders", orderId: id },
      ]);
      log("Controle de qualidade aprovado", id, "Checklist concluído.");
    } else log("Atualização de checklist", id, `${o.check.filter(Boolean).length}/5 itens.`);
    save();
    closeModal();
    toastMsg(
      o.check.every(Boolean)
        ? "Checklist aprovado. Veículo pronto para entrega."
        : "Checklist salvo com pendências.",
    );
    render();
    return;
  }
  if (a === "savebudget") {
    let o = db.orders.find((x) => x.id === id),
      v = +$("#bv").value;
    if (v <= 0) return toastMsg("Orçamento de R$ 0 não pode avançar para aprovação.");
    o.value = v;
    log("Orçamento atualizado", id, `Valor: ${money(v)}.`);
    save();
    closeModal();
    toastMsg("Orçamento salvo.");
    render();
    return;
  }
  if (a === "readall") {
    db.notifications.forEach((n) => (n[4] = 1));
    save();
    render();
    return;
  }
  if (a === "read") {
    db.notifications[i][4] = 1;
    save();
    render();
    return;
  }
  if (a === "opennotif") {
    let n = db.notifications[i];
    n[4] = 1;
    let t = n[5];
    if (t && t.view) view = t.view;
    save();
    render();
    let cta =
      t && t.orderId
        ? `<button class="btn primary" data-a="open" data-id="${t.orderId}">Abrir ${t.orderId}</button>`
        : "";
    openModal(
      n[0],
      `<div class="rule-box"><b>${n[2]}</b>${n[1]}</div><div class="modal-actions">${cta}<button class="btn" data-a="close">Fechar</button></div>`,
    );
    return;
  }
  if (a === "toggleuser") {
    db.users[i][4] = db.users[i][4] ? 0 : 1;
    log(db.users[i][4] ? "Usuário reativado" : "Usuário desativado", "Usuário", db.users[i][0]);
    save();
    render();
    return;
  }
  if (a === "editperms") return editPermissions(i);
  if (a === "saveperms") {
    let u = db.users[i],
      p = {};
    $$("[data-perm]").forEach((c) => (p[c.dataset.perm] = c.checked ? 1 : 0));
    u[5] = p;
    log("Permissões atualizadas", "Usuário", u[0]);
    save();
    closeModal();
    toastMsg("Permissões salvas para " + u[0] + ".");
    render();
    return;
  }
  if (a === "export" || a === "exportaudit") {
    let rows =
      a === "export"
        ? [
            [
              "OS",
              "Cliente",
              "Placa",
              "Veículo",
              "Problema",
              "Etapa",
              "Dias no fluxo",
              "Progresso (%)",
              "Valor",
              "Orçamento aprovado",
              "Peças reservadas",
              "Responsável",
              "Checklist",
              "Fotos anexadas",
            ],
            ...db.orders.map((o) => [
              o.id,
              o.client,
              o.plate,
              o.car,
              o.problem,
              stages[o.stage],
              o.days,
              o.progress,
              o.value,
              o.approved ? "Sim" : "Não",
              o.reserved ? "Sim" : "Não",
              o.owner,
              `${o.check.filter(Boolean).length}/${o.check.length}`,
              o.photos || 0,
            ]),
          ]
        : [["Data", "Usuário", "Ação", "Alvo", "Detalhe"], ...db.audit];
    let csv = rows
        .map((r) => r.map((x) => '"' + String(x).replaceAll('"', '""') + '"').join(";"))
        .join("\n"),
      u = URL.createObjectURL(new Blob(["\ufeff" + csv], { type: "text/csv" })),
      x = document.createElement("a");
    x.href = u;
    x.download = "armenguecar.csv";
    x.click();
    URL.revokeObjectURL(u);
    toastMsg("CSV exportado.");
    return;
  }
  if (a === "reset") {
    db = structuredClone(seed);
    save();
    toastMsg("Dados restaurados.");
    render();
    return;
  }
}
