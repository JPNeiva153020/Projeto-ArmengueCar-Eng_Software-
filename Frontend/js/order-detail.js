// Detalhe da Ordem de Serviço: modal de detalhe, regras de próxima ação,
// evidências/fotos e transições de etapa (avançar/retroceder).
function openOrder(id) {
  currentOrder = db.orders.find((o) => o.id === id);
  let o = currentOrder;
  if (!o) return;
  let missing = o.parts.some((p) => {
    let s = db.stock.find((x) => x[0] === p[0]);
    return s && s[2] < p[1];
  });
  openModal(
    `${o.id} · ${o.plate}`,
    `<div class="detail-grid"><div><div class="card" style="box-shadow:none"><div class="card-head"><h2>${o.client}</h2>${stageBadge(o.stage)}</div><div class="card-body"><p><b>Veículo:</b> ${o.car}<br><b>Problema:</b> ${o.problem}<br><b>Orçamento:</b> ${money(o.value)}</p><h3>Andamento</h3><div class="progress"><i style="width:${o.progress}%"></i></div><p class="muted">${o.progress}% concluído · ${o.days} dia(s) no fluxo</p></div></div></div><div><div class="card" style="box-shadow:none"><div class="card-head"><h2>Controles</h2></div><div class="card-body"><div class="rule-box"><b>Próxima ação</b>${next(o, missing)}</div><div class="detail-actions">${view === "portal" && o.stage > 0 ? `<button class="btn" data-a="retreat" data-id="${o.id}">← Voltar etapa</button>` : ""}${o.stage === 7 ? `<button class="btn primary" data-a="deliver" data-id="${o.id}">Registrar entrega</button>` : `<button class="btn primary" data-a="advance" data-id="${o.id}">Avançar etapa →</button>`}<button class="btn" data-a="budget" data-id="${o.id}">Orçamento</button><button class="btn" data-a="quality" data-id="${o.id}">Checklist</button></div></div></div></div></div><div class="card" style="margin-top:14px;box-shadow:none"><div class="card-head"><h2>Evidências do reparo</h2><small>${o.photos || 0} REGISTROS</small></div><div class="card-body"><div class="evidence-grid">${evidenceTiles(o, 6)}<div class="evidence-photo evidence-add" data-a="addphoto" data-id="${o.id}">+ Anexar<br>foto</div></div></div></div><div class="card" style="margin-top:14px;box-shadow:none"><div class="card-head"><h2>Histórico</h2></div><div class="card-body timeline">${stages
      .slice(0, o.stage + 1)
      .map(
        (s, i) =>
          `<div class="timeline-item"><div><div class="timeline-dot"></div></div><div><strong>${s}</strong><p>${i === o.stage ? "Etapa atual" : "Concluída"} · ${o.id}</p></div></div>`,
      )
      .join(
        "",
      )}</div></div><div class="modal-actions"><button class="btn" data-a="close">Fechar</button></div>`,
  );
}

function next(o, m) {
  if (o.stage === 1)
    return o.value > 0
      ? "Enviar orçamento para aprovação."
      : "Bloqueado: informe valor do orçamento.";
  if (o.stage === 2) return o.approved ? "Aguardando início." : "Aguardando decisão do cliente.";
  if (o.stage === 3) return o.reserved ? "Iniciar execução." : "Reserve as peças necessárias.";
  if (o.stage === 4)
    return m ? "Falta de peça: mover para Aguardando Peças." : "Continuar execução.";
  if (o.stage === 5) return "Retomar quando as peças estiverem disponíveis.";
  if (o.stage === 6)
    return o.check.every(Boolean)
      ? "Checklist aprovado; liberar entrega."
      : "Concluir checklist obrigatório.";
  return o.stage === 7 ? "Registrar entrega e baixa definitiva." : "Concluir diagnóstico.";
}

function evidenceTiles(o, limit) {
  let real = photosStore[o.id] || [],
    total = Math.max(o.photos || 0, real.length),
    n = limit ? Math.min(total, limit) : total,
    tiles = "";
  for (let i = 0; i < n; i++) {
    tiles += real[i]
      ? `<div class="evidence-photo" style="background-image:url('${real[i]}');background-size:cover;background-position:center"></div>`
      : `<div class="evidence-photo">EVIDÊNCIA ${i + 1}<br>REPARO</div>`;
  }
  return tiles;
}

function triggerPhotoUpload(id) {
  let inp = document.createElement("input");
  inp.type = "file";
  inp.accept = "image/*";
  inp.multiple = true;
  inp.onchange = () => addPhotos(id, inp.files);
  inp.click();
}

function addPhotos(id, files) {
  let o = db.orders.find((x) => x.id === id);
  if (!o || !files || !files.length) return;
  let arr = photosStore[id] || (photosStore[id] = []);
  Promise.all(
    [...files].map(
      (f) =>
        new Promise((res) => {
          let r = new FileReader();
          r.onload = () => res(r.result);
          r.readAsDataURL(f);
        }),
    ),
  ).then((urls) => {
    arr.push(...urls);
    o.photos = (o.photos || 0) + urls.length;
    log("Evidências anexadas", id, `${urls.length} foto(s) adicionada(s).`);
    save();
    toastMsg("Foto(s) anexada(s) ao reparo.");
    if (currentOrder && currentOrder.id === id) openOrder(id);
    render();
  });
}

function retreat(id) {
  let o = db.orders.find((x) => x.id === id);
  if (!o) return;
  if (o.stage <= 0) return toastMsg("Já está na primeira etapa do fluxo.");
  o.stage--;
  o.progress = Math.min(100, Math.round((o.stage / 7) * 100));
  log("Retorno de etapa", id, `Movida para ${stages[o.stage]}.`);
  save();
  toastMsg(`${id} ← ${stages[o.stage]}`);
  closeModal();
  render();
}

function advance(id) {
  let o = db.orders.find((x) => x.id === id);
  if (o.stage === 1 && o.value <= 0)
    return toastMsg("Bloqueado: orçamento de R$ 0 não pode avançar.");
  if (o.stage === 2 && !o.approved) return toastMsg("Bloqueado: aguardando aprovação do cliente.");
  if (o.stage === 3 && !o.reserved)
    return toastMsg("Bloqueado: reserve as peças antes da execução.");
  if (
    o.stage === 4 &&
    o.parts.some((p) => {
      let s = db.stock.find((x) => x[0] === p[0]);
      return s && s[2] < p[1];
    })
  ) {
    o.stage = 5;
    log("Bloqueio por falta de peças", id, "OS movida para Aguardando Peças.");
    toastMsg("Falta de peça: OS movida para Aguardando Peças.");
    save();
    openOrder(id);
    return;
  }
  if (o.stage === 6 && !o.check.every(Boolean))
    return toastMsg("Bloqueado: checklist de qualidade pendente.");
  if (o.stage < 7) {
    o.stage++;
    o.progress = Math.min(100, Math.round((o.stage / 7) * 100));
    log("Avanço de etapa", id, `Movida para ${stages[o.stage]}.`);
    if (o.stage === 4)
      db.notifications.unshift([
        "Execução iniciada",
        `${id} entrou em execução.`,
        "agora",
        "blue",
        0,
        { view: "orders", orderId: id },
      ]);
    save();
    toastMsg(`${id} → ${stages[o.stage]}`);
    closeModal();
    render();
  }
}
