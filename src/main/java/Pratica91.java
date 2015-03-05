
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
        System.out.println("Memória total (MB): " + rt.totalMemory() / 1024 / 1024);
        System.out.println("Memória livre (MB): " + rt.freeMemory() / 1024 / 1024);
        System.out.println("Memória máxima VM (MB): " + rt.maxMemory() / 1024 / 1024);
    }
    
}
