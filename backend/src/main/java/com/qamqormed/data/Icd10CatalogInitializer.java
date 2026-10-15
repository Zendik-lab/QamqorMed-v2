package com.qamqormed.data;

import com.qamqormed.entity.ICD10Code;
import com.qamqormed.repository.ICD10CodeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("dev")
public class Icd10CatalogInitializer implements CommandLineRunner {
    private final ICD10CodeRepository codes;
    public Icd10CatalogInitializer(ICD10CodeRepository codes) { this.codes = codes; }

    @Override
    @Transactional
    public void run(String... args) {
        List<String[]> seed = List.of(
                new String[]{"J06.9", "Острая инфекция верхних дыхательных путей неуточнённая"},
                new String[]{"I10", "Эссенциальная (первичная) гипертензия"},
                new String[]{"E11.9", "Сахарный диабет 2 типа без осложнений"},
                new String[]{"K21.9", "Гастроэзофагеальная рефлюксная болезнь без эзофагита"},
                new String[]{"Z00.0", "Общий медицинский осмотр"}
        );
        for (String[] item : seed) {
            ICD10Code code = codes.findByCodeIgnoreCase(item[0]).orElseGet(ICD10Code::new);
            code.setCode(item[0]); code.setTitleRu(item[1]); code.setActive(true); code.setSelectable(true);
            code.setSource("DEV_EXAMPLES"); code.setSourceVersion("NOT_FULL_CATALOG"); codes.save(code);
        }
    }
}
