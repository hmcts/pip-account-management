package uk.gov.hmcts.reform.pip.account.management.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class MediaCsv {

    @CsvBindByName(column = "email")
    private String email;

    @CsvBindByName(column = "firstName")
    private String firstName;

    @CsvBindByName(column = "surname")
    private String surname;
}
