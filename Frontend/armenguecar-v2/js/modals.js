// Infraestrutura de modais (abrir/fechar/toast) e os formulários usados
// em cada modal (nova OS, novo cliente, estoque, checklist, orçamento).
function openModal(t, b) {
  $("#modal").innerHTML =
    `<div class="modal-head"><h2>${t}</h2><button class="modal-close" data-a="close">×</button></div><div class="modal-body">${b}</div>`;
  $("#modalBackdrop").classList.remove("hidden");
  bind();
}

function closeModal() {
  $("#modalBackdrop").classList.add("hidden");
}

function toastMsg(m) {
  let t = $("#toast");
  t.textContent = m;
  t.classList.remove("hidden");
  clearTimeout(window.tt);
  window.tt = setTimeout(() => t.classList.add("hidden"), 2600);
}

function log(a, target, d) {
  db.audit.unshift([
    new Date().toLocaleString("pt-BR", { hour12: false }),
    roles[role][0],
    a,
    target,
    d,
  ]);
  save();
}

function newOrder() {
  openModal(
    "Abrir nova Ordem de Serviço",
    `<div class="form-grid"><div class="form-field"><label>Cliente</label><input id="nc"></div><div class="form-field"><label>Telefone</label><input id="np"></div><div class="form-field"><label>Placa</label><input id="npl"></div><div class="form-field"><label>Veículo</label><input id="ncar"></div><div class="form-field form-full"><label>Problema relatado</label><textarea id="nprob"></textarea></div></div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="create">Criar OS</button></div>`,
  );
}

function newClient() {
  openModal(
    "Novo cliente",
    `<div class="form-grid"><div class="form-field"><label>Nome</label><input id="cn"></div><div class="form-field"><label>Telefone</label><input id="cp"></div><div class="form-field"><label>E-mail</label><input id="ce"></div><div class="form-field"><label>Placa</label><input id="cpl"></div><div class="form-field"><label>Veículo</label><input id="cc"></div></div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="createclient">Salvar</button></div>`,
  );
}

function moveStock() {
  openModal(
    "Movimentar estoque",
    `<div class="form-grid"><div class="form-field form-full"><label>Item</label><input id="mi" list="mi-list" placeholder="Digite para buscar ou cadastrar um item novo" autocomplete="off"><datalist id="mi-list">${db.stock.map((x) => `<option value="${x[0]}">`).join("")}</datalist><small class="muted">Não achou o item? Só digitar o nome — ele é cadastrado automaticamente numa entrada.</small></div><div class="form-field"><label>Tipo</label><select id="mt"><option value="in">Entrada</option><option value="out">Saída</option></select></div><div class="form-field"><label>Quantidade</label><input id="mq" type="number" min="1" value="1"></div><div class="form-field"><label>Unidade (se for item novo)</label><input id="mu" value="un"></div><div class="form-field"><label>Motivo</label><input id="mr"></div></div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="savemove">Registrar</button></div>`,
  );
}

function editStockItem(i) {
  let s = db.stock[i];
  openModal(
    "Editar item de estoque",
    `<div class="form-grid"><div class="form-field"><label>Nome</label><input id="en" value="${s[0]}"></div><div class="form-field"><label>Código</label><input id="ec" value="${s[1]}"></div><div class="form-field"><label>Saldo atual</label><input id="eq" type="number" min="0" value="${s[2]}"></div><div class="form-field"><label>Ponto de reposição</label><input id="ep" type="number" min="0" value="${s[3]}"></div><div class="form-field"><label>Unidade</label><input id="eu" value="${s[4]}"></div></div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="saveeditstock" data-i="${i}">Salvar alterações</button></div>`,
  );
}

function quality(id) {
  let o = db.orders.find((x) => x.id === id),
    items = [
      "Estrutura e alinhamento",
      "Acabamento da funilaria",
      "Pintura e tonalidade",
      "Montagem e componentes",
      "Limpeza e entrega técnica",
    ];
  openModal(
    `${o.id} · Controle de qualidade`,
    `<div class="checklist">${items.map((x, i) => `<label class="check-row"><input type="checkbox" data-check="${i}" ${o.check[i] ? "checked" : ""}><span>${x}</span></label>`).join("")}</div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="savequality" data-id="${id}">Salvar checklist</button></div>`,
  );
}

function budget(id) {
  let o = db.orders.find((x) => x.id === id);
  openModal(
    `${o.id} · Orçamento`,
    `<div class="form-grid"><div class="form-field"><label>Valor</label><input id="bv" type="number" value="${o.value}"></div><div class="form-field"><label>Decisão</label><select id="bd"><option ${o.approved ? "selected" : ""}>Aprovado</option><option ${!o.approved ? "selected" : ""}>Aguardando aprovação</option></select></div><div class="form-field form-full"><label>Descrição</label><textarea>Serviços de funilaria e pintura conforme diagnóstico: ${o.problem}</textarea></div></div><div class="modal-actions"><button class="btn" data-a="close">Cancelar</button><button class="btn primary" data-a="savebudget" data-id="${id}">Salvar orçamento</button></div>`,
  );
}
