package br.com.orcamentoapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.orcamentoapi.model.Categoria;
import br.com.orcamentoapi.model.Transacao;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findAllByOrderByCriadaEmDesc();

    List<Transacao> findAllByCategoriaOrderByCriadaEmDesc(Categoria categoria);
}
