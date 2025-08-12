package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UtilService {

    public String getItemLabel(Item item, String label) {
        for (ItemLabel itemLabel : item.getLabels()) {
            if (itemLabel.getLabel().equals(label)) {
                return itemLabel.getValore();
            }
        }
        return null;
    }

    public List<String> getItemLabels(Item item, String label) {
        List<String> result = new ArrayList<>();
        for (ItemLabel itemLabel : item.getLabels()) {
            if (itemLabel.getLabel().equals(label)) {
                result.add(itemLabel.getValore());
            }
        }
        return result;
    }

    public String getCollegamentoLabel(Collegamento coll, String label) {
        for (CollegamentoLabel collLabel : coll.getLabels()) {
            if (collLabel.getLabel().equals(label)) {
                return collLabel.getValore();
            }
        }
        return null;
    }

    public String getPersonaggioLabel(Personaggio p, String label) {
        for (PersonaggioLabel pLabel : p.getLabels()) {
            if (pLabel.getLabel().equals(label)) {
                return pLabel.getValore();
            }
        }
        return null;
    }
}

