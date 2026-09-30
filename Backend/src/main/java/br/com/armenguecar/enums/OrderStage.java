package br.com.armenguecar.enums;

public enum OrderStage {

    RECEPCAO_CHECKIN("Recepção / Check-in"),
    DIAGNOSTICO("Em Diagnóstico"),
    AGUARDANDO_APROVACAO("Aguardando Aprovação"),
    AGUARDANDO_INICIO("Aguardando Início"),
    EXECUCAO("Em Execução"),
    AGUARDANDO_PECAS("Aguardando Peças"),
    CONTROLE_QUALIDADE("Controle de Qualidade"),
    PRONTO_ENTREGA("Pronto para Entrega");

    private final String label;

    OrderStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
