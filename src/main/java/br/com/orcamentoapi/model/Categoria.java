package br.com.orcamentoapi.model;

public enum Categoria {

    MERCADO("Supermercado, padaria, feira"),
    FARMACIA("Remédios e drogaria"),
    TRANSPORTE("Combustível, aplicativos de transporte, estacionamento"),
    RESTAURANTE("Restaurantes, lanchonetes e delivery"),
    LAZER("Cinema, shows, viagens"),
    OUTROS("Gastos que não se encaixam nas outras categorias");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
