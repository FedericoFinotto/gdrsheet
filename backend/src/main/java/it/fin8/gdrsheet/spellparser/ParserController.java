package it.fin8.gdrsheet.spellparser;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.entity.Item;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/parser")
public class ParserController {

    private final IncantesimoParser parser;

    public ParserController(IncantesimoParser parser) {
        this.parser = parser;
    }

    @Operation(
            summary = "EEEE",
            description = "OOOO"
    )
    @GetMapping("/edit")
    public ResponseEntity<List<Item>> elaboraByNome(
    ) throws Exception {
        return ResponseEntity.ok(parser.edit());
    }


    @GetMapping("/file/all")
    @Transactional
    public ResponseEntity<List<Item>> elaboraTutti() throws Exception {
        File cartella = new File("C://opt/gdrsheet/spells/");

        if (!cartella.exists() || !cartella.isDirectory()) {
            return ResponseEntity.notFound().build();
        }

        File[] files = cartella.listFiles((dir, name) -> name.toLowerCase().endsWith(".html"));
        if (files == null || files.length == 0) {
            return ResponseEntity.noContent().build();
        }

        List<Item> tuttiGliItem = new ArrayList<>();

        for (File html : files) {
            System.out.println("Elaboro file: " + html.getName());

            List<Incantesimo> incantesimi = parser.parse(html);
            List<Item> processato = parser.postProcessing(
                    incantesimi,
                    html.getName().substring(0, html.getName().lastIndexOf("."))
            );

            tuttiGliItem.addAll(processato);
        }


        List<String> BANNED_LIST = new ArrayList<>(List.of("Dragon Magazine", "Book of Exalted Deeds"));


        List<Item> incantesimiDaLibriBannati = tuttiGliItem.stream()
                .filter(item -> item.getLabels().stream()
                        .anyMatch(label ->
                                "MANUALE_SP".equals(label.getLabel()) &&
                                        (BANNED_LIST.contains(label.getValore()))
                        )
                )
                .toList();

        tuttiGliItem.removeIf(item -> item.getLabels().stream()
                .anyMatch(label ->
                        "MANUALE_SP".equals(label.getLabel()) &&
                                (BANNED_LIST.contains(label.getValore()))
                )
        );


        List<Item> tuttiGliItemSenzaNome = tuttiGliItem.stream()
                .filter(x -> x.getNome() != null && x.getNome().isEmpty())
                .toList();


        // Filtra item con nome non nullo e non vuoto
        tuttiGliItem = tuttiGliItem.stream()
                .filter(x -> x.getNome() != null && !x.getNome().isEmpty())
                .toList();

        // Raggruppa per nome e stampa duplicati
        Map<String, List<Item>> raggruppatiPerNome = tuttiGliItem.stream()
                .collect(Collectors.groupingBy(Item::getNome));

        raggruppatiPerNome.forEach((nome, lista) -> {
            if (lista.size() > 1) {
                System.out.println("Duplicati trovati per nome: " + nome + " (" + lista.size() + " occorrenze)");
                for (Item itm : lista) {
                    itm.getLabels().stream()
                            .filter(label -> label.getLabel().equals("MANUALE_SP"))
                            .findFirst()
                            .ifPresent(label -> System.out.println(label.getValore()));
                }
            }
        });

        // Prendi solo il primo per ogni nome
        List<Item> senzaDuplicati = raggruppatiPerNome.values().stream()
                .map(lista -> lista.get(0))
                .toList();

        IntStream.range(0, senzaDuplicati.size()).forEach(i -> {
            Item item = senzaDuplicati.get(i);
            parser.persist(item);
            System.out.println("Persistito " + (i + 1) + "/" + senzaDuplicati.size() + ": " + item.getNome());
        });

        return ResponseEntity.ok(senzaDuplicati);
    }


}
