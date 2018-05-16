package br.com.luceneTika;

import java.io.*;
import java.text.*;

import org.apache.log4j.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;
import org.apache.tika.*;

public class Indexador {
  private static Logger logger = Logger.getLogger(Indexador.class);
  //{1}
  private String diretorioDosIndices = System.getProperty("user.home")
      + "/lucene/index";
  //{2}
  private String diretorioParaIndexar = System.getProperty("user.home")
      + "/lucene/data";
  //{3}
  private IndexWriter writer;
  //{4}
  private Tika tika;

  public static void main(String[] args) {
    Indexador indexador = new Indexador();
    indexador.indexaArquivosDoDiretorio();
	//System.out.println(indexador.diretorioDosIndices);
  }

  public void indexaArquivosDoDiretorio() {
    try {
      File diretorio = new File(diretorioDosIndices); //caminho da pasta que armazenará os indices
      apagaIndices(diretorio); //apaga os arquivos antigos para uma nova indexação
      //{5}
      Directory d = new SimpleFSDirectory(diretorio); //diretorio que armazena os indices
      logger.info("Diretório do índice: " + diretorioDosIndices);
      //{6}
      Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36); //Analyzer faz o pré-processamento de texto
      //{7}
      IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer); // configurações para criação do índice. No projeto são utilizados os valores padrão
      //{8}
      writer = new IndexWriter(d, config); //Inicializa o IndexWriter para gravação
      long inicio = System.currentTimeMillis();
      indexaArquivosDoDiretorio(new File(diretorioParaIndexar));
      //{12}
      writer.commit();
      writer.close();
      long fim = System.currentTimeMillis();
      logger.info("Tempo para indexar: " + ((fim - inicio) / 1000) + "s");
    } catch (IOException e) {
      logger.error(e);
    }
  }

  private void apagaIndices(File diretorio) {
    if (diretorio.exists()) {
      File arquivos[] = diretorio.listFiles();
      for (File arquivo : arquivos) {
        arquivo.delete();
      }
    }
  }

  public void indexaArquivosDoDiretorio(File raiz) {
    FilenameFilter filtro = new FilenameFilter() {
      public boolean accept(File arquivo, String nome) {
        if (nome.toLowerCase().endsWith(".pdf")
            || nome.toLowerCase().endsWith(".odt")
            || nome.toLowerCase().endsWith(".doc")
            || nome.toLowerCase().endsWith(".docx")
            || nome.toLowerCase().endsWith(".ppt")
            || nome.toLowerCase().endsWith(".pptx")
            || nome.toLowerCase().endsWith(".xls")
            || nome.toLowerCase().endsWith(".txt")
            || nome.toLowerCase().endsWith(".rtf")) {
          return true;
        }
        return false;
      }
    };
    for (File arquivo : raiz.listFiles(filtro)) {
      if (arquivo.isFile()) {
        StringBuffer msg = new StringBuffer();
        msg.append("Indexando o arquivo ");
        msg.append(arquivo.getAbsoluteFile());
        msg.append(", ");
        msg.append(arquivo.length() / 1000);
        msg.append("kb");
        logger.info(msg);
        try {
          //{9}
          String textoExtraido = getTika().parseToString(arquivo); //Extrai o conteúdo do arquivo com o Tika
          indexaArquivo(arquivo, textoExtraido);
        } catch (Exception e) {
          logger.error(e);
        }
      } else {
    	  System.out.println(arquivo.getAbsolutePath() + " " + arquivo.getName() + " " + arquivo.isDirectory());
        indexaArquivosDoDiretorio(arquivo);
      }
    }
  }

  private void indexaArquivo(File arquivo, String textoExtraido) {
    SimpleDateFormat formatador = new SimpleDateFormat("yyyyMMdd");
    String ultimaModificacao = formatador.format(arquivo.lastModified());
    
    //{10}
    //Monta um Document para indexação
    //Field.Store.YES: armazena uma cópia do texto no índice, aumentando muito o seu tamanho;
    //Field.Index.ANALYZED: utilizado quando o campo é de texto livre;
    //Field.Index.NOT_ANALYZED: utilizado quando o campo é um ID, data ou númerico.
    Document documento = new Document();
    documento.add(new Field("UltimaModificacao", ultimaModificacao,
        Field.Store.YES, Field.Index.NOT_ANALYZED));
    documento.add(new Field("Caminho", arquivo.getAbsolutePath(),
        Field.Store.YES, Field.Index.NOT_ANALYZED));
    documento.add(new Field("Texto", textoExtraido, Field.Store.YES,
        Field.Index.ANALYZED));
    try {
      //{11}
      //Adiciona o Document no índice, mas este só estará disponível para consulta após o commit
      getWriter().addDocument(documento);
    } catch (IOException e) {
      logger.error(e);
    }
  }

  public Tika getTika() {
    if (tika == null) {
      tika = new Tika();
    }
    return tika;
  }

  public IndexWriter getWriter() {
    return writer;
  }
}