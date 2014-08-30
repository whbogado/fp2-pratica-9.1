
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * IF62C Fundamentos de Programação 2
 * Exercício de programação em Java.
 * @author Wilson Horstmeyer Bogado <wilson@utfpr.edu.br>
 */
public class Pratica91 {

    public static void main(String[] args) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        String os = System.getProperty("os.name");
        System.out.println("Executando em " + os);
        System.out.println("Número de processadores: " + rt.availableProcessors());
        System.out.println("Memória total (MB): " + rt.totalMemory() / 1024);
        System.out.println("Memória livre (MB): " + rt.freeMemory() / 1024);
        System.out.println("Memória máxima VM (MB): " + rt.maxMemory() / 1024);
        
        Process editor;
        String cmd;
        if (os.toLowerCase().contains("windows"))
            cmd = "notepad teste.txt";
        else
            cmd = "gedit teste.txt";
        System.out.println("Executando " + cmd);
        editor = rt.exec(cmd);
        editor.waitFor();
        File file = new File("teste.txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        if (file.exists()) {
            System.out.println("Arquivo " + file.getAbsolutePath() + " existe.");
            String linha;
            while ((linha = reader.readLine()) != null) {
                System.out.println(linha);
            }
            reader.close();
        } else {
            System.out.println("Arquivo não foi criado.");
        }
    }
    
}
