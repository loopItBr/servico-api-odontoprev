import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaInativacaoRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class TestJsonStructure {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create test data matching your JSON structure
        EmpresaInativacaoRequest.DadosInativacaoEmpresa dados = EmpresaInativacaoRequest.DadosInativacaoEmpresa.builder()
                .codigoEmpresa("787392")
                .codigoMotivoFimEmpresa("1")
                .codigoMotivoInativacao("2")
                .dataFimContrato("2024-12-31")
                .build();
        
        EmpresaInativacaoRequest request = EmpresaInativacaoRequest.builder()
                .sistema("787392")
                .codigoUsuario("0")
                .listaDadosInativacaoEmpresa(Collections.singletonList(dados))
                .build();
        
        String json = mapper.writeValueAsString(request);
        System.out.println("Generated JSON:");
        System.out.println(json);
        
        // Expected JSON structure:
        System.out.println("\nExpected JSON structure:");
        System.out.println("{\n" +
                "\"sistema\": \"string\",\n" +
                "\"codigoUsuario\": \"string \",\n" +
                "\"listaDadosInativacaoEmpresa\": [\n" +
                "{\n" +
                "\"codigoEmpresa\": \"string \",\n" +
                "\"codigoMotivoFimEmpresa\": \"string \",\n" +
                "\"codigoMotivoInativacao\": \"string \",\n" +
                "\"dataFimContrato\": \"YYYY-MM-DD\"\n" +
                "}\n" +
                "]\n" +
                "}");
    }
}
