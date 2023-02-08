package uk.gov.hmcts.reform.pip.account.management.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
public class CustomPageImpl<T> extends PageImpl<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -8188257920346699960L;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CustomPageImpl(@JsonProperty("content") List<T> content,
                          @JsonProperty("number") int page,
                          @JsonProperty("size") int size,
                          @JsonProperty("totalElements") long total) {

        super(content, PageRequest.of(page, size), total);
    }
}
